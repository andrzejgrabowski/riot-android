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

import androidx.preference.PreferenceManager;

import java.util.Objects;

public class MpditGotennaConnectionFragment extends VectorBaseFragment implements View.OnClickListener, MpditManager.GoTennaTechnicalMessageListener {

    final String GOTENNA_HAS_PREVIOUS_CONNECTIONE_KEY = "GOTENNA_HAS_PREVIOUS_CONNECTIONE_KEY";

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
    public void onDetach()
    {
        super.onDetach();
        MpditManager mpdit = getMpditManager();
        if(mpdit != null)
            mpdit.goTennaTechnicalMessageListener = null;
    }



    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        MpditManager mpdit = getMpditManager();
        if(mpdit != null)
            mpdit.goTennaTechnicalMessageListener = null;
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        // sprawdzamy czy jest ustawiano informacja o wcześniejszym połaćzeniu z gotenną
        if(mpdit != null)
        {
            mpdit.goTennaHasPreviousConnectionData = sp.getBoolean(GOTENNA_HAS_PREVIOUS_CONNECTIONE_KEY, false);
        }

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



        // przepisujemy wartości zapisane z pliku
        UpdateGoTennaData();


    }

    private void UpdateGoTennaData()
    {
        MpditManager mpdit = getMpditManager();
        if(mpdit == null)
            return;

        mpdit.goTennaUpdateDataFromSharedPreferences(getContext());


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
                break;

            case R.id.goTennaConnectNew: {

                boolean r = mpdit.goTennaConnectNew();
                /*scanProgressDialog = new ProgressDialog(this);
                scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                scanProgressDialog.setMessage(getString(R.string.searching_for_gotenna));
                scanProgressDialog.setCancelable(false);
                scanProgressDialog.show();*/
            }
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
                break;

            case R.id.goTennaConnectTest: {
                boolean r = mpdit.goTennaTest();
            }
                break;

            case R.id.goTennaInfo: {
                boolean r = mpdit.goTennaGetSystemInfo();
            }
                break;

            case R.id.goTennaUpdate:
                UpdateGoTennaData();
                mpdit.goTennaUpdateConnectedParameters();
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

        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()));
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(GOTENNA_HAS_PREVIOUS_CONNECTIONE_KEY, true);
            //editor.commit();
            editor.apply();

            MpditManager mpdit = getMpditManager();
            if (mpdit != null) {
                mpdit.goTennaHasPreviousConnectionData = true;
            }
        } catch (Exception e) {}
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
