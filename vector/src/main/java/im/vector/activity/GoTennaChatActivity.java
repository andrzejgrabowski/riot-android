package im.vector.activity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;

import im.vector.MpditManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.fragments.MpditGotennaAddUserFragment;
import im.vector.fragments.MpditGotennaChatFragment;

public class GoTennaChatActivity extends VectorAppCompatActivity {

    static public final String TAG_FRAGMENT_GOTENNA_CHAT = "TAG_FRAGMENT_GOTENNA_CHAT";
    private FragmentManager mFragmentManager;
    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gotenna_settings);
    }*/

    public MpditManager getMpditManager() {
        VectorApp app = VectorApp.getInstance();
        if (app != null) {
            MpditManager mpdit = app.getMpditManger();
            if (mpdit != null) {
                //mpdit.goTennaTechnicalMessageListener = this;
                return mpdit;
            }
        }
        return null;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_go_tenna_chat_acitivity;
    }

    @Override
    public int getTitleRes() {
        return R.string.gotenna_chat;
        /*        MpditManager mpdit = getMpditManager();
        if(null != mpdit)
            return mpdit.mGotennaChatUserName;

        return "rozmowa";*/
    }

    @Override
    public void initUiAndData() {
        configureToolbar();

        if (!isFirstCreation()) {

        }

        mFragmentManager = getSupportFragmentManager();
        MpditGotennaChatFragment fragment = (MpditGotennaChatFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_GOTENNA_CHAT);
        if (fragment == null) {
            fragment = MpditGotennaChatFragment.newInstance();
        }

        if (fragment != null) {

            try {
                mFragmentManager.beginTransaction()
                        .replace(R.id.vector_settings_page, fragment, TAG_FRAGMENT_GOTENNA_CHAT)
                        .addToBackStack(TAG_FRAGMENT_GOTENNA_CHAT)
                        .commit();
            } catch (Exception e) {
                //Log.e(LOG_TAG, "## updateSelectedFragment() failed : " + e.getMessage(), e);
            }
        }


    }

    @Override
    public void afterCreate() {
        // zmieniamy tytuł
        MpditManager mpdit = getMpditManager();
        if(null != mpdit) {
            ActionBar ab = getSupportActionBar();
            if(null != ab)
                ab.setTitle(mpdit.mGotennaChatUserName);
        }
    }

    @Override
    public int getMenuRes() {
        return R.menu.menu_country_picker;
    }
}