package im.vector.fragments;



import android.content.DialogInterface;
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
import im.vector.adapters.HomeGotennaAdapter;
import im.vector.util.PreferencesManager;
import im.vector.util.VectorUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Vector;


public class MpditGotennaAddUserFragment extends VectorBaseFragment implements View.OnClickListener, HomeGotennaAdapter.OnSelectGotennaListener {

    private RecyclerView mRecyclerView = null;
    private RecyclerView.Adapter mAdapter = null;
    private RecyclerView.LayoutManager mLayoutManager = null;



    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private Vector<MeshNode> mDataset;
        public HomeGotennaAdapter.OnSelectGotennaListener mListener = null;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public View mView;
            public MyViewHolder(View v) {
                super(v);
                mView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(Vector<MeshNode> dataset, HomeGotennaAdapter.OnSelectGotennaListener listener) {
            mDataset = dataset;
            mListener = listener;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {

            final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final View itemView = layoutInflater.inflate(R.layout.adapter_item_gotenna_user, parent, false);

            // create a new view
            //adapter_item_contact_view
            //adapter_item_gotenna_user
            // item_locale
            // item_country

            return new MyViewHolder(itemView);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            TextView tv = holder.mView.findViewById(R.id.gotenna_contact_name);
            TextView tvId = holder.mView.findViewById(R.id.gotenna_contact_gid);
            if(position < mDataset.size() && position >= 0) {
                if(tv!=null)    tv.setText(mDataset.get(position).name);
                if(tvId!=null)    tvId.setText(mDataset.get(position).ID);
                ImageView iv = holder.mView.findViewById(R.id.adapter_item_gotenna_contact_avatar);
                VectorUtils.setDefaultMemberAvatar(iv, mDataset.get(position).ID,mDataset.get(position).name);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView tvName = holder.mView.findViewById(R.id.gotenna_contact_name);
                    TextView tvId = holder.mView.findViewById(R.id.gotenna_contact_gid);
                    String n = (String) tvName.getText();
                    String id = (String) tvId.getText();
                    //Toast.makeText(mContext, n, Toast.LENGTH_SHORT).show();

                    if(null != mListener)
                        mListener.onSelectGotenna(id,n);
                    //mListener.onSelectRoom(room, viewHolder.getAdapterPosition());
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    TextView tvName = holder.mView.findViewById(R.id.gotenna_contact_name);
                    TextView tvId = holder.mView.findViewById(R.id.gotenna_contact_gid);
                    String n = (String) tvName.getText();
                    String id = (String) tvId.getText();
                    //Toast.makeText(mContext, n, Toast.LENGTH_SHORT).show();

                    if(null != mListener)
                        mListener.onLongClickGotenna(n,id);
                    //mListener.onLongClickRoom(v, room, viewHolder.getAdapterPosition());
                    return true;
                }
            });

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }



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

    public static MpditGotennaAddUserFragment newInstance() {
        return new MpditGotennaAddUserFragment();
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


        mRecyclerView = (RecyclerView) getActivity().findViewById(R.id.gotenna_users_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(mpdit.getGotennaNodes(),this);


        mRecyclerView.setAdapter(mAdapter);


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

    private void UpdateGotennaNamesInSharedPreferences()
    {
        MpditManager mpdit = getMpditManager();
        if (mpdit == null) return;

        // tworzymy zestawy
        ArraySet<String> n = new ArraySet<String>();
        ArraySet<String> g = new ArraySet<String>();
        Vector<MeshNode> nodes = mpdit.getGotennaNodes();
        for(int i=0; i<nodes.size(); i++) {
            MeshNode node = nodes.get(i);
            n.add(node.name);
            g.add(node.ID);
        }


        // zapisujemy do SharedPrefernces
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor e =sp.edit();
        e.putStringSet(PreferencesManager.GOTENNA_NAMES,n);
        e.putStringSet(PreferencesManager.GOTENNA_GIDS,g);
        e.apply();
    }

    @Override
    public void onClick(View v) {

        MpditManager mpdit = getMpditManager();
        if (mpdit == null) return;

        switch (v.getId()) {
            case R.id.buttonGoTennaAddUser:
            {
                // odczytujemy wartości i dodajemy
                EditText edGid = getActivity().findViewById(R.id.editTextGID);
                EditText edName = getActivity().findViewById(R.id.editTextGotennaUserName);
                if(null != edGid && null != edName) {
                    String name = edName.getText().toString();
                    String id = edGid.getText().toString();
                    if(id.length() > 0 && name.length() > 0) {
                        mpdit.goTennaAddUpdateNode(id, name);
                        UpdateGotennaNamesInSharedPreferences();


                        edGid.setText("");
                        edName.setText("");

                        Toast.makeText(getContext(), "Użytkownik dodany", Toast.LENGTH_SHORT).show();
                        //Toast.makeText(getContext(), n.toString(), Toast.LENGTH_SHORT).show();

                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.scrollToPosition(mAdapter.getItemCount()-1);
                    }
                }
            }
                break;
        }
    }



    @Override
    public void onSelectGotenna(String id, String name) {
        // tworozmy alert box i usuwamy użytkownika
        // crash reported by a rage shake
        try {
            new AlertDialog.Builder(getContext())
                    .setMessage("Czy napewno chcesz usunąć użytkownika " + name + " GID: " + id)
                    .setCancelable(false)
                    .setPositiveButton("Usuń", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MpditManager mpdit = getMpditManager();
                            if (mpdit == null) return;

                            if(mpdit.goTennaRemoveNode(id)) {
                                UpdateGotennaNamesInSharedPreferences();
                                mAdapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Użytkownik usunięty", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //BugReporter.deleteCrashFile(VectorHomeActivity.this);
                        }
                    })
                    .show();
        } catch (Exception e) {
            //Log.e(LOG_TAG, "## onResume() : appCrashedAlert failed " + e.getMessage(), e);
        }
    }
/*

   */

    @Override
    public void onLongClickGotenna(String id, String name) {

    }
}
