package teefourteen.glideplayer.connectivity;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.connectivity.listeners.ErrorListener;
import teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import teefourteen.glideplayer.connectivity.listeners.GroupCreationListener;
import teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import teefourteen.glideplayer.connectivity.listeners.NewGroupListener;
import teefourteen.glideplayer.connectivity.listeners.RequestListener;
import teefourteen.glideplayer.connectivity.listeners.ResponseListener;
import teefourteen.glideplayer.music.Song;
import teefourteen.glideplayer.music.database.Library;

public class ShareGroup implements NewGroupListener, ErrorListener, GroupMemberListener,
        RequestListener, Closeable {
    //TODO: change method parameters to be relevant to sharegroup (eg: memberId in place of deviceId)

    private static final int ACTION_GET_USERNAME = 1000;
    private static final int ACTION_GET_SONG = 1002;
    public static ShareGroup instance;  //TODO: find memory leak work around
    private NetworkService.LocalBinder netManager;
    private String userName;
    private static GlidePlayerGroup currentGroup;
    private ArrayList<GlidePlayerGroup> foundGroups;
    private Activity activity;
    private ArrayAdapter groupListAdapter;
    private ArrayList<String> memberList;
    private ArrayAdapter<String> memberListAdapter;
    private GroupConnectionListener groupConnectionListener;
    private GroupMemberListener groupMemberListener;
    private ShareGroupInitListener shareGroupInitListener;
    private EasyHandler handler;

    public class GlidePlayerGroup {
        public final String ownerId;
        public final Member owner;
        public final String groupName;
        private HashMap<String, Member> groupMembers;
        private int memberCount;

        GlidePlayerGroup(String ownerId, Member owner, String groupName, int memberCount) {
            this.ownerId = ownerId;
            this.owner = owner;
            this.groupName = groupName;
            this.memberCount = memberCount;
            groupMembers = new HashMap<>();
        }

        synchronized public int getMemberCount() {
            return memberCount;
        }

        void connected(){ memberCount++; }

        synchronized void addMember(String deviceId, String username, String dbFile) {
            groupMembers.put(deviceId, new Member(username, null, dbFile));
            memberCount++;
        }

        synchronized void removeMember(String deviceId) {
            groupMembers.remove(deviceId);
            memberCount--;
        }
    }



    public class Member{
        public  final String name;
        public  final String deviceName;
        public  String dbFile;

        Member(String name, String deviceName, String dbFile) {
            this.name = name;
            this.deviceName = deviceName;
            this.dbFile = dbFile;
        }
    }


    public interface ShareGroupInitListener {
        void onShareGroupReady();
    }

    public interface GetSongListener {
        void onGotSong(String songFilePath);
        void onFailedGettingSong();
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            netManager = (NetworkService.LocalBinder) service;
            shareGroupInitListener.onShareGroupReady();
            //TODO: listener notify ready
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Binding", "unbound");
        }
    };


    public static File getLibraryFile(String memberName) {
        //TODO: change key value of groupMembers to username itself (after implementing username conflict check)
        for(String memberId : currentGroup.groupMembers.keySet()){
            Member member = currentGroup.groupMembers.get(memberId);
            if(member.name.equals(memberName)) {
                return new File(member.dbFile);
            }
        }

        return null;
    }

    public static void getSong(String memberName, long songId,
                                 final GetSongListener getSongListener) {
        //TODO: update the SongTable's filepath column to avoid having to re download each time
        String memberId = null;
        for(String id : currentGroup.groupMembers.keySet()) {
            Member member = currentGroup.groupMembers.get(id);
            if(member.name.equals(memberName)) {
                memberId = id;
                break;
            }
        }

        instance.netManager.sendRequest(memberId, ACTION_GET_SONG, songId,
                new ResponseListener() {
                    @Override
                    public void onResponseReceived(Object responseData) {
                        getSongListener.onGotSong(((File) responseData).getAbsolutePath());
                    }

                    @Override
                    public void onRequestFailed() {
                        getSongListener.onFailedGettingSong();
                    }
                });
    }

    private File getSongRequest(long songId) {
        Library lib = new Library(activity,
                new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME));

        Cursor cursor = Library.getSong(lib.getReadableDatabase(), songId);
        lib.close();
        if(cursor.moveToFirst()) {
            return new File(Song.toSong(cursor).getFilePath());
        } else {
            return null; //TODO: something less destructive. This will cause the receiver to throw an exception due to null file
        }
    }

    public ShareGroup(Activity activity, String userName, ShareGroupInitListener listener,
                      ArrayList<String> memberList, ArrayAdapter<String> memberListAdapter){
        instance = this;
        this.activity = activity;
        this.userName = userName;
        this.shareGroupInitListener = listener;

        this.memberList = memberList;

        this.memberListAdapter = memberListAdapter;
        memberListAdapter.notifyDataSetChanged();

        handler = new EasyHandler();
        foundGroups = new ArrayList<>();
        startService();
    }

    private void startService() {
        Intent intent = new Intent(activity, NetworkService.class);


        activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void createGroup(final String groupName, final GroupCreationListener groupCreationListener,
                            GroupMemberListener groupMemberListener) {
        currentGroup = new GlidePlayerGroup(null,null,groupName, 1);
        netManager.createGroup(userName, groupName, groupCreationListener, this, this);
        this.groupMemberListener = groupMemberListener;
    }

    public void deleteGroup() {
        netManager.deleteGroup();
    }

    public ArrayList<GlidePlayerGroup> getGroupList() {
        return foundGroups;
    }

    public void findGroups(ArrayAdapter groupListAdapter) {
        this.groupListAdapter = groupListAdapter;
        netManager.discoverGroups(this, this);
    }

    public void stopFindingGroups() {
        netManager.stopDiscovery();
        groupListAdapter = null;
        foundGroups = null;
    }

    public void connectToGroup(int groupListIndex, final GroupConnectionListener connectionListener,
                               final GroupMemberListener memberListener) {
        //TODO: check if anyone else in the group has the same username
        String groupId = foundGroups.get(groupListIndex).ownerId;
        this.groupConnectionListener = connectionListener;
        GroupConnectionListener connectionListener2 = new GroupConnectionListener() {
            @Override
            public void onConnectionSuccess(String connectedGroup) {
                for(GlidePlayerGroup group : foundGroups) {
                    if (group.ownerId.equals(connectedGroup)) {
                        currentGroup = group;
                        break;
                    }
                }
                stopFindingGroups();
                currentGroup.connected();
                groupConnectionListener.onExchangingInfo();
            }
            @Override
            public void onExchangingInfo() {} //never called
            @Override
            public void onConnectionFailed(String failureMessage) {
                connectionListener.onConnectionFailed(failureMessage);
            }
        };

        groupMemberListener = memberListener;

        netManager.connectToGroup(groupId, connectionListener2, this, this);
    }

    @Override
    public void close() {
        activity.unbindService(serviceConnection);
        handler.closeAllHandlers();
        instance = null;
    }

    @Override
    public void newGroupFound(String Id, String groupName, String ownerName, String deviceName, int memberCount) {
        foundGroups.add(new GlidePlayerGroup(Id,new Member(ownerName, deviceName, null),groupName,memberCount));
        groupListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMemberLeft(String member) {

    }

    @Override //always called on a client-handling specific thread. Don't try to prevent blocking.
    public void onNewMemberJoined(final String memberId, String memberName) {

        final ResponseListener usernameResponse = new ResponseListener() {
            @Override
            public void onResponseReceived(Object responseData) {
                final String newMemberUsername = (String) responseData;
                currentGroup.addMember(memberId, newMemberUsername, null);

                ResponseListener getLibrary = new ResponseListener() {
                    @Override
                    public void onResponseReceived(Object responseData) {
                        exchangeLibraries(memberId, (Socket) responseData, true);
                        groupMemberListener.onNewMemberJoined(memberId,
                                newMemberUsername);

                        updateLibraryLists(memberId);
                    }

                    @Override
                    public void onRequestFailed() {
                        currentGroup.removeMember(memberId);
                    }
                };

                //exchange libraries
                netManager.getRawSocket(memberId, getLibrary);
            }
            @Override
            public void onRequestFailed() {}
        };

        netManager.sendRequest(memberId, ACTION_GET_USERNAME, userName, usernameResponse);
    }

    private void updateLibraryLists(String memberId) {
        Member member = currentGroup.groupMembers.get(memberId);

        memberList.add(member.name);
        memberListAdapter.notifyDataSetChanged();
    }


    private void exchangeLibraries(final String memberId, Socket socket, boolean sendFirst) {
        final String newMemberUsername = currentGroup.groupMembers.get(memberId).name;

        File remoteLibraryFile = new File(Library.DATABASE_LOCATION, newMemberUsername);
        if (remoteLibraryFile.exists()) {
            remoteLibraryFile.delete();
        }
        Library library = new Library(activity,
                new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME));
        Library remoteLibrary = new Library(activity, remoteLibraryFile);

        try {
            if(sendFirst) {
                library.sendOverStream(socket.getInputStream(), socket.getOutputStream());
                remoteLibrary.getFromStream(socket.getInputStream(), socket.getOutputStream());
            } else {
                remoteLibrary.getFromStream(socket.getInputStream(), socket.getOutputStream());
                library.sendOverStream(socket.getInputStream(), socket.getOutputStream());
            }

            socket.close();

            currentGroup.addMember(memberId, newMemberUsername,
                    Library.DATABASE_LOCATION + "/" + newMemberUsername);

            library.close();
            remoteLibrary.close();

            currentGroup.groupMembers.get(memberId).dbFile = remoteLibraryFile.getAbsolutePath();

        } catch (IOException | JSONException e) {
            Log.d("exchange libs", e.toString());
        }
    }

    @Override //always on a separate thread
    public Object onNewRequest(String deviceId, int action, Object requestData) {
        if(action == ACTION_GET_USERNAME) {
            currentGroup.addMember(deviceId, (String) requestData, null);
            return userName;
        } else if(action == NetworkService.ACTION_RAW_SOCKET) {
            exchangeLibraries(deviceId, (Socket) requestData, false);
            updateLibraryLists(deviceId);
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    groupConnectionListener.onConnectionSuccess(currentGroup.groupName);
                }
            });
            return null;
        } else if(action == ACTION_GET_SONG) {
            return getSongRequest((Long)requestData);
        }

        else return null;
    }

    @Override
    public void error(String errorMsg) {
        Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
    }
}

