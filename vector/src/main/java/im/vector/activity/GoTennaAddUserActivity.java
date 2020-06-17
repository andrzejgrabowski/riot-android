
package im.vector.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import im.vector.Matrix;
import im.vector.R;
import im.vector.fragments.MpditGotennaAddUserFragment;
import im.vector.fragments.MpditGotennaConnectionFragment;

public class GoTennaAddUserActivity extends VectorAppCompatActivity {

    static public final String TAG_FRAGMENT_GOTENNA_ADD_USER = "FRAGMENT_GOTENNA_ADD_USER";
    private FragmentManager mFragmentManager;
    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gotenna_settings);
    }*/

    @Override
    public int getLayoutRes() {
        return R.layout.activity_go_tenna_add_user;
    }

    @Override
    public int getTitleRes() {
        return R.string.add_gotenna_user;
    }

    @Override
    public void initUiAndData() {
        configureToolbar();

        if (!isFirstCreation()) {

        }

        mFragmentManager = getSupportFragmentManager();
        MpditGotennaAddUserFragment fragment = (MpditGotennaAddUserFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_GOTENNA_ADD_USER);
        if (fragment == null) {
            fragment = MpditGotennaAddUserFragment.newInstance();
        }

        if (fragment != null) {

            try {
                mFragmentManager.beginTransaction()
                        .replace(R.id.vector_settings_page, fragment, TAG_FRAGMENT_GOTENNA_ADD_USER)
                        .addToBackStack(TAG_FRAGMENT_GOTENNA_ADD_USER)
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