package im.vector.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import im.vector.Matrix;
import im.vector.R;
import im.vector.fragments.MpditGotennaSettingsFragment;
import im.vector.fragments.PeopleFragment;

import android.content.Intent;
import android.os.Bundle;

import org.matrix.androidsdk.core.Log;

public class GotennaSettingsActivity extends VectorAppCompatActivity {

    static public final String TAG_FRAGMENT_GOTENNA_SETTINGS = "FRAGMENT_GOTENNA_SETTINGS";
    private FragmentManager mFragmentManager;
    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gotenna_settings);
    }*/

    @Override
    public int getLayoutRes() {
        return R.layout.activity_gotenna_settings;
    }

    @Override
    public int getTitleRes() {
        return R.string.room_sliding_menu_settings_mpdit_gotenna;
    }

    @Override
    public void initUiAndData() {
        configureToolbar();

        if (!isFirstCreation()) {

        }

        mFragmentManager = getSupportFragmentManager();
        MpditGotennaSettingsFragment fragment = (MpditGotennaSettingsFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_GOTENNA_SETTINGS);
        if (fragment == null) {
            fragment = MpditGotennaSettingsFragment.newInstance();
        }


        if (fragment != null) {

            try {
                mFragmentManager.beginTransaction()
                        .replace(R.id.vector_settings_page, fragment, TAG_FRAGMENT_GOTENNA_SETTINGS)
                        .addToBackStack(TAG_FRAGMENT_GOTENNA_SETTINGS)
                        .commit();
            } catch (Exception e) {
                //Log.e(LOG_TAG, "## updateSelectedFragment() failed : " + e.getMessage(), e);
            }
        }


    }

    @Override
    public int getMenuRes() {
        return R.menu.menu_country_picker;
    }
}