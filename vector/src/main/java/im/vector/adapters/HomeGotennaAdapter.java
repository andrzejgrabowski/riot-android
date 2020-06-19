//package im.vector.adapters;
//public class HomeGotennaAdapter { }

/*
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import org.matrix.androidsdk.data.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import im.vector.MeshNode;
import im.vector.MpditManager;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.adapters.model.NotificationCounter;
import im.vector.fragments.MpditGotennaAddUserFragment;
import im.vector.util.RoomUtils;
import im.vector.util.VectorUtils;

public class HomeGotennaAdapter extends RecyclerView.Adapter<HomeGotennaAdapter.GotennaViewHolder> {//AbsFilterableAdapter<RoomViewHolder> {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class GotennaViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mView;
        public GotennaViewHolder(View v) {
            super(v);
            mView = v;
        }
    }


    private final int mLayoutRes;
    private Vector<MeshNode> mGotennas = new Vector<MeshNode>();
    private Context mContext;
    public OnSelectGotennaListener mListener = null;


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
    /*
     * *********************************************************************************************
     * Constructor
     * *********************************************************************************************
     */




    public HomeGotennaAdapter(final Context context,
                              @LayoutRes final int layoutRes)                          {
        mContext = context;

        MpditManager mpdit = getMpditManager();
        if(null != mpdit)
            mGotennas = mpdit.getGotennaNodes();// + mpdit.getMpditNodes();

        mLayoutRes = layoutRes;
    }
    /*
     * *********************************************************************************************
     * RecyclerView.Adapter methods
     * *********************************************************************************************
     */

    @Override
    public HomeGotennaAdapter.GotennaViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        final View view = layoutInflater.inflate(mLayoutRes, viewGroup, false);
        return new HomeGotennaAdapter.GotennaViewHolder(view);//mLayoutRes == R.layout.adapter_item_room_invite ? new RoomInvitationViewHolder(view) : new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GotennaViewHolder viewHolder, int position) {

        //return mGotennas.isEmpty();//
        if (position < mGotennas.size()) {
            MeshNode node = mGotennas.get(position);


            TextView tvName = viewHolder.mView.findViewById(R.id.room_name);
            TextView tvId = viewHolder.mView.findViewById(R.id.room_name_server);
            {
                if(tvName!=null)    {
                    tvName.setVisibility(View.VISIBLE);
                    tvName.setText(node.name);
                    tvName.setTypeface(null, Typeface.BOLD);
                }
                if(tvId!=null)    {
                    tvId.setVisibility(View.VISIBLE);
                    tvId.setText(node.ID);
                    tvId.setTypeface(null, Typeface.ITALIC);
                }
                ImageView iv = viewHolder.mView.findViewById(R.id.room_avatar);
                if(tvId!=null) {
                    iv.setVisibility(View.VISIBLE);
                    VectorUtils.setDefaultMemberAvatar(iv, node.ID, node.name);
                }
            }

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView tvId = viewHolder.mView.findViewById(R.id.room_name_server);
                    TextView tvName = viewHolder.mView.findViewById(R.id.room_name);
                    String n = (String) tvName.getText();
                    String id = (String) tvId.getText();
                    Toast.makeText(mContext, n, Toast.LENGTH_SHORT).show();

                    if(null != mListener)
                        mListener.onSelectGotenna(id,n);
                    //mListener.onSelectRoom(room, viewHolder.getAdapterPosition());
                }
            });
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    TextView tvId = viewHolder.mView.findViewById(R.id.room_name_server);
                    String n = (String) tvName.getText();
                    String id = (String) tvId.getText();
                    Toast.makeText(mContext, id, Toast.LENGTH_SHORT).show();

                    if(null != mListener)
                        mListener.onLongClickGotenna(n,id);
                    //mListener.onLongClickRoom(v, room, viewHolder.getAdapterPosition());
                    return true;
                }
            });

            /*
            if (mLayoutRes == R.layout.adapter_item_room_invite) {
                final RoomInvitationViewHolder invitationViewHolder = (RoomInvitationViewHolder) viewHolder;
                //invitationViewHolder.populateViews(mContext, mSession, room, mRoomInvitationListener, mMoreActionListener);
                if(null != invitationViewHolder.vRoomName)
                    invitationViewHolder.vRoomName.setText(node.name);
                if(null != invitationViewHolder.vRoomNameServer)
                    invitationViewHolder.vRoomNameServer.setText(node.ID);
            } else {
                //viewHolder.populateViews(mContext, mSession, room, room.isDirect(), false, mMoreActionListener);
                if(null != viewHolder.vRoomName) {
                    viewHolder.vRoomName.setText(node.name);
                    viewHolder.vRoomName.setVisibility(View.VISIBLE);
                    viewHolder.vRoomName.setTypeface(null,Typeface.BOLD);
                }
                if(null != viewHolder.vRoomNameServer) {
                    viewHolder.vRoomNameServer.setText(node.ID);
                    viewHolder.vRoomNameServer.setVisibility(View.VISIBLE);
                    viewHolder.vRoomNameServer.setTypeface(null,Typeface.NORMAL);
                }
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //mListener.onSelectRoom(room, viewHolder.getAdapterPosition());
                    }
                });
                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //mListener.onLongClickRoom(v, room, viewHolder.getAdapterPosition());
                        return true;
                    }
                });
            }*/
        }
        /*
        // reported by a rage shake
        if (position < mFilteredRooms.size()) {
            final Room room = mFilteredRooms.get(position);
            if (mLayoutRes == R.layout.adapter_item_room_invite) {
                final RoomInvitationViewHolder invitationViewHolder = (RoomInvitationViewHolder) viewHolder;
                invitationViewHolder.populateViews(mContext, mSession, room, mRoomInvitationListener, mMoreActionListener);
            } else {
                viewHolder.populateViews(mContext, mSession, room, room.isDirect(), false, mMoreActionListener);
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onSelectRoom(room, viewHolder.getAdapterPosition());
                    }
                });
                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mListener.onLongClickRoom(v, room, viewHolder.getAdapterPosition());
                        return true;
                    }
                });
            }
        }

         */
    }

    @Override
    public int getItemCount() {
        return mGotennas.size();
    }



    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    /**
     * Feed the adapter with items
     *
     * @param rooms the new room list
     */
    @CallSuper
    public void setRooms(final List<Room> rooms) {
        /*
        if (rooms != null) {
            mRooms.clear();
            mRooms.addAll(rooms);
            filterRooms(mCurrentFilterPattern);
        }*/
        MpditManager mpdit = getMpditManager();
        if(null != mpdit)
            mGotennas = mpdit.getGotennaNodes();
        notifyDataSetChanged();
    }

    /**
     * Provides the item at the dedicated position
     *
     * @param position the position
     * @return the item
     */
    public Room getRoom(int position) {
        /*if (position < mRooms.size()) {
            return mRooms.get(position);
        }*/

        return null;
    }

    /**
     * Return whether the section (not filtered) is empty or not
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return mGotennas.isEmpty();//mRooms.isEmpty();
    }

    /**
     * Return whether the section (filtered) is empty or not
     *
     * @return true if empty
     */
    public boolean hasNoResult() {
        return mGotennas.isEmpty();//return mFilteredRooms.isEmpty();
    }

    /**
     * Return the sum of highlight and notifications for all the displayed rooms
     *
     * @return badge value
     */
    public NotificationCounter getBadgeCount() {
        NotificationCounter notificationCounter = new NotificationCounter();
/*
        for (Room room : mFilteredRooms) {
            notificationCounter.addHighlights(room.getHighlightCount());

            // sanity checks : reported by GA
            if (null != room.getDataHandler()
                    && (null != room.getDataHandler().getBingRulesManager())
                    && room.getDataHandler().getBingRulesManager().isRoomMentionOnly(room.getRoomId())) {
                notificationCounter.addNotifications(room.getHighlightCount());
            } else {
                notificationCounter.addNotifications(room.getNotificationCount());
            }
        }
*/
        return notificationCounter;
    }

    /*
     * *********************************************************************************************
     * Private methods
     * *********************************************************************************************
     */

    /**
     * Filter the room list according to the given pattern
     *
     * @param constraint
     */
    private void filterRooms(CharSequence constraint) {
        //mFilteredRooms.clear();
        //mFilteredRooms.addAll(RoomUtils.getFilteredRooms(mContext, mRooms, constraint));
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    public interface OnSelectGotennaListener {
        void onSelectGotenna(String id, String name);

        void onLongClickGotenna(String id, String name);
    }
}

