package im.vector.fragments;

import android.os.Bundle;


import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import im.vector.R;
import im.vector.util.PreferencesManager;


public class MpditGotennaSettingsFragment extends PreferenceFragmentCompat {
    //EditTextPreference mMpditGid = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        //setPreferencesFromResource(R.xml.mpdit_gotenna_settings_preferences, rootKey);

        //EditTextPreference edp = (EditTextPreference) findPreference(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY);
        //edp.setSummary("test summary");

        //EditTextPreference gid = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_GID_PREFERENCE_KEY);
        //gid.setSummary(gid.getText());

        //mMpditGid = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_MPDIT_GID_PREFERENCE_KEY);
        //gid.setSummary(mMpditGid.getText());
        /*mMpditGid.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                //mMpditGid.setSummary(newValue.toString());
                return false;
            }
        });*/

    }

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static MpditGotennaSettingsFragment newInstance() {
        return new MpditGotennaSettingsFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    //@Override
    public int getLayoutResId() {
        return R.layout.fragment_home;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //mPrimaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home);
        //mSecondaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home_secondary);
        //mFabColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms);
        //mFabPressedColor = ContextCompat.getColor(getActivity(), R.color.tab_rooms_secondary);

        initViews();

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
}
