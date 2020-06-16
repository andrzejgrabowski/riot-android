package im.vector.fragments;


import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import im.vector.MpditManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.util.PreferencesManager;

import android.app.Application;

import androidx.preference.PreferenceManager;

public class MpditGotennaConnectionFragment extends VectorBaseFragment implements View.OnClickListener, MpditManager.GoTennaTechnicalMessageListener {


    public MpditManager getMpditManager() {
        VectorApp app = VectorApp.getInstance();
        if (app != null) {
            MpditManager mpdit = app.getMpditManger();
            if (mpdit != null) {
                mpdit.goTennaTechnicalMessageListener = this;
                return mpdit;
            }
        }
        return null;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_mpdit_gotenna_connection;
    }

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static MpditGotennaConnectionFragment newInstance() {
        return new MpditGotennaConnectionFragment();
    }

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //mPrimaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home);
        //mSecondaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home_secondary);
        //mFabColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms);
        //mFabPressedColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms_secondary);

        initViews();

        Button b = null;


        MpditManager mpdit = getMpditManager();

        b = getActivity().findViewById(R.id.goTennaInit);
        if(null != b)
        {
            b.setOnClickListener(this);
            if(mpdit != null) {
                if(!mpdit.goTennaNeedInit)
                    b.setVisibility(View.INVISIBLE);
            }
        }

        b = getActivity().findViewById(R.id.goTennaConnectLast);
        if(null != b) {
            b.setOnClickListener(this);
            if(mpdit != null) {
                b.setVisibility(View.INVISIBLE);
                if(!mpdit.goTennaNeedInit && mpdit.goTennaNeedConnect && mpdit.goTennaHasPreviousConnectionData)
                    b.setVisibility(View.VISIBLE);
            }
        }

        b = getActivity().findViewById(R.id.goTennaConnectNew);
        if(null != b) {
            b.setOnClickListener(this);
            if(mpdit != null) {
                b.setVisibility(View.INVISIBLE);
                if(!mpdit.goTennaNeedInit && mpdit.goTennaNeedConnect)
                b.setVisibility(View.VISIBLE);
            }
        }

        b = getActivity().findViewById(R.id.goTennaDisconnect);
        if(null != b) {
            b.setOnClickListener(this);
            if(mpdit != null) {
                if(mpdit.goTennaNeedConnect)
                    b.setVisibility(View.INVISIBLE);
            }
        }

        b = getActivity().findViewById(R.id.goTennaConnectTest);
        if(null != b) {
            b.setOnClickListener(this);
            if(mpdit != null) {
                if(mpdit.goTennaNeedConnect)
                    b.setVisibility(View.INVISIBLE);
            }
        }

        b = getActivity().findViewById(R.id.goTennaInfo);
        if(null != b) {
            b.setOnClickListener(this);
            if(mpdit != null) {
                if(mpdit.goTennaNeedConnect)
                    b.setVisibility(View.INVISIBLE);
            }
        }

        b = getActivity().findViewById(R.id.goTennaUpdate);
        if(null != b) {
            b.setOnClickListener(this);
            if(mpdit != null) {
                if(mpdit.goTennaNeedConnect)
                    b.setVisibility(View.INVISIBLE);
            }
        }


        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String gid = sp.getString(PreferencesManager.GOTENNA_SETTINGS_GID_PREFERENCE_KEY, "999666");
        String name = sp.getString(PreferencesManager.GOTENNA_SETTINGS_NAME_PREFERENCE_KEY, "999666");
        String gidMpdit = sp.getString(PreferencesManager.GOTENNA_SETTINGS_MPDIT_PREFERENCE_KEY, "999666");

        String control1 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_1_PREFERENCE_KEY, "145");
        String control2 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_2_PREFERENCE_KEY, "146");
        String control3 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_1_PREFERENCE_KEY, "147");

        String data1 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_1_PREFERENCE_KEY, "148");
        String data2 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_2_PREFERENCE_KEY, "149");
        String data3 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_3_PREFERENCE_KEY, "151");
        String data4 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_4_PREFERENCE_KEY, "152");
        String data5 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_5_PREFERENCE_KEY, "153");
        String data6 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_6_PREFERENCE_KEY, "154");
        String data7 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_7_PREFERENCE_KEY, "155");
        String data8 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_8_PREFERENCE_KEY, "156");
        String data9 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_9_PREFERENCE_KEY, "157");
        String data10 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_10_PREFERENCE_KEY, "158");
        String data11 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_11_PREFERENCE_KEY, "159");
        String data12 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_12_PREFERENCE_KEY, "160");
        String data13 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_13_PREFERENCE_KEY, "161");


        Toast.makeText(getActivity(), "GID: " + gid, Toast.LENGTH_SHORT).show();

        String power = sp.getString(PreferencesManager.GOTENNA_SETTINGS_POWER_PREFERENCE_KEY, "4");
        String bandwidth = sp.getString(PreferencesManager.GOTENNA_SETTINGS_BANDWIDTH_PREFERENCE_KEY, "4");

        //Toast.makeText(getActivity(), power + "  " + bandwidth, Toast.LENGTH_SHORT).show();




    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    private void initViews() {

    }

    @Override
    public void onClick(View v) {

        MpditManager mpdit = getMpditManager();
        if(mpdit == null) return;


        switch (v.getId()) {
            case R.id.goTennaInit: {
                    // TO DO !!!

                    //getApplicationContext()
                    boolean r = mpdit.goTennaInit(getContext());
                    Toast.makeText(getActivity(), mpdit.mLastExceptionGoTennaMessage, Toast.LENGTH_SHORT).show();
                    if(r)
                    {
                        Button b = getActivity().findViewById(R.id.goTennaConnectLast);
                        if(null != b) {
                            if(mpdit.goTennaHasPreviousConnectionData)
                                b.setVisibility(View.VISIBLE);
                            else
                                b.setVisibility(View.INVISIBLE);
                        }


                        b = getActivity().findViewById(R.id.goTennaConnectNew);
                        if(null != b) { b.setVisibility(View.VISIBLE);}

                        b = getActivity().findViewById(R.id.goTennaInit);
                        if(null != b) { b.setVisibility(View.INVISIBLE);}
                    }
                }
                break;


            case R.id.goTennaConnectLast: {
                boolean r = mpdit.goTennaConnectPrevious();
            }
                // TO DO !!!
                break;

            case R.id.goTennaConnectNew: {

                boolean r = mpdit.goTennaConnectNew();

                /*scanProgressDialog = new ProgressDialog(this);
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                scanProgressDialog.setMessage(getString(R.string.searching_for_gotenna));
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.show();*/

            }
                // TO DO !!!
                break;

            case R.id.goTennaDisconnect: {
                boolean r = mpdit.goTennaDisconnect();
                if(r)
                {
                    Button b = getActivity().findViewById(R.id.goTennaConnectLast);
                    if(null != b) {
                        if(mpdit.goTennaHasPreviousConnectionData)
                            b.setVisibility(View.VISIBLE);
                        else
                            b.setVisibility(View.INVISIBLE);
                    }

                    b = getActivity().findViewById(R.id.goTennaConnectNew);
                    if(null != b) { b.setVisibility(View.VISIBLE);}

                    b = getActivity().findViewById(R.id.goTennaDisconnect);
                    if(null != b) { b.setVisibility(View.INVISIBLE);}

                    b = getActivity().findViewById(R.id.goTennaConnectTest);
                    if(null != b) { b.setVisibility(View.INVISIBLE);}

                    b = getActivity().findViewById(R.id.goTennaInfo);
                    if(null != b) { b.setVisibility(View.INVISIBLE);}

                    b = getActivity().findViewById(R.id.goTennaUpdate);
                    if(null != b) { b.setVisibility(View.INVISIBLE);}
                }
            }
                // TO DO !!!
                break;

            case R.id.goTennaConnectTest: {
                boolean r = mpdit.goTennaTest();
            }
                // TO DO !!!
                break;

            case R.id.goTennaInfo: {
                boolean r = mpdit.goTennaGetSystemInfo();

            }
                // TO DO !!!
                break;

            case R.id.goTennaUpdate: {

            }
                // TO DO !!!
                break;
        }
    }

    @Override
    public void onNewGotennaTechnicalMessage() {
        MpditManager mpdit = getMpditManager();
        if(mpdit == null) return;

        if(mpdit.mGoTennaHasNewMessage)
        {
            Toast.makeText(getActivity(), mpdit.mGoTennaLastMessage, Toast.LENGTH_SHORT).show();
            mpdit.mGoTennaHasNewMessage = false;
        }
    }

    @Override
    public void onGotennaTechnicalMessageConnected() {
        {
            Button b = getActivity().findViewById(R.id.goTennaConnectLast);
            if(null != b) { b.setVisibility(View.INVISIBLE);}

            b = getActivity().findViewById(R.id.goTennaConnectNew);
            if(null != b) { b.setVisibility(View.INVISIBLE);}

            b = getActivity().findViewById(R.id.goTennaDisconnect);
            if(null != b) { b.setVisibility(View.VISIBLE);}

            b = getActivity().findViewById(R.id.goTennaConnectTest);
            if(null != b) { b.setVisibility(View.VISIBLE);}

            b = getActivity().findViewById(R.id.goTennaInfo);
            if(null != b) { b.setVisibility(View.VISIBLE);}

            b = getActivity().findViewById(R.id.goTennaUpdate);
            if(null != b) { b.setVisibility(View.VISIBLE);}
        }
    }

    @Override
    public void onNewGotennaStatusMessage() {

        MpditManager mpdit = getMpditManager();
        if(mpdit == null) return;

        new AlertDialog.Builder(getContext())
                .setMessage(mpdit.mGoTennaStatus)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }
}
