package com.teefourteen.glideplayer.connectivity.group;

import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;
import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.Network;
import com.teefourteen.glideplayer.connectivity.network.NetworkListener;
import com.teefourteen.glideplayer.connectivity.network.ResponseListener;
import com.teefourteen.glideplayer.connectivity.network.server.Request;
import com.teefourteen.glideplayer.connectivity.network.server.Response;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static com.teefourteen.glideplayer.connectivity.group.Group.GroupState.CONNECTED;
import static com.teefourteen.glideplayer.connectivity.group.Group.GroupState.CONNECTING;
import static com.teefourteen.glideplayer.connectivity.group.Group.GroupState.CREATING;
import static com.teefourteen.glideplayer.connectivity.group.Group.GroupState.DISCONNECTED;

public class Group extends StateListener<GroupListener, Group.GroupState> {
    private static final String LOG_TAG = "Group";
    private static final String REQUEST_ACQUAINTANCE = "/group/username";

    private Member owner;
    private Member me;
    private Network network;
    private ArrayList<Member> members;
    private String userName;

    public final String groupName;

    public enum GroupState {
        CREATING,
        CONNECTING,
        CONNECTED,
        JOINING,
        JOINED,
        DISCONNECTED
    }

    public GroupState getState() {
        return state;
    }




    private Group(String groupName, GroupListener groupListener) {
        super(null, DISCONNECTED);
        this.groupName = groupName;
    }



    synchronized private void addMember(Member member) {
        members.add(member);
        if(member != me) {
            for(GroupListener listener : listeners) {
                EasyHandler.executeOnMainThread(() -> listener.onMemberConnect(member));
            }
        }
    }

    synchronized private void removeMember(String id) {
        Member member = getMember(id);
        if(member != null) {
            members.remove(member);
        }

        for(GroupListener listener : listeners) {
            EasyHandler.executeOnMainThread(() -> listener.onMemberLeave(member));
        }
    }

    private Member getMember(String name) {
        for(Member member : members) {
            if(member.userName.equals(name)) {
                return member;
            }
        }

        return null;
    }

    public Member getMemberById(String id) {
        for(Member member : members) {
            if(member.memberId.equals(id)) {
                return member;
            }
        }

        return null;
    }

    public Member[] getMembers() {
        return members.toArray(new Member[members.size()]);
    }




    public void create(String userName) {
        this.userName = userName;
    }

    public void connect(String userName) {
        this.userName = userName;
    }

    public void disconnect() {

    }



    private void getAcquainted() {
        for(Client client : network.getClients()) {
            getAcquainted(client);
        }
    }

    private void getAcquainted(Client client) {
        HashMap<String, String> request = new HashMap<>(1);
        request.put("userName", userName);

        ResponseListener<JSONObject> usernameResponseListener = new ResponseListener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String username = response.getString("username");

                    Member member = new Member(username, client.clientId, network);
                    exchangeLibraries(member);
                } catch (Exception e) {
                    this.onError(Response.makeErrorJSON(e.toString()));
                }
            }

            @Override
            public void onError(JSONObject error) {
                groupConnectFailure(error.toString());
                updateState(DISCONNECTED);
            }
        };

        network.requestJSON(client.clientId, REQUEST_ACQUAINTANCE, request, usernameResponseListener);
    }

    private void exchangeLibraries(Member member) {
        //TODO lots of stuff here

        addMember(member);
        if(members.size() == network.getClients().length) {
            updateState(CONNECTED);
        }
    }



    private Response handleRequest(Request request) {
        if(request.requestType.equals(REQUEST_ACQUAINTANCE)) {
            return handleAcquaintanceRequest(request);
        } else {
            return new Response("Unknown request");
        }
    }


    private Response handleAcquaintanceRequest(Request request) {
        JSONObject response = new JSONObject();

        try {
            response.put("username", userName);
            return new Response(response);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while getting acquainted", e);
            return new Response(e.toString());
        }
    }



    private void groupConnectFailure(String reason) {
        for(GroupListener listener : listeners) {
            EasyHandler.executeOnMainThread(() -> listener.onConnectFailure(reason));
        }
    }

    private void groupCreateFailure(String reason) {
        for(GroupListener listener : listeners) {
            EasyHandler.executeOnMainThread(() -> listener.onCreateFailure(reason));
        }
    }


    private NetworkListener networkListener = new NetworkListener() {
        @Override
        public void onCreating() {
            updateState(CREATING);
        }

        @Override
        public void onCreate() {
            updateState(CONNECTED);
            addMe();
        }

        @Override
        public void onCreateFailed(String reason) {
            groupCreateFailure(reason);
            updateState(DISCONNECTED);
        }

        @Override
        public void onConnecting() {
            updateState(CONNECTING);
        }

        @Override
        public void onConnect() {
            addOwner();
            addMe();
            getAcquainted();
        }

        @Override
        public void onConnectFailed(String reason) {
            groupConnectFailure(reason);
            updateState(DISCONNECTED);
        }

        @Override
        public void onClientConnect(String clientId) {

        }

        @Override
        public void onClientDisconnect(String clientId) {
            removeMember(clientId);
        }

        @Override
        public void onDisconnect() {
            updateState(DISCONNECTED);
        }

        @Override
        public Response onRequestReceived(Request request) {
            return handleRequest(request);
        }
    };

    private void addOwner() {
        Member owner = new Member(network.ownerName, network.getOwner().clientId, network);
        addMember(owner);
    }

    private void addMe() {
        Member me = new Member(userName, network.getMe().clientId, network);
        this.me = me;
        addMember(me);
    }

    @Override
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
}