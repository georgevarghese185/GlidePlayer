package com.teefourteen.glideplayer.connectivity;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import android.util.LongSparseArray;
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

import com.teefourteen.glideplayer.AppNotification;
import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.connectivity.listeners.ErrorListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupConnectionListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupCreationListener;
import com.teefourteen.glideplayer.connectivity.listeners.GroupMemberListener;
import com.teefourteen.glideplayer.connectivity.listeners.NewGroupListener;
import com.teefourteen.glideplayer.connectivity.listeners.RequestListener;
import com.teefourteen.glideplayer.connectivity.listeners.ResponseListener;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.music.database.Library;
import com.teefourteen.glideplayer.services.PlayerService;

public class ShareGroup implements NewGroupListener, ErrorListener, GroupMemberListener,
        RequestListener, Closeable {
    class HOOMAN{

    }
    //TODO: change method parameters to be relevant to sharegroup (eg: memberId in place of deviceId)

    private static final int ACTION_GET_USERNAME = 1000;
    private static final int ACTION_GET_SONG = 1002;
    private static final int ACTION_GET_ALBUM_ART = 1004;
    private static final int ACTION_NOTIFY_USERNAME_TAKEN = 1005;
    private boolean isOwner = false;
    private Mode mode;
    public static WeakReference<ShareGroup> shareGroupWeakReference;
    private NetworkService.ServiceBinder netService;
    public static String userName;
    private static GlidePlayerGroup currentGroup;
    private String lastJoinedGroupName;
    private ArrayList<GlidePlayerGroup> foundGroups;
    private Context context;
    private ArrayAdapter groupListAdapter;
    private ArrayList<String> memberList;
    private ArrayList<GroupMemberListener> groupMemberListenerList = new ArrayList<>();
    private ArrayList<GroupConnectionListener> groupConnectionListenerList  = new ArrayList<>();
    private ArrayList<GroupCreationListener> groupCreationListenerList = new ArrayList<>();
    private CreationStatus creationStatus = CreationStatus.GROUP_NOT_CREATED;
    private JoinStatus joinStatus = JoinStatus.NOT_CONNECTED;
    private EasyHandler handler;
    private AppNotification networkNotification;

    public enum CreationStatus {
        GROUP_NOT_CREATED,
        GROUP_CREATED,
        GROUP_WAITING_FOR_CREATION
    }

    public enum JoinStatus {
        NOT_CONNECTED,
        RECENTLY_DISCONNECTED,
        CONNECTING,
        EXCHANGING_INFO,
        CONNECTED
    }

    public enum Mode {
        CREATE_GROUP,
        JOIN_GROUP
    }

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

        synchronized void addMember(String deviceId, String username) {
            groupMembers.put(deviceId, new Member(username, null));
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
        public LongSparseArray<String> songCache;

        Member(String name, String deviceName) {
            this.name = name;
            this.deviceName = deviceName;
            this.songCache = new LongSparseArray<>();
        }
    }

    public interface GetSongListener {
        void onGotSong(String songFilePath);
        void onFailedGettingSong();
    }

    public interface GetAlbumArtListener {
        void onGotAlbumArt(File imageFile);
        void onFailedGettingAlbumArt();
    }

    public static void getSong(String memberName, final long songId,
                               final GetSongListener getSongListener) {
        String memberId = currentGroup.getMemberId(memberName);

        ShareGroup group = ShareGroup.shareGroupWeakReference.get();

        final Member member = currentGroup.getMemberFromUsername(memberName);
        String cachedSongName = member.songCache.get(songId);


        if(cachedSongName != null && new File(Library.FILE_SAVE_LOCATION,cachedSongName).exists()) {
            getSongListener.onGotSong(Library.FILE_SAVE_LOCATION+"/"+cachedSongName);
        } else {

            group.netService.sendRequest(memberId, ACTION_GET_SONG, songId,
                    new ResponseListener() {
                        @Override
                        public void onResponseReceived(Object responseData) {
                            member.songCache.put(songId, ((File) responseData).getName());
                            getSongListener.onGotSong(((File) responseData).getAbsolutePath());
                        }

                        @Override
                        public void onRequestFailed() {
                            getSongListener.onFailedGettingSong();
                        }
                    });
        }
    }

    static void getAlbumArt(String userName, long albumId,
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

    public static void deleteCacheFile(String fileName) {
        ShareGroup group = ShareGroup.shareGroupWeakReference.get();
        group.netService.deleteCacheFile(fileName);
    }

    private File albumArtRequest(Long albumId) {
        return Library.getAlbumArt(albumId);
    }

    private File songRequest(long songId) {
        Cursor cursor = Library.getSong(null, songId);
        if(cursor.moveToFirst()) {
            return new File(Song.toSong(cursor).getFilePath());
        } else {
            return null; //TODO: something less destructive. This will cause the receiver to throw an exception due to null file
        }
    }

    public ShareGroup(Context context, String userName, Mode mode){
        this.mode = mode;
        shareGroupWeakReference = new WeakReference<ShareGroup>(this);

        deleteFiles();

        this.context = context;
        ShareGroup.userName = userName;

        memberList = new ArrayList<>();
        memberList.add(userName);

        handler = new EasyHandler();
        foundGroups = new ArrayList<GlidePlayerGroup>();
        currentGroup = null;
        context.startService(new Intent(context, NetworkService.class));
        networkNotification = AppNotification.getInstance(context);
        networkNotification.displayNetworkNotification(null, false);
    }

    public Mode getMode() { return mode; }

    public void createGroup(final String groupName, final GroupCreationListener groupCreationListener,
                            GroupMemberListener groupMemberListener) {
        if(netService == null) { netService = NetworkService.getServiceBinder(); }
        isOwner = true;
        currentGroup = new GlidePlayerGroup(null,null,groupName, 1);

        registerGroupCreationListener(groupCreationListener);
        GroupCreationListener localCreationListener = new GroupCreationListener() {
            @Override
            public void onGroupCreated() {
                networkNotification.displayNetworkNotification(currentGroup.groupName, true);
                for(final GroupCreationListener listener : groupCreationListenerList) {
                    creationStatus = CreationStatus.GROUP_CREATED;
                    EasyHandler.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGroupCreated();
                        }
                    });
                }
            }

            @Override
            public void onGroupCreationFailed(final String failureMessage) {
                for(final GroupCreationListener listener : groupCreationListenerList) {
                    EasyHandler.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            creationStatus = CreationStatus.GROUP_NOT_CREATED;
                            listener.onGroupCreationFailed(failureMessage);
                        }
                    });
                }
            }
        };

        registerGroupMemberListener(groupMemberListener);
        creationStatus = CreationStatus.GROUP_WAITING_FOR_CREATION;
        netService.createGroup(userName, groupName, localCreationListener, this, this);
    }

    public String getGroupName() {
        if(currentGroup != null) {
            return currentGroup.groupName;
        } else {
            return lastJoinedGroupName;
        }
    }

    public CreationStatus getCreationStatus() { return creationStatus; }

    public void deleteGroup() {
        if(netService == null) { netService = NetworkService.getServiceBinder(); }
        netService.deleteGroup();
        if(currentGroup != null) {
            Library.clearRemoteTables();
            for (Member member : currentGroup.groupMembers.values()) {
                clearPlayQueue(member.name);
            }
        }
    }

    public ArrayList<GlidePlayerGroup> getGroupList() {
        return foundGroups;
    }

    public void findGroups(ArrayAdapter groupListAdapter) {
        if(netService == null) { netService = NetworkService.getServiceBinder(); }
        this.groupListAdapter = groupListAdapter;
        netService.discoverGroups(this, this);
    }

    public void stopFindingGroups() {
        if(netService == null) { netService = NetworkService.getServiceBinder(); }
        netService.stopDiscovery();
        foundGroups.clear();
        groupListAdapter.clear();
    }

    public void connectToGroup(int groupListIndex, final GroupConnectionListener connectionListener,
                               final GroupMemberListener memberListener) {
        //TODO: check if anyone else in the group has the same username
        if(netService == null) { netService = NetworkService.getServiceBinder(); }
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
                lastJoinedGroupName = currentGroup.groupName;
                joinStatus = JoinStatus.EXCHANGING_INFO;
                networkNotification.displayNetworkNotification(currentGroup.groupName, false);
                for(GroupConnectionListener listener : groupConnectionListenerList) {
                    listener.onExchangingInfo();
                }
            }

            @Override
            public void onExchangingInfo() {} //never called
            @Override
            public void onConnectionFailed(final String failureMessage) {
                joinStatus = JoinStatus.NOT_CONNECTED;
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
                for(Member member: currentGroup.groupMembers.values()) {
                    clearPlayQueue(member.name);
                }

                memberList.clear();
                memberList.add(userName);
                Library.clearRemoteTables();
                currentGroup = null;
                stopFindingGroups();
                joinStatus = JoinStatus.RECENTLY_DISCONNECTED;
                networkNotification.displayNetworkNotification(null, false);
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

        joinStatus = JoinStatus.CONNECTING;
        netService.connectToGroup(groupId, connectionListener2, this, this);
    }

    public JoinStatus getJoinStatus() { return joinStatus; }

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
    
    public void registerGroupCreationListener(GroupCreationListener listener) {
        if(!groupCreationListenerList.contains(listener)) {
            groupCreationListenerList.add(listener);
        }
    }
    
    public void unregisterGroupCreationListener(GroupCreationListener listener) {
        groupCreationListenerList.remove(listener);
    }

    private void clearPlayQueue(final String userName) {
        context.bindService(new Intent(context, PlayerService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        ((PlayerService.PlayerServiceBinder)service).removeRemoteUserSongs(userName);
                        context.unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {}
                },
                Context.BIND_ABOVE_CLIENT);
    }

    @Override
    public void close() {
        netService = null;
        context.stopService(new Intent(context, NetworkService.class));
        handler.closeAllHandlers();
        deleteFiles();
        shareGroupWeakReference = null;
        networkNotification.dismissNetworkNotification();
    }

    private void deleteFiles() {
        File file = new File(Library.FILE_SAVE_LOCATION);
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
                new Member(ownerName, deviceName),
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
        final String memberName = currentGroup.groupMembers.get(member).name;

        Library.deleteUser(memberName);
        clearPlayQueue(memberName);

        memberList.remove(memberName);
        for(final GroupMemberListener listener : groupMemberListenerList) {
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onMemberLeft(memberName);
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

                    currentGroup.addMember(memberId, newMemberUsername);

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

        try {
            if(sendFirst) {
                Library.sendOverStream(socket.getOutputStream());
                Library.getFromStream(socket.getInputStream(), newMemberUsername);
            } else {
                Library.getFromStream(socket.getInputStream(), newMemberUsername);
                Library.sendOverStream(socket.getOutputStream());
            }

            socket.close();

            currentGroup.addMember(memberId, newMemberUsername);
        } catch (IOException | JSONException e) {
            Log.d("exchange libs", e.toString());
        }
    }

    @Override //always on a separate thread
    public Object onNewRequest(String deviceId, int action, Object requestData) {
        if(action == ACTION_GET_USERNAME) {
            currentGroup.addMember(deviceId, (String) requestData);
            return userName;
        } else if(action == NetworkService.ACTION_RAW_SOCKET) {
            exchangeLibraries(deviceId, (Socket) requestData, false);
            updateLibraryLists(deviceId);
            joinStatus = JoinStatus.CONNECTED;
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
            return songRequest((Long)requestData);
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
            return albumArtRequest((Long) requestData);
        }

        else return null;
    }

    @Override
    public void error(String errorMsg) {
        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
    }
}