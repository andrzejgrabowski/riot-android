

package im.vector.fragments;



        import android.app.AlertDialog;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ImageView;
        import android.widget.TextView;
        import android.widget.Toast;

        import im.vector.MeshNode;
        import im.vector.MpditManager;
        import im.vector.R;
        import im.vector.VectorApp;
        import im.vector.util.PreferencesManager;
        import im.vector.util.VectorUtils;

        import android.app.Application;

        import androidx.collection.ArraySet;
        import androidx.preference.PreferenceManager;
        import androidx.recyclerview.widget.LinearLayoutManager;
        import androidx.recyclerview.widget.RecyclerView;

        import java.lang.reflect.Array;
        import java.util.Objects;
        import java.util.Set;
        import java.util.Vector;


public class MpditGotennaChatFragment extends VectorBaseFragment implements View.OnClickListener{



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
    public int getLayoutResId() {
        return R.layout.fragment_mpdit_gotenna_add_user;
    }

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static MpditGotennaChatFragment newInstance() {
        return new MpditGotennaChatFragment();
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
        b = getActivity().findViewById(R.id.buttonGoTennaAddUser);
        if(null != b)       b.setOnClickListener(this);

        MpditManager mpdit = getMpditManager();

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
        if (mpdit == null) return;

        switch (v.getId()) {
            case R.id.buttonGoTennaAddUser:

            break;
        }
    }

}

