/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.call.MXCallsManager;
import org.matrix.androidsdk.core.Log;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import im.vector.activity.CommonActivityUtils;
import im.vector.activity.JitsiCallActivity;
import im.vector.activity.VectorCallViewActivity;
import im.vector.activity.VectorMediaPickerActivity;
import im.vector.activity.WidgetActivity;
import im.vector.analytics.Analytics;
import im.vector.analytics.AppAnalytics;
import im.vector.analytics.MatomoAnalytics;
import im.vector.analytics.e2e.DecryptionFailureTracker;
import im.vector.contacts.ContactsManager;
import im.vector.contacts.PIDsRetriever;
import im.vector.notifications.NotificationDrawerManager;
import im.vector.notifications.NotificationUtils;
import im.vector.push.PushManager;
import im.vector.settings.FontScale;
import im.vector.settings.VectorLocale;
import im.vector.tools.VectorUncaughtExceptionHandler;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.CallsManager;
import im.vector.util.PermissionsToolsKt;
import im.vector.util.PhoneNumberUtils;
import im.vector.util.PreferencesManager;
import im.vector.util.RageShake;
import im.vector.util.VectorMarkdownParser;
import im.vector.util.VectorUtils;

/**
 * The main application injection point
 */
public class VectorApp extends MultiDexApplication {
    private static final String LOG_TAG = VectorApp.class.getSimpleName();

    /**
     * The current instance.
     */
    private static VectorApp instance = null;

    /**
     * Rage shake detection to send a bug report.
     */
    private RageShake mRageShake;

    /**
     * Delay to detect if the application is in background.
     * If there is no active activity during the elapsed time, it means that the application is in background.
     */
    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 4000;

    /**
     * The current active activity
     */
    private static Activity mCurrentActivity = null;

    /**
     * Background application detection
     */
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    private boolean mIsInBackground = true;

    /**
     * Google analytics information.
     */
    private static String VECTOR_VERSION_STRING = "";
    private static String SDK_VERSION_STRING = "";

    /**
     * Tells if there a pending call whereas the application is backgrounded.
     */
    private boolean mIsCallingInBackground = false;

    /**
     * Monitor the created activities to detect memory leaks.
     */
    private final List<String> mCreatedActivities = new ArrayList<>();

    /**
     * Markdown parser
     */
    private VectorMarkdownParser mMarkdownParser;

    private NotificationDrawerManager mNotificationDrawerManager;

    public NotificationDrawerManager getNotificationDrawerManager() {
        return mNotificationDrawerManager;
    }

    /**
     * Lifecycle observer to start/stop eventstream service
     */
    private VectorLifeCycleObserver mLifeCycleListener;

    /**
     * Calls manager
     */
    private CallsManager mCallsManager;

    private Analytics mAppAnalytics;
    private DecryptionFailureTracker mDecryptionFailureTracker;

    /**
     * @return the current instance
     */
    public static VectorApp getInstance() {
        return instance;
    }

    /**
     * The directory in which the logs are stored
     */
    public static File mLogsDirectoryFile = null;

    /**
     * The last time that removeMediasBefore has been called.
     */
    private long mLastMediasCheck = 0;

    private final BroadcastReceiver mLanguageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(Locale.getDefault().toString(), VectorLocale.INSTANCE.getApplicationLocale().toString())) {
                Log.d(LOG_TAG, "## onReceive() : the locale has been updated to " + Locale.getDefault().toString()
                        + ", restore the expected value " + VectorLocale.INSTANCE.getApplicationLocale().toString());
                updateApplicationSettings(VectorLocale.INSTANCE.getApplicationLocale(),
                        FontScale.INSTANCE.getFontScalePrefValue(),
                        ThemeUtils.INSTANCE.getApplicationTheme(context));

                if (null != getCurrentActivity()) {
                    restartActivity(getCurrentActivity());
                }
            }

        }
    };

    private MpditManager mMpdit = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate();

        mLifeCycleListener = new VectorLifeCycleObserver();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(mLifeCycleListener);

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }

        VectorUtils.initAvatarColors(this);
        mNotificationDrawerManager = new NotificationDrawerManager(this);
        NotificationUtils.INSTANCE.createNotificationChannels(this);

        // init the REST client
        MXSession.initUserAgent(this, BuildConfig.FLAVOR_DESCRIPTION);

        VectorUncaughtExceptionHandler.INSTANCE.activate();

        instance = this;
        mCallsManager = new CallsManager(this);
        mAppAnalytics = new AppAnalytics(this, new MatomoAnalytics(this));
        mDecryptionFailureTracker = new DecryptionFailureTracker(mAppAnalytics);

        mActivityTransitionTimer = null;
        mActivityTransitionTimerTask = null;

        if (PreferencesManager.useDefaultTurnServer(this)) {
            MXCallsManager.defaultStunServerUri = getString(R.string.default_stun_server);
        } else {
            MXCallsManager.defaultStunServerUri = null;
        }

        VECTOR_VERSION_STRING = Matrix.getInstance(this).getVersion(true, true);
        // not the first launch
        if (null != Matrix.getInstance(this).getDefaultSession()) {
            SDK_VERSION_STRING = Matrix.getInstance(this).getDefaultSession().getVersion(true);
        } else {
            SDK_VERSION_STRING = "";
        }

        VectorUncaughtExceptionHandler.INSTANCE.setVersions(VECTOR_VERSION_STRING, SDK_VERSION_STRING);

        mLogsDirectoryFile = new File(getCacheDir().getAbsolutePath() + "/logs");

        org.matrix.androidsdk.core.Log.setLogDirectory(mLogsDirectoryFile);
        org.matrix.androidsdk.core.Log.init("RiotLog");

        // log the application version to trace update
        // useful to track backward compatibility issues

        Log.d(LOG_TAG, "----------------------------------------------------------------");
        Log.d(LOG_TAG, "----------------------------------------------------------------");
        Log.d(LOG_TAG, " Application version: " + VECTOR_VERSION_STRING);
        Log.d(LOG_TAG, " SDK version: " + SDK_VERSION_STRING);
        Log.d(LOG_TAG, " Local time: " + (new SimpleDateFormat("MM-dd HH:mm:ss.SSSZ", Locale.US)).format(new Date()));
        Log.d(LOG_TAG, "----------------------------------------------------------------");
        Log.d(LOG_TAG, "----------------------------------------------------------------\n\n\n\n");

        mRageShake = new RageShake(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            final Map<String, String> mLocalesByActivity = new HashMap<>();

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.d(LOG_TAG, "onActivityCreated " + activity);
                mCreatedActivities.add(activity.toString());
                // matomo
                onNewScreen(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.d(LOG_TAG, "onActivityStarted " + activity);
            }

            /**
             * Compute the locale status value
             *
             * @param activity the activity
             * @return the local status value
             */
            private String getActivityLocaleStatus(Activity activity) {
                return VectorLocale.INSTANCE.getApplicationLocale().toString()
                        + "_" + FontScale.INSTANCE.getFontScalePrefValue()
                        + "_" + ThemeUtils.INSTANCE.getApplicationTheme(activity);
            }

            @Override
            public void onActivityResumed(final Activity activity) {
                Log.d(LOG_TAG, "onActivityResumed " + activity);
                setCurrentActivity(activity);

                String activityKey = activity.toString();

                if (mLocalesByActivity.containsKey(activityKey)) {
                    String prevActivityLocale = mLocalesByActivity.get(activityKey);

                    if (!TextUtils.equals(prevActivityLocale, getActivityLocaleStatus(activity))) {
                        Log.d(LOG_TAG, "## onActivityResumed() : restart the activity " + activity
                                + " because of the locale update from " + prevActivityLocale + " to " + getActivityLocaleStatus(activity));
                        restartActivity(activity);
                        return;
                    }
                }

                // it should never happen as there is a broadcast receiver (mLanguageReceiver)
                if (!TextUtils.equals(Locale.getDefault().toString(), VectorLocale.INSTANCE.getApplicationLocale().toString())) {
                    Log.d(LOG_TAG, "## onActivityResumed() : the locale has been updated to " + Locale.getDefault().toString()
                            + ", restore the expected value " + VectorLocale.INSTANCE.getApplicationLocale().toString());
                    updateApplicationSettings(VectorLocale.INSTANCE.getApplicationLocale(),
                            FontScale.INSTANCE.getFontScalePrefValue(),
                            ThemeUtils.INSTANCE.getApplicationTheme(activity));
                    restartActivity(activity);
                }

                PermissionsToolsKt.logPermissionStatuses(VectorApp.this);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d(LOG_TAG, "onActivityPaused " + activity);
                mLocalesByActivity.put(activity.toString(), getActivityLocaleStatus(activity));
                setCurrentActivity(null);
                onAppPause();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d(LOG_TAG, "onActivityStopped " + activity);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log.d(LOG_TAG, "onActivitySaveInstanceState " + activity);
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(LOG_TAG, "onActivityDestroyed " + activity);
                mCreatedActivities.remove(activity.toString());
                mLocalesByActivity.remove(activity.toString());

                if (mCreatedActivities.size() > 1) {
                    Log.d(LOG_TAG, "onActivityDestroyed : \n" + mCreatedActivities);
                }
            }
        });

        // create the markdown parser
        try {
            mMarkdownParser = new VectorMarkdownParser(this);
        } catch (Exception e) {
            // reported by GA
            Log.e(LOG_TAG, "cannot create the mMarkdownParser " + e.getMessage(), e);
        }

        // track external language updates
        // local update from the settings
        // or screen rotation !
        registerReceiver(mLanguageReceiver, new IntentFilter(Intent.ACTION_LOCALE_CHANGED));
        registerReceiver(mLanguageReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));

        PreferencesManager.fixMigrationIssues(this);
        initApplicationLocale();
        visitSessionVariables();

        // MPDIT
        CreateMpdit();


    }


    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }


    private void CreateMpdit()
    {
        if(mMpdit != null)
            return;

        mMpdit = new MpditManager();





        //WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        //String ip = wm.getConnectionInfo().getIpAddress();//Formatter.formatIpAddress();
        WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));

        mMpdit.mDeviceIP = ip;
        mMpdit.create();

        // zbieranie danych GPS
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;
        }
        else {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, mMpdit);
        }

        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, mMpdit);

        String MpditGID = PreferencesManager.mpditGID(getApplicationContext());
        mMpdit.AddFirstMpditNode(MpditGID);


        Set<String> setNames = PreferencesManager.gotennaNames(getApplicationContext());
        Set<String> setGids = PreferencesManager.gotennaGids(getApplicationContext());

        if(setNames != null && setGids != null) {
            Vector<String> n = new Vector<String>();
            Vector<String> g = new Vector<String>();

            Iterator<String> ni = setNames.iterator();
            while(ni.hasNext()) n.add(ni.next());

            Iterator<String> gi = setGids.iterator();
            while(gi.hasNext()) g.add(gi.next());

            for(int i =0; i<n.size() && i<g.size() && i<100; i++) {
                mMpdit.goTennaAddUpdateNode(g.get(i),n.get(i));
            }

            /*String[] n = setNames.toArray(new String[100]);
            String[] g = setGids.toArray(new String[100]);
            if(g != null && n != null) {
                for(int i =0; i<n.length && i<g.length && i<100; i++) {
                    mMpdit.AddUpdateGotennaNode(g[i],n[i]);
                }
                // setGids.iterator();
            }*/
        }






        Thread mThreadSocket = new Thread(mMpdit);
        mThreadSocket.start();

        /*
            mSession = Matrix.getInstance(this).getDefaultSession();
            //mSession.getMyUserId();
            //mSession.getMyUser().displayname;
            mMpdit.mDisplayedName = mSession.getMyUser().displayname;
            mMpdit.mID = mSession.getMyUserId();
         */

    }

    public MpditManager getMpditManger()
    {
        if(mMpdit == null)  CreateMpdit();
        return mMpdit;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!TextUtils.equals(Locale.getDefault().toString(), VectorLocale.INSTANCE.getApplicationLocale().toString())) {
            Log.d(LOG_TAG, "## onConfigurationChanged() : the locale has been updated to " + Locale.getDefault().toString()
                    + ", restore the expected value " + VectorLocale.INSTANCE.getApplicationLocale().toString());
            updateApplicationSettings(VectorLocale.INSTANCE.getApplicationLocale(),
                    FontScale.INSTANCE.getFontScalePrefValue(),
                    ThemeUtils.INSTANCE.getApplicationTheme(this));
        }
    }

    /**
     * Parse a markdown text
     *
     * @param text     the text to parse
     * @param listener the result listener
     */
    public static void markdownToHtml(final String text, final VectorMarkdownParser.IVectorMarkdownParserListener listener) {
        if (null != getInstance().mMarkdownParser) {
            getInstance().mMarkdownParser.markdownToHtml(text, listener);
        } else {
            (new Handler(Looper.getMainLooper())).post(new Runnable() {
                @Override
                public void run() {
                    // GA issue
                    listener.onMarkdownParsed(text, null);
                }
            });
        }
    }

    /**
     * Suspend background threads.
     */
    private void suspendApp() {
        PushManager pushManager = Matrix.getInstance(this).getPushManager();

        // suspend the events thread if the client uses FCM
        if (!pushManager.isBackgroundSyncAllowed() || (pushManager.useFcm() && pushManager.hasRegistrationToken())) {
            Log.d(LOG_TAG, "suspendApp ; pause the event stream");
            //CommonActivityUtils.pauseEventStream(this);
        } else {
            Log.d(LOG_TAG, "suspendApp ; the event stream is not paused because FCM is disabled.");
        }

        // the sessions are not anymore seen as "online"
        List<MXSession> sessions = Matrix.getInstance(this).getSessions();

        for (MXSession session : sessions) {
            if (session.isAlive()) {
                session.setIsOnline(false);

                // remove older medias
                if ((System.currentTimeMillis() - mLastMediasCheck) < (24 * 60 * 60 * 1000)) {
                    mLastMediasCheck = System.currentTimeMillis();
                    session.removeMediaBefore(this, PreferencesManager.getMinMediasLastAccessTime(getApplicationContext()));
                }

                if (session.getDataHandler().areLeftRoomsSynced()) {
                    session.getDataHandler().releaseLeftRooms();
                }
            }
        }

        clearSyncingSessions();

        PIDsRetriever.getInstance().onAppBackgrounded();

        MyPresenceManager.advertiseAllUnavailable();

        mRageShake.stop();

        onAppPause();
    }

    /**
     * Test if application is put in background.
     * i.e wait 2s before assuming that the application is put in background.
     */
    private void startActivityTransitionTimer() {
        Log.d(LOG_TAG, "## startActivityTransitionTimer()");

        try {
            mActivityTransitionTimer = new Timer();
            mActivityTransitionTimerTask = new TimerTask() {
                @Override
                public void run() {
                    // reported by GA
                    try {
                        if (mActivityTransitionTimerTask != null) {
                            mActivityTransitionTimerTask.cancel();
                            mActivityTransitionTimerTask = null;
                        }

                        if (mActivityTransitionTimer != null) {
                            mActivityTransitionTimer.cancel();
                            mActivityTransitionTimer = null;
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## startActivityTransitionTimer() failed " + e.getMessage(), e);
                    }

                    if (null != mCurrentActivity) {
                        Log.e(LOG_TAG, "## startActivityTransitionTimer() : the timer expires but there is an active activity.");
                    } else {
                        mIsInBackground = true;
                        mIsCallingInBackground = (null != mCallsManager.getActiveCall());

                        // if there is a pending call
                        // the application is not suspended
                        if (!mIsCallingInBackground) {
                            Log.d(LOG_TAG, "Suspend the application because there was no resumed activity within "
                                    + (MAX_ACTIVITY_TRANSITION_TIME_MS / 1000) + " seconds");
                            CommonActivityUtils.displayMemoryInformation(null, " app suspended");
                            suspendApp();
                        } else {
                            Log.d(LOG_TAG, "App not suspended due to call in progress");
                        }
                    }
                }
            };

            mActivityTransitionTimer.schedule(mActivityTransitionTimerTask, MAX_ACTIVITY_TRANSITION_TIME_MS);
        } catch (Throwable throwable) {
            Log.e(LOG_TAG, "## startActivityTransitionTimer() : failed to start the timer " + throwable.getMessage());

            if (null != mActivityTransitionTimer) {
                mActivityTransitionTimer.cancel();
                mActivityTransitionTimer = null;
            }
        }
    }

    /**
     * Stop the background detection.
     */
    private void stopActivityTransitionTimer() {
        Log.d(LOG_TAG, "## stopActivityTransitionTimer()");

        if (mActivityTransitionTimerTask != null) {
            mActivityTransitionTimerTask.cancel();
            mActivityTransitionTimerTask = null;
        }

        if (mActivityTransitionTimer != null) {
            mActivityTransitionTimer.cancel();
            mActivityTransitionTimer = null;
        }

        if (isAppInBackground() && !mIsCallingInBackground) {

            // try to perform a FCM registration if it failed
            // or if the FCM server generated a new push key
            PushManager pushManager = Matrix.getInstance(this).getPushManager();

            if (null != pushManager) {
                pushManager.checkRegistrations();
            }

            // get the contact update at application launch
            ContactsManager.getInstance().clearSnapshot();
            ContactsManager.getInstance().refreshLocalContactsSnapshot();

            List<MXSession> sessions = Matrix.getInstance(this).getSessions();
            for (MXSession session : sessions) {
                session.getMyUser().refreshUserInfos(null);
                session.setIsOnline(true);
                addSyncingSession(session);
            }

            mCallsManager.checkDeadCalls();
            Matrix.getInstance(this).getPushManager().onAppResume();
        }

        MyPresenceManager.advertiseAllOnline();
        mRageShake.start();

        mIsCallingInBackground = false;
        mIsInBackground = false;
    }

    /**
     * Update the current active activity.
     * It manages the application background / foreground when it is required.
     *
     * @param activity the current activity, null if there is no more one.
     */
    private void setCurrentActivity(@Nullable Activity activity) {
        Log.d(LOG_TAG, "## setCurrentActivity() : from " + mCurrentActivity + " to " + activity);

        if (VectorApp.isAppInBackground() && (null != activity)) {
            Matrix matrixInstance = Matrix.getInstance(activity.getApplicationContext());

            // sanity check
            if (null != matrixInstance) {
                matrixInstance.refreshPushRules();
            }

            Log.d(LOG_TAG, "The application is resumed");
            // display the memory usage when the application is put iun foreground..
            CommonActivityUtils.displayMemoryInformation(activity, " app resumed with " + activity);
        }

        // wait 2s to check that the application is put in background
        if (null != getInstance()) {
            if (null == activity) {
                getInstance().startActivityTransitionTimer();
            } else {
                getInstance().stopActivityTransitionTimer();
            }
        } else {
            Log.e(LOG_TAG, "The application is resumed but there is no active instance");
        }

        mCurrentActivity = activity;

        if (null != mCurrentActivity) {
            PopupAlertManager.INSTANCE.onNewActivityDisplayed(mCurrentActivity);
        }
    }

    /**
     * @return the analytics app instance
     */
    public Analytics getAnalytics() {
        return mAppAnalytics;
    }

    /**
     * @return the DecryptionFailureTracker instance
     */
    public DecryptionFailureTracker getDecryptionFailureTracker() {
        return mDecryptionFailureTracker;
    }

    /**
     * @return the current active activity
     */
    public static Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    /**
     * Return true if the application is in background.
     */
    public static boolean isAppInBackground() {
        return (null == mCurrentActivity) && (null != getInstance()) && getInstance().mIsInBackground;
    }

    /**
     * Restart an activity to manage language update
     *
     * @param activity the activity to restart
     */
    private void restartActivity(Activity activity) {
        // avoid restarting activities when it is not required
        // some of them has no text
        if (!(activity instanceof VectorMediaPickerActivity)
                && !(activity instanceof VectorCallViewActivity)
                && !(activity instanceof JitsiCallActivity)
                && !(activity instanceof WidgetActivity)) {
            activity.startActivity(activity.getIntent());
            activity.finish();
        }
    }

    //==============================================================================================================
    // cert management : store the active activities.
    //==============================================================================================================

    private final EventEmitter<Activity> mOnActivityDestroyedListener = new EventEmitter<>();

    /**
     * @return the EventEmitter list.
     */
    public EventEmitter<Activity> getOnActivityDestroyedListener() {
        return mOnActivityDestroyedListener;
    }

    //==============================================================================================================
    // Media pickers : image backup
    //==============================================================================================================

    private static Bitmap mSavedPickerImagePreview = null;

    /**
     * The image taken from the medias picker is stored in a static variable because
     * saving it would take too much time.
     *
     * @return the saved image from medias picker
     */
    public static Bitmap getSavedPickerImagePreview() {
        return mSavedPickerImagePreview;
    }

    /**
     * Save the image taken in the medias picker
     *
     * @param aSavedCameraImagePreview the bitmap.
     */
    public static void setSavedCameraImagePreview(Bitmap aSavedCameraImagePreview) {
        if (aSavedCameraImagePreview != mSavedPickerImagePreview) {
            // force to release memory
            // reported by GA
            // it seems that the medias picker might be refreshed
            // while leaving the activity
            // recycle the bitmap trigger a rendering issue
            // Canvas: trying to use a recycled bitmap...

            /*if (null != mSavedPickerImagePreview) {
                mSavedPickerImagePreview.recycle();
                mSavedPickerImagePreview = null;
                System.gc();
            }*/


            mSavedPickerImagePreview = aSavedCameraImagePreview;
        }
    }

    //==============================================================================================================
    // Syncing mxSessions
    //==============================================================================================================

    /**
     * syncing sessions
     */
    private static final Set<MXSession> mSyncingSessions = new HashSet<>();

    /**
     * Add a session in the syncing sessions list
     *
     * @param session the session
     */
    public static void addSyncingSession(MXSession session) {
        synchronized (mSyncingSessions) {
            mSyncingSessions.add(session);
        }
    }

    /**
     * Remove a session in the syncing sessions list
     *
     * @param session the session
     */
    public static void removeSyncingSession(MXSession session) {
        if (null != session) {
            synchronized (mSyncingSessions) {
                mSyncingSessions.remove(session);
            }
        }
    }

    /**
     * Clear syncing sessions list
     */
    public static void clearSyncingSessions() {
        synchronized (mSyncingSessions) {
            mSyncingSessions.clear();
        }
    }

    /**
     * Tell if a session is syncing
     *
     * @param session the session
     * @return true if the session is syncing
     */
    public static boolean isSessionSyncing(MXSession session) {
        boolean isSyncing = false;

        if (null != session) {
            synchronized (mSyncingSessions) {
                isSyncing = mSyncingSessions.contains(session);
            }
        }

        return isSyncing;
    }

    //==============================================================================================================
    // Locale management
    //==============================================================================================================

    /**
     * Init the application locale from the saved one
     */
    private void initApplicationLocale() {
        VectorLocale.INSTANCE.init(this);

        Locale locale = VectorLocale.INSTANCE.getApplicationLocale();
        float fontScale = FontScale.INSTANCE.getFontScale();
        String theme = ThemeUtils.INSTANCE.getApplicationTheme(this);

        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.locale = locale;
        config.fontScale = fontScale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // init the theme
        ThemeUtils.INSTANCE.setApplicationTheme(this, theme);
    }

    /**
     * Update the application locale
     *
     * @param locale
     */
    public static void updateApplicationLocale(Locale locale) {
        updateApplicationSettings(locale, FontScale.INSTANCE.getFontScalePrefValue(), ThemeUtils.INSTANCE.getApplicationTheme(VectorApp.getInstance()));
    }

    /**
     * Update the application theme
     *
     * @param theme the new theme
     */
    public static void updateApplicationTheme(String theme) {
        ThemeUtils.INSTANCE.setApplicationTheme(VectorApp.getInstance(), theme);
        updateApplicationSettings(VectorLocale.INSTANCE.getApplicationLocale(),
                FontScale.INSTANCE.getFontScalePrefValue(),
                ThemeUtils.INSTANCE.getApplicationTheme(VectorApp.getInstance()));
    }

    /**
     * Update the application locale.
     *
     * @param locale the locale
     * @param theme  the new theme
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private static void updateApplicationSettings(Locale locale, String textSize, String theme) {
        Context context = VectorApp.getInstance();

        VectorLocale.INSTANCE.saveApplicationLocale(context, locale);
        FontScale.INSTANCE.saveFontScale(textSize);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.locale = locale;
        config.fontScale = FontScale.INSTANCE.getFontScale();
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

        ThemeUtils.INSTANCE.setApplicationTheme(context, theme);
        PhoneNumberUtils.onLocaleUpdate();
    }

    /**
     * Compute a localised context
     *
     * @param context the context
     * @return the localised context
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static Context getLocalisedContext(Context context) {
        try {
            Resources resources = context.getResources();
            Locale locale = VectorLocale.INSTANCE.getApplicationLocale();
            Configuration configuration = resources.getConfiguration();
            configuration.fontScale = FontScale.INSTANCE.getFontScale();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocale(locale);
                configuration.setLayoutDirection(locale);
                return context.createConfigurationContext(configuration);
            } else {
                configuration.locale = locale;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    configuration.setLayoutDirection(locale);
                }
                resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                return context;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## getLocalisedContext() failed : " + e.getMessage(), e);
        }

        return context;
    }

    //==============================================================================================================
    // Analytics management
    //==============================================================================================================

    /**
     * Send session custom variables
     */
    private void visitSessionVariables() {
        mAppAnalytics.visitVariable(1, "App Platform", "Android Platform");
        mAppAnalytics.visitVariable(2, "App Version", BuildConfig.VERSION_NAME);
        mAppAnalytics.visitVariable(4, "Chosen Language", VectorLocale.INSTANCE.getApplicationLocale().toString());

        final MXSession session = Matrix.getInstance(this).getDefaultSession();
        if (session != null) {
            mAppAnalytics.visitVariable(7, "Homeserver URL", session.getHomeServerConfig().getHomeserverUri().toString());
            String identityServerUrl = session.getIdentityServerManager().getIdentityServerUrl();
            if (identityServerUrl == null) {
                mAppAnalytics.visitVariable(8, "Identity Server URL", "");
            } else {
                mAppAnalytics.visitVariable(8, "Identity Server URL", identityServerUrl);
            }
        }
    }

    /**
     * A new activity has been resumed
     *
     * @param activity the new activity
     */
    private void onNewScreen(Activity activity) {
        final String screenPath = "/android/" + Matrix.getApplicationName()
                + "/" + BuildConfig.FLAVOR_DESCRIPTION
                + "/" + BuildConfig.VERSION_NAME
                + "/" + activity.getClass().getName().replace(".", "/");
        mAppAnalytics.trackScreen(screenPath, null);
    }

    /**
     * The application is paused.
     */
    private void onAppPause() {
        mDecryptionFailureTracker.dispatch();
        mAppAnalytics.forceDispatch();
        mNotificationDrawerManager.persistInfo();
    }
}
