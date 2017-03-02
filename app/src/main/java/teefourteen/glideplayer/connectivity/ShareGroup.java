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
import java.lang.ref.WeakReference;
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
    private static final int ACTION_GET_ALBUM_ART = 1004;
    private static final int ACTION_NOTIFY_USERNAME_TAKEN = 1005;
    private static int sessionId = 0;
    private boolean isOwner = false;
    public static WeakReference<ShareGroup> shareGroupWeakReference;
    private NetworkService.ServiceBinder netService;
    public static String userName;
    private static GlidePlayerGroup currentGroup;
    private ArrayList<GlidePlayerGroup> foundGroups;
    private Activity activity;
    private ArrayAdapter groupListAdapter;
    private ArrayList<String> memberList;
    private ArrayList<GroupMemberListener> groupMemberListenerList = new ArrayList<>();
    private ArrayList<GroupConnectionListener> groupConnectionListenerList  = new ArrayList<>();
    private ShareGroupInitListener shareGroupInitListener;
    private EasyHandler handler;

    public class GlidePlayerGroup {
        public final String ownerId;
        public final Member owner;
        public final String groupName;
        private HashMap<String, Member> groupMembers;
        private int memberCount;

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof GlidePlayerGroup
                    && (this.ownerId.equals(((GlidePlayerGroup) obj).ownerId)));
        }

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

        public Member getMemberFromUsername(String username) {
            for(Member member : groupMembers.values()) {
                if(member.name.equals(username)) {
                    return member;
                }
            }
            return null;
        }

        public String getMemberId(String memberName) {
            String memberId = null;
            for(String id : currentGroup.groupMembers.keySet()) {
                Member member = currentGroup.groupMembers.get(id);
                if(member.name.equals(memberName)) {
                    memberId = id;
                    break;
                }
            }

            return memberId;
        }

        public boolean isOwner() {
            return owner.name.equals(userName);
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

    public interface GetAlbumArtListener {
        void onGotAlbumArt(File imageFile);
        void onFailedGettingAlbumArt();
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            netService = (NetworkService.ServiceBinder) service;
            shareGroupInitListener.onShareGroupReady();
            //TODO: listener notify ready
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Binding", "unbound");
        }
    };


    public static File getLibraryFile(String memberName) {
        Member member = currentGroup.getMemberFromUsername(memberName);
        if(member != null) {
            return new File(member.dbFile);
        } else {
            return null;
        }
    }

    public static void getSong(String memberName, long songId,
                                 final GetSongListener getSongListener) {
        String memberId = currentGroup.getMemberId(memberName);

        ShareGroup group = ShareGroup.shareGroupWeakReference.get();

        group.netService.sendRequest(memberId, ACTION_GET_SONG, songId,
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

    public static void getAlbumArt(String userName, long albumId,
                                   final GetAlbumArtListener albumArtListener) {
        String memberId = currentGroup.getMemberId(userName);

        ShareGroup group = ShareGroup.shareGroupWeakReference.get();

        group.netService.sendRequest(memberId, ACTION_GET_ALBUM_ART, albumId,
                new ResponseListener() {
                    @Override
                    public void onResponseReceived(Object responseData) {
                        albumArtListener.onGotAlbumArt((File) responseData);
                    }

                    @Override
                    public void onRequestFailed() {
                        albumArtListener.onFailedGettingAlbumArt();
                    }
                });
    }

    private File getAlbumArtRequest(Long albumId) {
        Library library = new Library(activity,
                new File(Library.DATABASE_LOCATION,Library.LOCAL_DATABASE_NAME));

        File albumArtFile = library.getAlbumArt(albumId);

        library.close();

        return albumArtFile;
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

    public ShareGroup(Activity activity, String userName, ShareGroupInitListener listener){
        shareGroupWeakReference = new WeakReference<ShareGroup>(this);
        sessionId++;

        this.activity = activity;
        ShareGroup.userName = userName;
        this.shareGroupInitListener = listener;

        memberList = new ArrayList<>();
        memberList.add(userName);

        handler = new EasyHandler();
        foundGroups = new ArrayList<GlidePlayerGroup>();
        currentGroup = null;
        startService();
    }

    private void startService() {
        Intent intent = new Intent(activity, NetworkService.class);


        activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean isConnected() {
        return (currentGroup != null);
    }

    public void createGroup(final String groupName, final GroupCreationListener groupCreationListener,
                            GroupMemberListener groupMemberListener) {
        isOwner = true;
        currentGroup = new GlidePlayerGroup(null,null,groupName, 1);
        netService.createGroup(userName, groupName, groupCreationListener, this, this);
        registerGroupMemberListener(groupMemberListener);
    }

    public void deleteGroup() {
        netService.deleteGroup();
    }

    public ArrayList<GlidePlayerGroup> getGroupList() {
        return foundGroups;
    }

    public void findGroups(ArrayAdapter groupListAdapter) {
        this.groupListAdapter = groupListAdapter;
        netService.discoverGroups(this, this);
    }

    public void stopFindingGroups() {
        netService.stopDiscovery();
        foundGroups.clear();
        groupListAdapter.clear();
    }

    public void connectToGroup(int groupListIndex, final GroupConnectionListener connectionListener,
                               final GroupMemberListener memberListener) {
        //TODO: check if anyone else in the group has the same username
        isOwner = false;
        String groupId = foundGroups.get(groupListIndex).ownerId;
        registerGroupConnectionListener(connectionListener);
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
                for(GroupConnectionListener listener : groupConnectionListenerList) {
                    listener.onExchangingInfo();
                }
            }
            @Override
            public void onExchangingInfo() {} //never called
            @Override
            public void onConnectionFailed(final String failureMessage) {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        for(GroupConnectionListener listener : groupConnectionListenerList) {
                            listener.onConnectionFailed(failureMessage);
                        }
                    }
                });
            }

            @Override
            public void onOwnerDisconnected() {
                if(currentGroup == null) return;
                memberList.clear();
                memberList.add(userName);
                currentGroup = null;
                stopFindingGroups();
                for(final GroupConnectionListener listener : groupConnectionListenerList) {
                    EasyHandler.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onOwnerDisconnected();
                        }
                    });
                }
            }
        };

        registerGroupMemberListener(memberListener);

        netService.connectToGroup(groupId, connectionListener2, this, this);
    }

    public ArrayList<String> getMemberList() {
        return memberList;
    }

    public void registerGroupMemberListener(GroupMemberListener listener) {
        if(!groupMemberListenerList.contains(listener)) {
            groupMemberListenerList.add(listener);
        }
    }

    public void unregisterGroupMemberListener(GroupMemberListener listener) {
        groupMemberListenerList.remove(listener);
    }

    public void registerGroupConnectionListener(GroupConnectionListener listener) {
        if(!groupConnectionListenerList.contains(listener)) {
            groupConnectionListenerList.add(listener);
        }
    }

    public void unregisterGroupConnectionListener(GroupConnectionListener listener) {
        groupConnectionListenerList.remove(listener);
    }

    public int getSessionId() {
        return sessionId;
    }

    @Override
    public void close() {
        activity.unbindService(serviceConnection);
        handler.closeAllHandlers();
        deleteFiles();
        shareGroupWeakReference = null;
    }

    private void deleteFiles() {
        File file = new File(Library.REMOTE_DATABASE_LOCATION);
        clearDir(file);

        file = new File(Library.FILE_SAVE_LOCATION);
        clearDir(file);

        file = new File(Library.REMOTE_COVERS_LOCATION);
        clearDir(file);
    }

    private void clearDir(File dir) {
        for(File file : dir.listFiles()) {
            if(file.isDirectory()) {
                clearDir(file);
                file.delete();
            } else {
                file.delete();
            }
        }
    }

    @Override
    public void newGroupFound(String Id, String groupName, String ownerName, String deviceName, int memberCount) {
        GlidePlayerGroup newGroup = new GlidePlayerGroup(
                Id,
                new Member(ownerName, deviceName, null),
                groupName,
                memberCount);

        if(!foundGroups.contains(newGroup)) {
            foundGroups.add(newGroup);
            groupListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMemberLeft(final String member) {
        if(currentGroup.groupMembers.get(member) == null) {
            return;
        }

        memberList.remove(currentGroup.groupMembers.get(member).name);
        for(final GroupMemberListener listener : groupMemberListenerList) {
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onMemberLeft(currentGroup.groupMembers.get(member).name);
                }
            });
        }

        //since above executeOnMainThread()'s could still be running and accessing currentGroup, removeMember() is also called on main thread
        EasyHandler.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                currentGroup.removeMember(member);
            }
        });
    }

    @Override //always called on a client-handling specific thread. Don't try to prevent blocking.
    public void onNewMemberJoined(final String memberId, String memberName) {

        final ResponseListener usernameResponse = new ResponseListener() {
            @Override
            public void onResponseReceived(Object responseData) {
                final String newMemberUsername = (String) responseData;

                //check if username taken
                if(userName.equals(newMemberUsername)
                        || currentGroup.getMemberFromUsername(newMemberUsername) != null) {
                    //check if this is group owner

                    if(isOwner) {
                        netService.sendRequest(memberId, ACTION_NOTIFY_USERNAME_TAKEN, null, null);
                    }
                } else {

                    currentGroup.addMember(memberId, newMemberUsername, null);

                    ResponseListener getLibrary = new ResponseListener() {
                        @Override
                        public void onResponseReceived(Object responseData) {
                            exchangeLibraries(memberId, (Socket) responseData, true);

                            updateLibraryLists(memberId);
                        }

                        @Override
                        public void onRequestFailed() {
                            currentGroup.removeMember(memberId);
                        }
                    };

                    //exchange libraries
                    netService.getRawSocket(memberId, getLibrary);
                }
            }
            @Override
            public void onRequestFailed() {}
        };

        netService.sendRequest(memberId, ACTION_GET_USERNAME, userName, usernameResponse);
    }

    private void updateLibraryLists(String memberId) {
        final Member member = currentGroup.groupMembers.get(memberId);

        memberList.add(member.name);

        for(final GroupMemberListener listener : groupMemberListenerList) {
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onNewMemberJoined(null, member.name);
                }
            });
        }
    }


    private void exchangeLibraries(final String memberId, Socket socket, boolean sendFirst) {
        final String newMemberUsername = currentGroup.groupMembers.get(memberId).name;

        File remoteLibraryFile = new File(Library.REMOTE_DATABASE_LOCATION, newMemberUsername);
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
                    for(GroupConnectionListener listener : groupConnectionListenerList) {
                        listener.onConnectionSuccess(currentGroup.groupName);
                    }
                }
            });
            return null;
        } else if(action == ACTION_GET_SONG) {
            return getSongRequest((Long)requestData);
        } else if(action == ACTION_NOTIFY_USERNAME_TAKEN) {
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    for(GroupConnectionListener listener : groupConnectionListenerList) {
                        listener.onConnectionFailed("Username already taken");
                    }
                }
            });
            deleteGroup();
            currentGroup.removeMember(deviceId);
            return null;
        } else if(action == ACTION_GET_ALBUM_ART) {
            return getAlbumArtRequest((Long) requestData);
        }

        else return null;
    }

    @Override
    public void error(String errorMsg) {
        Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
    }
}

