package com.teefourteen.glideplayer.connectivity.group;

import android.content.Context;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;
import com.teefourteen.glideplayer.connectivity.network.Network;

import java.util.ArrayList;

import static com.teefourteen.glideplayer.connectivity.group.Group.GroupState.DISCONNECTED;

public class Group extends StateListener<GroupListener, Group.GroupState> {
    public final String groupName;
    public final Member owner;

    public enum GroupState {
        CREATING,
        CONNECTING,
        CONNECTED,
        JOINING,
        JOINED,
        DISCONNECTED
    }

    final private Network mNetwork;
    private ArrayList<Member> mMembers;


    private Group(String groupName, Member owner, ArrayList<Member> members, Network network) {
        super(null, DISCONNECTED);

        this.groupName = groupName;
        this.owner = owner;
        this.mMembers = members;
        this.mNetwork = network;
    }

    Member[] members() {
        return mMembers.toArray(new Member[mMembers.size()]);
    }

    public GroupState getState() {
        return state;
    }

    protected void notifyListener(final GroupListener listener, final GroupState state) {
        EasyHandler.executeOnMainThread(() -> {
            switch (state) {

                case CREATING:
                    listener.onCreating();
                    break;
                case CONNECTING:
                    listener.onConnecting();
                    break;
                case CONNECTED:
                    listener.onConnect();
                    break;
                case JOINING:
                    listener.onJoining();
                    break;
                case JOINED:
                    listener.onJoin();
                    break;
                case DISCONNECTED:
                    listener.onDisconnect();
                    break;
            }
        });
    }

    void join() {
        mNetwork.connect();
    }

    void leave() {}
}