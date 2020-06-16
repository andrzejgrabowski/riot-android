package im.vector.fragments;

import android.os.Bundle;


import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import im.vector.R;
import im.vector.util.PreferencesManager;


public class MpditGotennaSettingsFragment extends PreferenceFragmentCompat {
    //public EditTextPreference mMpditGid = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.mpdit_gotenna_settings_preferences, rootKey);

        //EditTextPreference edp = (EditTextPreference) findPreference(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY);
        //edp.setSummary("test summary");

        Preference.OnPreferenceChangeListener pl = (preference, newValue) -> {
            preference.setSummary(newValue.toString());
            return true;
        };

        Preference.OnPreferenceChangeListener pls = (preference, newValue) -> {
            preference.setSummary(newValue.toString());
            return true;
        };


        /*
        mMpditGid.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(mMpditGid != null)
                    mMpditGid.setSummary(newValue.toString());
                return false;
            }
        });*/




        EditTextPreference gid = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_GID_PREFERENCE_KEY);
        if(gid != null) {
            gid.setSummary(gid.getText());
            gid.setOnPreferenceChangeListener(pl);
        }

        EditTextPreference name = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_NAME_PREFERENCE_KEY);
        if(name != null) {
            name.setSummary(name.getText());
            name.setOnPreferenceChangeListener(pl);
        }

        EditTextPreference mpdit = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_MPDIT_PREFERENCE_KEY);
        if(mpdit != null) {
            mpdit.setSummary(mpdit.getText());
            mpdit.setOnPreferenceChangeListener(pl);
        }



        ListPreference power = (ListPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_POWER_PREFERENCE_KEY);
        power.setSummary(power.getValue());
        power.setOnPreferenceChangeListener(pls);

        ListPreference bandwidth = (ListPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_BANDWIDTH_PREFERENCE_KEY);
        bandwidth.setSummary(bandwidth.getValue());
        bandwidth.setOnPreferenceChangeListener(pls);

        EditTextPreference frequency;

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_1_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_2_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_3_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_1_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_2_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_3_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_4_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_5_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_6_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_7_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_8_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_9_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_10_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_11_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_12_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }

        frequency = (EditTextPreference) findPreference(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_13_PREFERENCE_KEY);
        if(frequency != null) {
            frequency.setSummary(frequency.getText());
            frequency.setOnPreferenceChangeListener(pl);
        }


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
