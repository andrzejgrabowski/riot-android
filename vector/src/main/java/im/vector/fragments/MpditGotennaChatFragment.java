

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


    private RecyclerView mRecyclerView = null;
    private RecyclerView.Adapter mAdapter = null;
    private RecyclerView.LayoutManager mLayoutManager = null;
    private String mGID = "?";
    private String mUsername = "?";



    public class GotennaChatAdapter extends RecyclerView.Adapter<GotennaChatAdapter.GotennaMessageViewHolder> {
        private Vector<MeshNode.GotennaMessage> mDataset;
        private static final int CHAT_MINE_TYPE = 0;
        private static final int CHAT_OTHER_TYPE = 1;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class GotennaMessageViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public View mView;

            public GotennaMessageViewHolder(View v) {
                super(v);
                mView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public GotennaChatAdapter(Vector<MeshNode.GotennaMessage> dataset) {
            mDataset = dataset;
        }

        @Override
        public int getItemViewType(int position)
        {
            if(null != mDataset) {
                if(position < mDataset.size()) {
                    if(!mDataset.get(position).fromHost)
                        return CHAT_OTHER_TYPE;
                }
            }
            return CHAT_MINE_TYPE;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public GotennaChatAdapter.GotennaMessageViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {

            final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            int res = R.layout.adapter_item_gotenna_chat_message;
            if(viewType != CHAT_MINE_TYPE)
                res = R.layout.adapter_item_gotenna_chat_message_received;
            final View itemView = layoutInflater.inflate(res, parent, false);



            return new GotennaMessageViewHolder(itemView);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(GotennaMessageViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            TextView m = holder.mView.findViewById(R.id.cellMessageTextView);
            TextView time = holder.mView.findViewById(R.id.cellInfoTextView);
            ImageView iv = holder.mView.findViewById(R.id.messageStatusImageView);
            //TextView tvId = holder.mView.findViewById(R.id.gotenna_contact_gid);
            if(position < mDataset.size() && position >= 0) {
                if(m!=null) {
                    m.setText(mDataset.get(position).text);
                    m.setVisibility(View.VISIBLE);
                }
                if(time!=null) {
                    time.setText(mDataset.get(position).time);
                    time.setVisibility(View.VISIBLE);
                }
                if(iv!=null) {

                    time.setVisibility(View.VISIBLE);
                }
                //if(tvId!=null)    tvId.setText(mDataset.get(position).ID);
                //ImageView iv = holder.mView.findViewById(R.id.adapter_item_gotenna_contact_avatar);
                //VectorUtils.setDefaultMemberAvatar(iv, mDataset.get(position).ID,mDataset.get(position).name);
            }

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
        return R.layout.fragment_mpdit_gotenna_chat;
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
        b = getActivity().findViewById(R.id.gotennaMessageSendButton);
        if(null != b)       b.setOnClickListener(this);

        MpditManager mpdit = getMpditManager();

        mGID = mpdit.mGotennaChatUserGID;
        mUsername = mpdit.mGotennaChatUserName;


        mRecyclerView = (RecyclerView) getActivity().findViewById(R.id.gotenna_chat_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new GotennaChatAdapter(mpdit.getGotennaMessages(mGID));

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

    @Override
    public void onClick(View v) {

        MpditManager mpdit = getMpditManager();
        if (mpdit == null) return;

        switch (v.getId()) {
            case R.id.gotennaMessageSendButton: {
                // wysyłamy wiadomość
                EditText et = getActivity().findViewById(R.id.gotennaSendMessageEditText);
                if (null != et) {
                    if(mpdit.isGotennaConected()) {
                        String messageText = et.getText().toString();
                        if(messageText.length() < mpdit.GOTENNA_MESSAGE_BYTE_LIMIT) {
                            mpdit.GotennaSendTextMessage(mGID, messageText);
                            Toast.makeText(getActivity(), messageText, Toast.LENGTH_SHORT).show();
                            et.setText("");
                            // refresh recycler
                            // TO DO !!!
                            //mAdapter.notifyItemInserted();
                            mAdapter.notifyDataSetChanged();
                            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                        } else { Toast.makeText(getActivity(), "Wiadomość jest za długa", Toast.LENGTH_SHORT).show(); }
                    } else { Toast.makeText(getActivity(), "Brak połączenia z goTenną", Toast.LENGTH_SHORT).show(); }

                } else { Toast.makeText(getActivity(), "EditText = null", Toast.LENGTH_SHORT).show(); }

            }
            break;
        }
    }

}

