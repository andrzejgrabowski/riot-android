package im.vector.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.MXPatterns;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.groups.GroupsManager;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.model.group.Group;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import im.vector.MeshNode;
import im.vector.MpditManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.VectorGroupDetailsActivity;
import im.vector.adapters.AbsAdapter;
import im.vector.adapters.GroupAdapter;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import im.vector.contacts.PIDsRetriever;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.HomeRoomsViewModel;
import im.vector.util.SystemUtilsKt;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SimpleDividerItemDecoration;


import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
//import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

import static android.graphics.Color.parseColor;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;



import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;




public class MpditMapFragment extends AbsHomeFragment  implements PermissionsListener, OnMapReadyCallback, View.OnClickListener, MapboxMap.OnMapClickListener {
    private static final String LOG_TAG = GroupsFragment.class.getSimpleName();


    // kontakty
    private List<Room> mDirectChats = new ArrayList<>();

    // MAPBOX-MPDIT
    private static final String LAYER_ID_UBIQUITY = "UBIQUITY_LAYER_ID";
    private static final String LAYER_ID_GOTENNA = "GOTENNA_LAYER_ID";
    private static final String LAYER_ID_MPDIT = "MPDIT_LAYER_ID";

    private static final String UBIQUITY_ICON_ID = "UBIQUITY_ICON_ID";
    private static final String GOTENNA_ICON_ID = "GOTENNA_ICON_ID";
    private static final String MPDIT_ICON_ID = "MPDIT_ICON_ID";

    private static final String SOURCE_GOTENNA_ID = "SOURCE_GOTENNA_ID";
    private static final String SOURCE_MPDIT_ID = "SOURCE_MPDIT_ID";
    private static final String SOURCE_UBIQUITY_ID = "SOURCE_UBIQUITY_ID";


    private static final String CIRCLE_LAYER_ID = "CIRCLE_LAYER_ID";
    private static final String MARKER_LAYER_ID = "MARKER_LAYER_ID";
    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String MARKER_ICON_ID = "MARKER_ICON_ID";
    private static final String PROPERTY_ID = "PROPERTY_ID";
    private static final String PROPERTY_SELECTED = "PROPERTY_SELECTED";
    private static final String PROPERTY_VISIBLE = "PROPERTY_VISIBLE";

    private MapboxMap mapboxMap = null;
    private FeatureCollection featureCollection;
    private FeatureCollection ubiquityCollection;
    private FeatureCollection gotennaCollection;
    private FeatureCollection mpditCollection;

    private static final int UBIQUITY_TABLE = 0;
    private static final int GOTENNA_TABLE = 1;
    private static final int MPDIT_TABLE = 2;
    private int mSelctedID = -1;
    private int mSelectedTable = -1;
    private String mSelectedProprtyID = "-1";

    private PermissionsManager permissionsManager = null;
    SupportMapFragment mapFragment = null;
    FragmentTransaction transaction= null;
    long startTime = 0;
    TextView mTextViewLatLng = null;
    Button mButtonMapObject = null;






    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long milis = System.currentTimeMillis() - startTime;
            int seconds = (int) (milis/1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            // odświerzamy dane dotyczące położeń
            initFeatureCollection();
            refreshSource();


            if(mTextViewLatLng != null) {
                try {
                    VectorApp app = VectorApp.getInstance();
                    if (app != null) {
                        MpditManager mpdit = app.getMpditManger();
                        if (mpdit != null) {
                            //mTextViewLatLng.setText(String.format("%d:%02d  %.5f  %.5f", minutes, seconds, mpdit.mLat, mpdit.mLng));

                            mpdit.sendGpsData();

                            //mTextViewLatLng.setText(String.format("%d %d -- %d %d : ",mpdit.getUbiquityNodes().size(),mpdit.getGotennaNodes().size(),mSelectedTable, mSelctedID ) + mSelectedProprtyID);

                            //String s = mpdit.mLastExceptionMessage + " packet: " + mpdit.mLastPacket;
                            //mTextViewLatLng.setText(s);

                        } else {
                            mTextViewLatLng.setText("mpdit=null");
                        }

                    } else {
                        mTextViewLatLng.setText("app=null");
                    }

                } catch (Exception e) {
                    mTextViewLatLng.setText(e.getMessage());
                }
            }
            timerHandler.postDelayed(this,2500);
        }
    };


    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    // groups management
    private GroupAdapter mAdapter;
    private GroupsManager mGroupsManager;

    // rooms list
    private final List<Group> mJoinedGroups = new ArrayList<>();
    private final List<Group> mInvitedGroups = new ArrayList<>();

    // refresh when there is a group event
    private final MXEventListener mEventListener = new MXEventListener() {
        @Override
        public void onNewGroupInvitation(String groupId) {
            //refreshGroups();
        }

        @Override
        public void onJoinGroup(String groupId) {
            //refreshGroups();
        }

        @Override
        public void onLeaveGroup(String groupId) {
            //refreshGroups();
        }
    };

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static MpditMapFragment newInstance() {
        return new MpditMapFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_groups;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //mGroupsManager = mSession.getGroupsManager();
        mPrimaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home);
        mSecondaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home_secondary);

        mFabColor = ContextCompat.getColor(getActivity(), R.color.tab_groups);
        mFabPressedColor = ContextCompat.getColor(getActivity(), R.color.tab_groups_secondary);

        initViews();

        Button b1 = getActivity().findViewById(R.id.buttonMapBoxCenter);
        b1.setOnClickListener(this);
        Button b2 = getActivity().findViewById(R.id.buttonMapBoxShowAll);
        b2.setOnClickListener(this);

        //mAdapter.onFilterDone(mCurrentFilter);

        //CreateMapBox();

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable,1500);
        mTextViewLatLng = getActivity().findViewById(R.id.map_box_lat_lng);
        mButtonMapObject = getActivity().findViewById(R.id.buttonMapBoxObject);

        if(mButtonMapObject != null)
            mButtonMapObject.setVisibility(View.INVISIBLE);

        VectorApp app = VectorApp.getInstance();
        if (app != null) {
            MpditManager mpdit = app.getMpditManger();
            if (mpdit != null) {
                String s = mpdit.mDisplayedName + " - " + mpdit.mID;
                mTextViewLatLng.setText(s);
            }
        }

        // TWORZYMY KOLEKCJE KONTAKTOW
        Collection<Contact> contacts = ContactsManager.getInstance().getLocalContactsSnapshot();
        if (null != contacts) {
            String c = "Kontakty: ";
            for (Contact contact : contacts) {
                c += contact.getDisplayName();

                for (String email : contact.getEmails()) {
                    Contact.MXID mxid = PIDsRetriever.getInstance().getMXID(email);

                    if (null != mxid) {
                        String userId = mxid.mMatrixId;
                        c += userId;
                    } else {
                        c += email;
                    }
                }
            }
            mTextViewLatLng.setText(c);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //mSession.getDataHandler().addListener(mEventListener);
        //mRecycler.addOnScrollListener(mScrollListener);
        //refreshGroupsAndProfiles();
        //Toast.makeText(mActivity, "Map: OnResume", Toast.LENGTH_SHORT).show();
        CreateMapBox();
    }

    @Override
    public void onPause() {
        super.onPause();
        //mSession.getDataHandler().removeListener(mEventListener);
        //mRecycler.removeOnScrollListener(mScrollListener);
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    @Override
    public void onRoomResultUpdated(final HomeRoomsViewModel.Result result) {
        if (isResumed()) {
            //mAdapter.setInvitation(mActivity.getRoomInvitations());
            mDirectChats = result.getDirectChatsWithFavorites();
            //mAdapter.setRooms(mDirectChats);
        }
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */


    @Override
    protected List<Room> getRooms() {
        return new ArrayList<>(mDirectChats);
    }

    @Override
    protected void onFilter(String pattern, final OnFilterListener listener) {
        /*mAdapter.getFilter().filter(pattern, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onFilterComplete " + count);
                if (listener != null) {
                    listener.onFilterDone(count);
                }
            }
        });*/
    }

    @Override
    protected void onResetFilter() {
        /*mAdapter.getFilter().filter("", new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onResetFilter " + count);
            }
        });*/
    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    private void initViews() {
        int margin = (int) getResources().getDimension(R.dimen.item_decoration_left_margin);




    }



    @Override
    public boolean onFabClick() {

        return true;
    }


    /*
    MAP BOX
     */




    public void onClick(final View v) { //check for what button is pressed
        switch (v.getId()) {
            case R.id.buttonMapBoxObject:
                // wywolujemy komunikacje z wybranym na mapie użytkownikiem
                // TO DO !!!
                break;
            case R.id.buttonMapBoxCenter:
                //Toast.makeText(mActivity, "Centruj", Toast.LENGTH_SHORT).show();
                {
                    LatLng ll = new LatLng(51.50550, -0.07520);
                    Location l = mapboxMap.getLocationComponent().getLastKnownLocation();
                    ll.setLatitude(l.getLatitude());
                    ll.setLongitude(l.getLongitude());

                    CameraPosition position = new CameraPosition.Builder()
                            .target(ll) // Sets the new camera position
                            .zoom(17) // Sets the zoom
                            .bearing(180) // Rotate the camera
                            .tilt(0) // Set the camera tilt
                            .build(); // Creates a CameraPosition from the builder

                    mapboxMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(position), 7000);
                }
                break;
            case R.id.buttonMapBoxShowAll:
                //Toast.makeText(mActivity, "Pokaż wszystko", Toast.LENGTH_SHORT).show();
                {
                    Location l = mapboxMap.getLocationComponent().getLastKnownLocation();
                    double minLat = l.getLatitude() - 0.001;
                    double maxLat = l.getLatitude() + 0.001;
                    double minLng = l.getLongitude() - 0.001;
                    double maxLng = l.getLongitude() + 0.001;
                    VectorApp app = VectorApp.getInstance();
                    if (app != null) {
                        MpditManager mpdit = app.getMpditManger();
                        if (mpdit != null) {
                            // petla po ubiquity
                            {
                                Vector<MeshNode> nodes = mpdit.getUbiquityNodes();
                                for(int i=0; i<nodes.size(); i++)
                                {
                                    MeshNode n = nodes.get(i);
                                    if(n.lat > maxLat)      maxLat = n.lat;
                                    if(n.lat < minLat)      minLat = n.lat;
                                    if(n.lng > maxLng)      maxLng = n.lng;
                                    if(n.lng < minLng)      minLng = n.lng;
                                }
                            }
                            // petla po gotenna
                            {
                                Vector<MeshNode> nodes = mpdit.getGotennaNodes();
                                for(int i=0; i<nodes.size(); i++)
                                if(nodes.get(i).visibleOnMap)
                                {
                                    MeshNode n = nodes.get(i);
                                    if(n.lat > maxLat)      maxLat = n.lat;
                                    if(n.lat < minLat)      minLat = n.lat;
                                    if(n.lng > maxLng)      maxLng = n.lng;
                                    if(n.lng < minLng)      minLng = n.lng;
                                }
                            }
                            // petla po mpdit
                            {
                                Vector<MeshNode> nodes = mpdit.getMpditNodes();
                                for(int i=0; i<nodes.size(); i++)
                                    if(nodes.get(i).visibleOnMap)
                                {
                                    MeshNode n = nodes.get(i);
                                    if(n.lat > maxLat)      maxLat = n.lat;
                                    if(n.lat < minLat)      minLat = n.lat;
                                    if(n.lng > maxLng)      maxLng = n.lng;
                                    if(n.lng < minLng)      minLng = n.lng;
                                }
                            }
                        }
                    }
                    /*
                    for (int x = 0; x < featureCollection.features().size(); x++) {
                        Feature f = featureCollection.features().get(x);

                    }*/

                    LatLng locationOne = new LatLng(minLat, minLng);
                    LatLng locationTwo = new LatLng(maxLat, maxLng);

                    LatLngBounds latLngBounds = new LatLngBounds.Builder()
                            .include(locationOne) // Northeast
                            .include(locationTwo) // Southwest
                            .build();

                    mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50), 3000);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Tworzy mapę MapBox: MPDIT
     */

    private void CreateMapBox()
    {
        int d = 5;

        //Bundle savedInstanceState = null;
        //Bundle savedInstanceState = getSavedInstanceState();

        // Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(mActivity, getString(R.string.mapbox_access_token));

// Create supportMapFragment
        //String tost = "mamy mapfragment";
        //SupportMapFragment mapFragment = null;
        mapFragment = (SupportMapFragment) mActivity.getSupportFragmentManager().findFragmentByTag("com.mapbox.map");
        if (mapFragment != null)
        {
            // TO DO !!!
            // trzeba usunąć stary fragment (?)
        }
        //Toast.makeText(this, tost, Toast.LENGTH_SHORT).show();
        if(true) //(mapFragment == null)
        {
// Create fragment
            //Toast.makeText(this, "FragmentTransaction", Toast.LENGTH_SHORT).show();
            transaction = mActivity.getSupportFragmentManager().beginTransaction();

// Build mapboxMap
            //Toast.makeText(this, "MapboxMapOptions", Toast.LENGTH_SHORT).show();
            MapboxMapOptions options = MapboxMapOptions.createFromAttributes(mActivity, null);

            //Toast.makeText(this, "options", Toast.LENGTH_SHORT).show();
            options.camera(new CameraPosition.Builder()
                    .target(new LatLng(-52.6885, -70.1395))
                    .zoom(9)
                    .build());

// Create map fragment
            mapFragment = SupportMapFragment.newInstance(options);

            if (mapFragment == null)
                Toast.makeText(mActivity, "mapFragment=NULL", Toast.LENGTH_SHORT).show();

// Add map fragment to parent container
            transaction.add(R.id.containerMapBox, mapFragment, "com.mapbox.map");
            try {
                transaction.commit();//AllowingStateLoss();
            } catch (Exception e) {
                Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } else {
            //mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentByTag("com.mapbox.map");
            //transaction.commit();
        }



        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }


    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMapX) {
        mapboxMap = mapboxMapX;
        mapboxMap.setStyle(Style.SATELLITE, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                initFeatureCollection();

                //style.addSource(new GeoJsonSource(SOURCE_ID, featureCollection));
                style.addSource(new GeoJsonSource(SOURCE_GOTENNA_ID, gotennaCollection));
                style.addSource(new GeoJsonSource(SOURCE_UBIQUITY_ID, ubiquityCollection));
                style.addSource(new GeoJsonSource(SOURCE_MPDIT_ID, mpditCollection));

// Add the CircleLayer and set the filter so that circle are only shown
// if the PROPERTY_SELECTED boolean property is false.
                /*CircleLayer circleLayer = new CircleLayer(CIRCLE_LAYER_ID, SOURCE_ID)
                        .withProperties(
                                circleRadius(interpolate(linear(), zoom(),
                                        stop(2, 5f),
                                        stop(3, 20f)
                                )),
                                circleColor(parseColor("#2196F3")));
                circleLayer.setFilter(eq(get(PROPERTY_SELECTED), literal(false)));
                style.addLayer(circleLayer);*/

// Add the marker icon image to the map
                //style.addImage(MARKER_ICON_ID, BitmapFactory.decodeResource(
                //        MpditMapFragment.this.getResources(), R.drawable.blue_marker_view));

                style.addImage(UBIQUITY_ICON_ID, BitmapFactory.decodeResource(
                        MpditMapFragment.this.getResources(), R.drawable.marker_ubiquity));

                style.addImage(GOTENNA_ICON_ID, BitmapFactory.decodeResource(
                        MpditMapFragment.this.getResources(), R.drawable.marker_gotenna));

                style.addImage(MPDIT_ICON_ID, BitmapFactory.decodeResource(
                        MpditMapFragment.this.getResources(), R.drawable.marker_mpdit));

// Add the SymbolLayer and set the filter so that circle are only shown
// if the PROPERTY_SELECTED boolean property is true.
                SymbolLayer symbolLayerUbiquity = new SymbolLayer(LAYER_ID_UBIQUITY, SOURCE_UBIQUITY_ID)
                        .withProperties(iconImage(UBIQUITY_ICON_ID),
                                iconAllowOverlap(true),
                                iconOffset(new Float[] {0f, -13f})
                        );
                symbolLayerUbiquity.setFilter(eq(get(PROPERTY_VISIBLE), literal(true)));
                style.addLayer(symbolLayerUbiquity);

// Add the SymbolLayer and set the filter so that circle are only shown
// if the PROPERTY_SELECTED boolean property is true.
                SymbolLayer symbolLayerGotenna = new SymbolLayer(LAYER_ID_GOTENNA, SOURCE_GOTENNA_ID)
                        .withProperties(iconImage(GOTENNA_ICON_ID),
                                iconAllowOverlap(true),
                                iconOffset(new Float[] {0f, -13f})
                        );
                symbolLayerGotenna.setFilter(eq(get(PROPERTY_VISIBLE), literal(true)));
                style.addLayer(symbolLayerGotenna);


// Add the SymbolLayer and set the filter so that circle are only shown
// if the PROPERTY_SELECTED boolean property is true.
                SymbolLayer symbolLayerMpdit = new SymbolLayer(LAYER_ID_MPDIT, SOURCE_MPDIT_ID)
                        .withProperties(iconImage(MPDIT_ICON_ID),
                                iconAllowOverlap(true),
                                iconOffset(new Float[] {0f, -13f})
                        );
                symbolLayerMpdit.setFilter(eq(get(PROPERTY_VISIBLE), literal(true)));
                style.addLayer(symbolLayerMpdit);



                mapboxMap.addOnMapClickListener(MpditMapFragment.this);

// Map is set up and the style has loaded. Now you can add data or make other map adjustments
                enableLocationComponent(style);


            }
        });
    }


    /**
     * Create sample data to use for both the {@link CircleLayer} and
     * {@link SymbolLayer}.
     */
    private void initFeatureCollection() {
        List<Feature> markerCoordinatesUbiquity = new ArrayList<>();
        List<Feature> markerCoordinatesGotenna = new ArrayList<>();
        List<Feature> markerCoordinatesMpdit = new ArrayList<>();

        VectorApp app = VectorApp.getInstance();
        if (app != null) {
            MpditManager mpdit = app.getMpditManger();
            if (mpdit != null) {
                // petla po ubiquity
                {
                    Vector<MeshNode> nodes = mpdit.getUbiquityNodes();
                    for(int i=0; i<nodes.size(); i++) {
                        MeshNode n = nodes.get(i);
                        Feature featureOne = Feature.fromGeometry(
                                Point.fromLngLat(n.lng, n.lat));
                        featureOne.addStringProperty(PROPERTY_ID, Integer.toString(i));
                        featureOne.addBooleanProperty(PROPERTY_SELECTED, false);
                        featureOne.addBooleanProperty(PROPERTY_VISIBLE, true);
                        markerCoordinatesUbiquity.add(featureOne);
                    }
                }

                // petla po gotenna
                {
                    Vector<MeshNode> nodes = mpdit.getGotennaNodes();
                    for(int i=0; i<nodes.size(); i++)
                    if(nodes.get(i).visibleOnMap)  {
                        MeshNode n = nodes.get(i);
                        Feature featureOne = Feature.fromGeometry(
                                Point.fromLngLat(n.lng, n.lat));
                        featureOne.addStringProperty(PROPERTY_ID, Integer.toString(i));
                        featureOne.addBooleanProperty(PROPERTY_SELECTED, false);
                        featureOne.addBooleanProperty(PROPERTY_VISIBLE, true);
                        markerCoordinatesGotenna.add(featureOne);
                    }
                }

                // petla po mpdit
                {
                    Vector<MeshNode> nodes = mpdit.getMpditNodes();
                    for(int i=0; i<nodes.size(); i++)
                        if(nodes.get(i).visibleOnMap) {
                        MeshNode n = nodes.get(i);
                        Feature featureOne = Feature.fromGeometry(
                                Point.fromLngLat(n.lng, n.lat));
                        featureOne.addStringProperty(PROPERTY_ID, Integer.toString(i));
                        featureOne.addBooleanProperty(PROPERTY_SELECTED, false);
                        featureOne.addBooleanProperty(PROPERTY_VISIBLE, true);
                        markerCoordinatesMpdit.add(featureOne);
                    }
                }
            }
        }

        /*
        Feature featureOne = Feature.fromGeometry(
                Point.fromLngLat(45.37353515625, -14.32825967774));
        featureOne.addStringProperty(PROPERTY_ID, "1");
        featureOne.addBooleanProperty(PROPERTY_SELECTED, false);
        markerCoordinates.add(featureOne);

        Feature featureTwo = Feature.fromGeometry(
                Point.fromLngLat(50.1416015625, -20.200346006493735));
        featureTwo.addStringProperty(PROPERTY_ID, "2");
        featureTwo.addBooleanProperty(PROPERTY_SELECTED, false);
        markerCoordinates.add(featureTwo);

        Feature featureThree = Feature.fromGeometry(
                Point.fromLngLat(42.86865234375, -24.266997288418157));
        featureThree.addStringProperty(PROPERTY_ID, "3");
        featureThree.addBooleanProperty(PROPERTY_SELECTED, false);
        markerCoordinates.add(featureThree);
        */


        ubiquityCollection = FeatureCollection.fromFeatures(markerCoordinatesUbiquity);
        gotennaCollection = FeatureCollection.fromFeatures(markerCoordinatesGotenna);
        mpditCollection = FeatureCollection.fromFeatures(markerCoordinatesMpdit);
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
        return true;
    }


    /*
    Funkcja wyświetla przyciski umożłiwiajace komunikację z wybranym na mapie użytkownikiem
     */
    private void ShowCommunicationButtons()
    {
        String text = "-";
        VectorApp app = VectorApp.getInstance();
        if (app != null) {
            MpditManager mpdit = app.getMpditManger();
            if (mpdit != null) {

                if (UBIQUITY_TABLE == mSelectedTable) {
                    Vector<MeshNode> nodes = mpdit.getUbiquityNodes();
                    if (mSelctedID >= 0 && mSelctedID < nodes.size()) {
                        MeshNode node = nodes.get(mSelctedID);
                        text = String.format("Ubiquity: ") + node.name;
                    }
                }

                if (GOTENNA_TABLE == mSelectedTable) {
                    Vector<MeshNode> nodes = mpdit.getGotennaNodes();
                        if (mSelctedID >= 0 && mSelctedID < nodes.size()) {
                            MeshNode node = nodes.get(mSelctedID);
                            text = String.format("GoTenna: ") + node.name;
                        }
                }

                if (MPDIT_TABLE == mSelectedTable) {
                    Vector<MeshNode> nodes = mpdit.getMpditNodes();
                        if (mSelctedID >= 0 && mSelctedID < nodes.size()) {
                            MeshNode node = nodes.get(mSelctedID);
                            text = String.format("Mpdit: ") + node.name;
                        }
                    }


            }
        }

        if(mTextViewLatLng != null)
        {
            mTextViewLatLng.setText("-");//text);
        }

        if(mButtonMapObject != null)
        {
            mButtonMapObject.setText(text);
            mButtonMapObject.setVisibility(View.VISIBLE);
        }
    }


    /*
    Funkcja ukrywa przyciski umożliwiajace komunikację z wybranym na mapie użytkownikiem
     */
    private void HideCommunicationButtons()
    {
        if(mButtonMapObject != null)
        {
            mButtonMapObject.setText("-");
            mButtonMapObject.setVisibility(View.INVISIBLE);
        }

        if(mTextViewLatLng != null)
        {
            mTextViewLatLng.setText(" - ");

            String c = "kontakty: ";
            for(int i=0; i < mDirectChats.size(); i++)
            {
                c += i + ": ";
                c += mDirectChats.get(i).getRoomId();
                //mDirectChats.get(i).getD
            }

            mTextViewLatLng.setText(c);
        }
    }

    /**
     * This method handles click events for both layers.
     * <p>
     * The PROPERTY_SELECTED feature property is set to its opposite, so
     * that the visual toggling between circles and icons is correct.
     *
     * @param screenPoint the point on screen clicked
     */
    private boolean handleClickIcon(PointF screenPoint) {

        boolean somethingIsSelected = false;

        List<Feature> ubiquityList = mapboxMap.queryRenderedFeatures(screenPoint, LAYER_ID_UBIQUITY);
        if (!ubiquityList.isEmpty()) {
            Feature selectedFeature = ubiquityList.get(0);
            String s = selectedFeature.getStringProperty(PROPERTY_ID);
            mSelectedProprtyID = s;
            try {
                mSelctedID = Integer.parseInt(s);
            } catch (Exception e) {}
            mSelectedTable = UBIQUITY_TABLE;
            ShowCommunicationButtons();
            return true;
        }

        List<Feature> gotennaList = mapboxMap.queryRenderedFeatures(screenPoint, LAYER_ID_GOTENNA);
        if (!gotennaList.isEmpty()) {
            Feature selectedFeature = gotennaList.get(0);
            String s = selectedFeature.getStringProperty(PROPERTY_ID);
            mSelectedProprtyID = s;
            try {
                mSelctedID = Integer.parseInt(s);
            } catch (Exception e) {}
            mSelectedTable = GOTENNA_TABLE;
            ShowCommunicationButtons();
            return true;
        }

        List<Feature> mpditList = mapboxMap.queryRenderedFeatures(screenPoint, LAYER_ID_MPDIT);
        if (!mpditList.isEmpty()) {
            Feature selectedFeature = mpditList.get(0);
            String s = selectedFeature.getStringProperty(PROPERTY_ID);
            mSelectedProprtyID = s;
            try {
                mSelctedID = Integer.parseInt(s);
            } catch (Exception e) {}
            mSelectedTable = MPDIT_TABLE;
            ShowCommunicationButtons();
            return true;
        }


        if(!somethingIsSelected)
        {
            mSelectedProprtyID = "-1";
            mSelectedTable = -1;
            HideCommunicationButtons();

// Reset all features to unselected so that all circles are shown and no icons are shown

            /*for (int x = 0; x < ubiquityCollection.features().size(); x++) {
                setFeatureSelectState(x, ubiquityCollection.features().get(x), false);
            }

            for (int x = 0; x < gotennaCollection.features().size(); x++) {
                setFeatureSelectState(x, gotennaCollection.features().get(x), false);
            }

            for (int x = 0; x < mpditCollection.features().size(); x++) {
                setFeatureSelectState(x, mpditCollection.features().get(x), false);
            }*/
        }


        return true;
    }

    /**
     * Set a feature selected state.
     *
     * @param index the index of selected feature
     */
    private void setSelected(int index) {
        if (featureCollection.features() != null) {
            Feature feature = featureCollection.features().get(index);
            setFeatureSelectState(index, feature, true);
            refreshSource();
        }
    }

    /**
     * Updates the display of data on the map after the FeatureCollection has been modified
     */
    private void refreshSource() {
        mapboxMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                /*GeoJsonSource geoJsonSource = style.getSourceAs(SOURCE_ID);
                if (geoJsonSource != null && featureCollection != null) {
                    geoJsonSource.setGeoJson(featureCollection);
                }*/

                GeoJsonSource geoJsonSourceUbiquity = style.getSourceAs(SOURCE_UBIQUITY_ID);
                if (geoJsonSourceUbiquity != null && ubiquityCollection != null) {
                    geoJsonSourceUbiquity.setGeoJson(ubiquityCollection);
                }

                GeoJsonSource geoJsonSourceGotenna = style.getSourceAs(SOURCE_GOTENNA_ID);
                if (geoJsonSourceGotenna != null && gotennaCollection != null) {
                    geoJsonSourceGotenna.setGeoJson(gotennaCollection);
                }

                GeoJsonSource geoJsonSourceMpdit = style.getSourceAs(SOURCE_MPDIT_ID);
                if (geoJsonSourceMpdit != null && mpditCollection != null) {
                    geoJsonSourceMpdit.setGeoJson(mpditCollection);
                }
            }
        });
    }

    /**
     * Selects the state of a feature
     *
     * @param feature the feature to be selected.
     */
    private void setFeatureSelectState(int index, Feature feature, boolean selectedState) {
        feature.addBooleanProperty(PROPERTY_SELECTED, selectedState);
        featureCollection.features().set(index, feature);
        refreshSource();
    }

    /**
     * Checks whether a Feature's boolean "selected" property is true or false
     *
     * @param selectedFeature the specific Feature to check
     * @return true if "selected" is true. False if the boolean property is false.
     */
    private boolean featureSelectStatusIsTrue(Feature selectedFeature) {
        if (featureCollection == null) {
            return false;
        }
        return selectedFeature.getBooleanProperty(PROPERTY_SELECTED);
    }



    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(mActivity)) {

// Get an instance of the LocationComponent.
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate the LocationComponent
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(mActivity, loadedMapStyle).build());

// Enable the LocationComponent so that it's actually visible on the map
            locationComponent.setLocationComponentEnabled(true);

// Set the LocationComponent's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the LocationComponent's render mode
            locationComponent.setRenderMode(RenderMode.NORMAL);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(mActivity);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(mActivity, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(mActivity, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            mActivity.finish();
        }
    }
}





