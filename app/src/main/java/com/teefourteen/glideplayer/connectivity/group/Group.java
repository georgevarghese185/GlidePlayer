package com.teefourteen.glideplayer.connectivity.group;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;
import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.Network;
import com.teefourteen.glideplayer.connectivity.network.NetworkListener;
import com.teefourteen.glideplayer.connectivity.network.ResponseListener;
import com.teefourteen.glideplayer.connectivity.network.server.Request;
import com.teefourteen.glideplayer.connectivity.network.server.Response;
import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.database.Table;

import org.json.JSONArray;
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
    private static final String REQUEST_TABLE = "/library/table";
    private static final String REQUEST_TABLE_META = "/library/table/meta";
    private static final int ROW_BATCH = 100;

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




    private Group(String groupName, GroupListener groupListener, Network network) {
        super(null, DISCONNECTED);
        this.groupName = groupName;
        this.network = network;

        listeners = new ArrayList<>();
        listeners.add(groupListener);
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
        network.create();
    }

    public void connect(String userName) {
        this.userName = userName;
        network.connect();
    }

    public void disconnect() {
        network.disconnect();
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
            }
        };

        network.requestJSON(client.clientId, REQUEST_ACQUAINTANCE, request, usernameResponseListener);
    }

    private void exchangeLibraries(Member member) {

        Table[] tables = Library.getTables();
        SQLiteDatabase libraryDb = Library.getDb();
        libraryDb.beginTransaction();

        try {
            for (Table table : tables) {
                String requestType = REQUEST_TABLE_META;
                HashMap<String, String> params = new HashMap<>();
                params.put("tableName", table.TABLE_NAME);

                Response metaResponse = network.requestJSON(member.memberId, requestType, params);

                assert metaResponse.jsonResponse != null;
                int rowCount = metaResponse.jsonResponse.getInt("rowCount");

                for(int i = 0; i < rowCount; i += ROW_BATCH) {
                    requestType = REQUEST_TABLE;
                    params = new HashMap<>();
                    params.put("tableName", table.TABLE_NAME);
                    params.put("row_start", String.valueOf(i));
                    int rowEnd = ((i + ROW_BATCH) > rowCount) ? rowCount : i + ROW_BATCH;
                    params.put("row_end", String.valueOf(rowEnd));

                    Response rowsResponse = network.requestJSON(member.memberId, requestType, params);
                    assert rowsResponse.jsonResponse != null;
                    JSONArray rows = rowsResponse.jsonResponse.getJSONArray("rows");

                    Library.addRemoteRows(rows, table, libraryDb, member.userName);
                }
            }

            libraryDb.setTransactionSuccessful();

            libraryDb.endTransaction();

            addMember(member);
            if(members.size() == network.getClients().length) {
                updateState(CONNECTED);
            }
        } catch (Exception e) {
            libraryDb.endTransaction();
            groupConnectFailure("Failed to exchange libraries");
            Log.e(LOG_TAG, "Exception while exchanging libraries", e);
        }
    }


    private Response handleTableMetaRequest(Request request) {
        try {
            String tableName = request.requestParams.get("tableName");
            JSONObject tableMeta = Library.getTableMeta(tableName);

            return new Response(tableMeta);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while getting table meta", e);
            return new Response(e.toString());
        }
    }


    private Response handleTableRequest(Request request) {
        try {
            String tableName = request.requestParams.get("tableName");
            int start = Integer.parseInt(request.requestParams.get("row_start"));
            int end = Integer.parseInt(request.requestParams.get("row_end"));

            JSONArray rows = Library.getTableRows(tableName, start, end);

            JSONObject response = new JSONObject();
            response.put("rows", rows);

            return new Response(response);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while handling table request", e);
            return new Response(e.toString());
        }
    }



    private Response handleRequest(Request request) {
        if(request.requestType.equals(REQUEST_ACQUAINTANCE)) {
            return handleAcquaintanceRequest(request);
        } else if(request.requestType.equals(REQUEST_TABLE_META)) {
            return handleTableMetaRequest(request);
        } else if(request.requestType.equals(REQUEST_TABLE)) {
            return handleTableRequest(request);
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
        disconnect();
    }

    private void groupCreateFailure(String reason) {
        for(GroupListener listener : listeners) {
            EasyHandler.executeOnMainThread(() -> listener.onCreateFailure(reason));
        }
        disconnect();
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