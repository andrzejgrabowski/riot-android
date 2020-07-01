

package im.vector.fragments;



        import android.content.Context;
        import android.graphics.drawable.AnimationDrawable;
        import android.os.Bundle;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ImageView;
        import android.widget.TextView;
        import android.widget.Toast;

        import im.vector.GoTennaMessage;
        import im.vector.MeshNode;
        import im.vector.MpditManager;
        import im.vector.R;
        import im.vector.VectorApp;

        import androidx.recyclerview.widget.LinearLayoutManager;
        import androidx.recyclerview.widget.RecyclerView;

        import java.util.Vector;


public class MpditGotennaChatFragment extends VectorBaseFragment implements View.OnClickListener, MpditManager.GoTennaMessageListener {


    private RecyclerView mRecyclerView = null;
    private RecyclerView.Adapter mAdapter = null;
    private RecyclerView.LayoutManager mLayoutManager = null;
    private String mGID = "?";
    private String mUsername = "?";

    @Override
    public void onMessageResponseReceived() {
        if(null != mRecyclerView && null != mAdapter) {
            mAdapter.notifyDataSetChanged();
            mRecyclerView.scrollToPosition(mAdapter.getItemCount()-1);
        }
    }

    @Override
    public void onIncomingMessage(String sender, String text) {
        if(null != mRecyclerView && null != mAdapter) {
            mAdapter.notifyDataSetChanged();
            mRecyclerView.scrollToPosition(mAdapter.getItemCount()-1);

            Toast.makeText(getActivity(), sender + ": " + text, Toast.LENGTH_SHORT).show();
        }
    }


    public class GotennaChatAdapter extends RecyclerView.Adapter<GotennaChatAdapter.GotennaMessageViewHolder> {
        private Vector<GoTennaMessage> mDataset;
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
        public GotennaChatAdapter(Vector<GoTennaMessage> dataset) {
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
            if (mDataset == null) return;

            TextView m = holder.mView.findViewById(R.id.cellMessageTextView);
            TextView time = holder.mView.findViewById(R.id.cellInfoTextView);
            ImageView iv = holder.mView.findViewById(R.id.messageStatusImageView);
            //TextView tvId = holder.mView.findViewById(R.id.gotenna_contact_gid);
            if(position < mDataset.size() && position >= 0) {
                GoTennaMessage gm = mDataset.get(position);
                if(m!=null) {
                    m.setText(gm.text_only);//text.substring(3));
                    m.setVisibility(View.VISIBLE);
                }
                if(time!=null) {
                    String t = gm.time;
                    if(gm.milisReceived > gm.milisSend)
                        t += String.format(" ping: %d ms", gm.milisReceived - gm.milisSend);
                    time.setText(t);
                    time.setVisibility(View.VISIBLE);
                }
                if(iv!=null) {

                    //time.setVisibility(View.VISIBLE);

                    if (gm.fromHost)
                    {
                        switch (gm.getMessageStatus())
                        {
                            case SENDING:
                                iv.setImageResource(R.drawable.sending_animation);
                                AnimationDrawable animationDrawable = (AnimationDrawable) iv.getDrawable();
                                animationDrawable.start();
                                break;
                            case SENT_SUCCESSFULLY:
                            {
                                if (gm.willDisplayAckConfirmation)
                                {
                                    iv.setImageResource(R.drawable.ic_success);
                                }
                                else
                                {
                                    iv.setImageResource(R.drawable.ic_clear_square);
                                }
                            }
                            break;
                            case ERROR_SENDING:
                                iv.setImageResource(R.drawable.ic_failed);
                                break;
                        }
                    }
                    else
                    {
                        iv.setVisibility(View.GONE);
                    }

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

        mpdit.goTennaMessageListener = this;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        MpditManager mpdit = getMpditManager();
        if(null != mpdit) mpdit.goTennaMessageListener = this;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MpditManager mpdit = getMpditManager();
        if(null != mpdit) mpdit.goTennaMessageListener = null;
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
                    if(mpdit.isGotennaConnected()) {
                        String messageText = et.getText().toString();
                        if(messageText.length() < mpdit.GOTENNA_MESSAGE_BYTE_LIMIT) {
                            mpdit.goTennaSendTextMessage(mGID, messageText);
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

