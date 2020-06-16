/*package im.vector.activity;

public class GotennaConnectionActivity {
}
*/

package im.vector.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import im.vector.Matrix;
import im.vector.R;
import im.vector.fragments.MpditGotennaConnectionFragment;


public class GotennaConnectionActivity extends VectorAppCompatActivity {

    static public final String TAG_FRAGMENT_GOTENNA_CONNECTION = "FRAGMENT_GOTENNA_CONNECTION";
    private FragmentManager mFragmentManager;
    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gotenna_settings);
    }*/

    @Override
    public int getLayoutRes() {
        return R.layout.activity_gotenna_connection;
    }

    @Override
    public int getTitleRes() {
        return R.string.room_sliding_menu_connection_mpdit_gotenna;
    }

    @Override
    public void initUiAndData() {
        configureToolbar();

        if (!isFirstCreation()) {

        }

        mFragmentManager = getSupportFragmentManager();
        MpditGotennaConnectionFragment fragment = (MpditGotennaConnectionFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_GOTENNA_CONNECTION);
        if (fragment == null) {
            fragment = MpditGotennaConnectionFragment.newInstance();
        }

        if (fragment != null) {

            try {
                mFragmentManager.beginTransaction()
                        .replace(R.id.vector_settings_page, fragment, TAG_FRAGMENT_GOTENNA_CONNECTION)
                        .addToBackStack(TAG_FRAGMENT_GOTENNA_CONNECTION)
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

