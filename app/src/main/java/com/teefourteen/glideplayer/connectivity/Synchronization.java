/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.connectivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.connectivity.listeners.ResponseListener;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.services.PlayerService;
import com.teefourteen.glideplayer.video.Video;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("ConstantConditions")
public class Synchronization implements Group.RequestListener {
    private static final int ACTION_FETCH_MUSIC_SESSION = 1000;
    private static final int ACTION_FETCH_VIDEO_SESSION = 1001;
    public enum SessionType {
        MUSIC,
        VIDEO
    }

    private static Synchronization instance;
    private Session activeSession = null;

    public interface FetchSessionsListener {
        void sessionsFetched(ArrayList<Session> sessions);
        void fetchFailed();
    }

    public interface JoinSessionListener {
        void sessionJoined(Session session);
        void sessionJoinFailed();
    }

    static void createInstance() {
        instance = new Synchronization();
    }

    public static Synchronization getInstance() {
        return instance;
    }

    public Session getActiveSession() {
        return activeSession;
    }

    void destroy() {
        if(activeSession != null) {
            leaveSession();
            activeSession = null;
            instance = null;
        }
    }

    public void fetchMusicSessions(final FetchSessionsListener listener) {
        fetchSessions(listener, SessionType.MUSIC);
    }

    public void fetchVideoSessions(final FetchSessionsListener listener) {
        fetchSessions(listener, SessionType.VIDEO);
    }

    private void fetchSessions(final FetchSessionsListener listener, final SessionType type) {
        Group group = Group.getInstance();
        final ArrayList<Session> sessions = new ArrayList<>();

        final ArrayList<String> memberList = group.getMemberList();
        if(memberList.size() == 1)  {
            listener.sessionsFetched(sessions);
            return;
        }

        int action = (type == SessionType.MUSIC) ?
                ACTION_FETCH_MUSIC_SESSION : ACTION_FETCH_VIDEO_SESSION;
        for(final String member : memberList) {
            if(member.equals(Group.userName)) continue;
            group.sendSyncRequest(member, action, null, new ResponseListener() {
                @Override
                public void onResponseReceived(Object responseData) {
                    if(responseData != null) {
                        if(type == SessionType.MUSIC) {
                            sessions.add(MusicSession.fromJSON((String) responseData));
                        } else if(type == SessionType.VIDEO) {
                            sessions.add(VideoSession.fromJSON((String) responseData));
                        }
                    }
                    if(memberList.indexOf(member) == memberList.size() -1) {
                        EasyHandler.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.sessionsFetched(sessions);
                            }
                        });
                    }
                }

                @Override
                public void onRequestFailed() {
                    if(memberList.indexOf(member) == memberList.size() -1) {
                        if(sessions.size() > 0) {
                            EasyHandler.executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.sessionsFetched(sessions);
                                }
                            });
                        } else {
                            EasyHandler.executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.fetchFailed();
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    public Session createMusicSession() {
        if(activeSession == null) {
            MusicSession musicSession = new MusicSession(Group.userName);
            activeSession = musicSession;
            return musicSession;
        } else {
            throw new IllegalStateException("Active session already exists. Cannot create new Session");
        }
    }

    public Session createVideoSession() {
        if(activeSession == null) {
            VideoSession videoSession = new VideoSession(Group.userName);
            activeSession = videoSession;
            return videoSession;
        } else {
            throw new IllegalStateException("Active session already exists. Cannot create new Session");
        }
    }

    public void joinSession(final Session session, final JoinSessionListener listener) {
        session.join(new JoinSessionListener() {
            @Override
            public void sessionJoined(Session s) {
                activeSession = s;
                String owner = activeSession.getOwnerName();
                long clockOffset = Group.getInstance().getClockOffset(owner);
                activeSession.setClockOffset(clockOffset);
                listener.sessionJoined(activeSession);
            }

            @Override
            public void sessionJoinFailed() {
                listener.sessionJoinFailed();
            }
        });
    }

    public void leaveSession() {
        activeSession.leave();
        activeSession = null;
    }

    @Override
    public Object onNewRequest(String username, int action, Object requestData) {
        if(action == ACTION_FETCH_MUSIC_SESSION) {
            return fetchMusicSessionsRequest();
        } else if(action == ACTION_FETCH_VIDEO_SESSION) {
            return fetchVideoSessionsRequest();
        } else if(activeSession != null) {
            return activeSession.onNewRequest(username, action, requestData);
        } else {
            return null;
        }
    }

    private String fetchMusicSessionsRequest() {
        if(activeSession != null && activeSession instanceof MusicSession && activeSession.isOwner()) {
            return activeSession.toJSONArray().toString();
        } else {
            return null;
        }
    }

    private String fetchVideoSessionsRequest() {
        if(activeSession != null && activeSession instanceof VideoSession && activeSession.isOwner()) {
            return activeSession.toJSONArray().toString();
        } else {
            return null;
        }
    }








    public static abstract class Session implements Group.RequestListener {
        private static final int ACTION_JOIN_SESSION = 1100;
        private static final int ACTION_MEMBER_JOINED = 1101;
        private static final int ACTION_MEMBER_LEFT = 1102;
        private static final int ACTION_NEW_OWNER = 1103;
        private static final int ACTION_LEAVE_SESSION = 1104;

        
        protected String ownerName = null;
        protected ArrayList<String> memberList = new ArrayList<>();

        //don't send
        protected long clockOffset = 0;

        public interface BroadcastListener {
            void onBroadcastComplete(int successes, int failures);
        }

        protected Session() {}

        public void setFields(String jsonArrayString) {
            try {
                JSONArray jsonArray = new JSONArray(jsonArrayString);
                setFields(jsonArray);
            } catch (JSONException e) {
                throw new RuntimeException("Session rebuild failed. JSON Exception thrown: \n"
                 + e);
            }
        }

        public void setFields(JSONArray jsonArray) {
            try {
                ownerName = jsonArray.getString(0);
                JSONArray members = jsonArray.getJSONArray(1);
                for (int i = 0; i < members.length(); i++) {
                    memberList.add(jsonArray.getString(i));
                }
                buildQueueFromJSON(jsonArray.getJSONArray(2));
            } catch (JSONException e) {
                throw new RuntimeException("Session rebuild failed. JSON Exception thrown: \n"
                        + e);
            }
        }

        JSONArray toJSONArray() {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(ownerName);

            JSONArray members = new JSONArray();
            for(String member : memberList) {
                members.put(member);
            }

            jsonArray.put(members);

            jsonArray.put(getQueueAsJSON());

            return jsonArray;
        }

        public boolean isOwner() {
            return Group.userName.equals(ownerName);
        }

        public String getOwnerName() {
            return ownerName;
        }

        public int getNumberOfMembers() {
            return memberList.size();
        }

        synchronized void join(final JoinSessionListener listener) {
            if (memberList.contains(Group.userName)) {
                listener.sessionJoined(this);
                return;
            }
            Group.getInstance().sendSyncRequest(ownerName, ACTION_JOIN_SESSION, null,
                    new ResponseListener() {
                        @Override
                        public void onResponseReceived(Object responseData) {
                            if (responseData != null) {
                                try {
                                    JSONArray jsonArray = new JSONArray((String) responseData);
                                    Session session = newSession(jsonArray);
                                    memberList.add(Group.userName);
                                    listener.sessionJoined(session);
                                } catch (JSONException e) {
                                    throw new RuntimeException("JSON exception: " + e);
                                }
                            } else {
                                listener.sessionJoinFailed();
                            }
                        }

                        @Override
                        public void onRequestFailed() {
                            listener.sessionJoinFailed();
                        }
                    });
        }

        abstract Session newSession(JSONArray jsonArray);

        void leave() {
            Group group = Group.getInstance();
            memberList.remove(Group.userName);

            if(memberList.size() > 0) {
                if(isOwner()) {
                    ownerName = memberList.get(0);
                    for(String member : memberList) {
                        group.sendSyncRequest(member, ACTION_NEW_OWNER, ownerName, null);
                    }
                }

                group.sendSyncRequest(ownerName, ACTION_LEAVE_SESSION, null, null);
            }
        }

        void setClockOffset(long clockOffset) {
            this.clockOffset = clockOffset;
        }

        synchronized public void broadcastMessage(int action, JSONArray data,
                                                  final BroadcastListener listener) {
            final int numberOfMembers = memberList.size()-1;
            if(numberOfMembers == 0) {
                listener.onBroadcastComplete(0,0);
                return;
            }
            final int result[] = {0,0};
            for(String member : memberList) {
                if(member.equals(Group.userName)) continue;
                new SyncMessageTask(action, data, new ResponseListener() {
                    @Override
                    public void onResponseReceived(Object responseData) {
                        if((++result[0] + result[1]) == numberOfMembers) {
                            listener.onBroadcastComplete(result[0], result[1]);
                        }
                    }

                    @Override
                    public void onRequestFailed() {
                        if((result[0] + ++result[1]) == numberOfMembers) {
                            listener.onBroadcastComplete(result[0], result[1]);
                        }
                    }
                }).execute(member);
            }
        }

        @Override
        public Object onNewRequest(String username, int action, Object requestData) {
            if(action == ACTION_JOIN_SESSION) {
                return joinSessionRequest(username);
            } else if(action == ACTION_MEMBER_JOINED) {
                memberList.add((String) requestData);
                return null;
            } else if(action == ACTION_NEW_OWNER) {
                ownerName = (String)requestData;
                return null;
            } else if(action == ACTION_LEAVE_SESSION) {
                leaveSessionRequest(username);
                return null;
            } else if(action == ACTION_MEMBER_LEFT) {
                memberList.remove((String)requestData);
                return null;
            } else {
                return null;
            }
        }

        synchronized private String joinSessionRequest(String memberName) {
            for(String member : memberList) {
                if(member.equals(Group.userName)) continue;
                Group.getInstance().sendSyncRequest(member, ACTION_MEMBER_JOINED, memberName, null);
            }
            memberList.add(memberName);
            return toJSONArray().toString();
        }

        synchronized private void leaveSessionRequest(String memberName) {
            memberList.remove(memberName);
            for(String member : memberList) {
                if(member.equals(Group.userName)) continue;
                Group.getInstance().sendSyncRequest(member, ACTION_MEMBER_LEFT, memberName, null);
            }
        }

        abstract JSONArray getQueueAsJSON();

        abstract void buildQueueFromJSON(JSONArray jsonArray);

        abstract void removeFromPlayQueue(String memberName);

        public abstract String getCurrentPlaying();


        private class SyncMessageTask extends AsyncTask<String, Object, Object> {
            int action;
            JSONArray data;
            ResponseListener listener;

            SyncMessageTask(int action, JSONArray data, ResponseListener listener) {
                this.action = action;
                this.data = data;
                this.listener = listener;
            }

            @Override
            protected Object doInBackground(String... params) {
                String username = params[0];
                Group.getInstance().sendSyncRequest(username, action, data.toString(), listener);
                return null;
            }
        }
    }


    public static class MusicSession extends Session {
        private static final String SYNC_PLAY_THREAD = "sync_play_thread";
        private static final int ACTION_PLAY = 1200;
        private static final int ACTION_PREPARE_SONG = 1201;
        private static final int ACTION_PAUSE_SONG = 1202;
        private static final int ACTION_ADD_SONG = 1203;
        private static final int ACTION_APPEND_SONG = 1204;

        private EasyHandler handler = new EasyHandler();
        private PlayQueue playQueue = new PlayQueue(new ArrayList<Song>(), 0);
        private Context context;
        private PlayerService.PlayerServiceBinder binder;
        private ServiceConnection serviceConnection;
        private EventListener eventListener;
        
        private class NullBinderException extends Exception {}
        
        public interface InitListener {
            void initialized();
        }

        public interface AddSongListener {
            void songAdded();
            void songAddFailed();
        }

        public interface EventListener {
            void queueUpdated();
        }

        MusicSession(String ownerName) {
            super.ownerName = ownerName;
            memberList.add(ownerName);
            handler.createHandler(SYNC_PLAY_THREAD);
        }

        @Override
        Session newSession(JSONArray jsonArray) {
            return MusicSession.fromJSON(jsonArray);
        }

        private MusicSession() {
            handler.createHandler(SYNC_PLAY_THREAD);
        }

        static MusicSession fromJSON(String jsonArrayString) {
            try {
                JSONArray jsonArray = new JSONArray(jsonArrayString);
                return MusicSession.fromJSON(jsonArray);
            } catch(JSONException e) {
                return null;
            }
        }

        static MusicSession fromJSON(JSONArray jsonArray) {
            MusicSession musicSession = new MusicSession();
            musicSession.setFields(jsonArray);
            return musicSession;
        }

        private PlayerService.PlayerServiceBinder getBinder() throws NullBinderException {
            if(binder == null) {
                Log.e("MusicSession", "Service unbound. Player operation not possible. Either " +
                        "MusicSession.initialize() was not called or MusicSession.leave() was" +
                        "called immediately before the player operation.");
                throw new NullBinderException();
            } else {
                return binder;
            }
        }

        @Override
        JSONArray getQueueAsJSON() {
            JSONArray queueJSON = new JSONArray();

            for (Song song : playQueue.getQueue()) {
                JSONArray s = new JSONArray();
                s.put((song.isRemote()) ? song.getLibraryUsername() : Group.userName);
                s.put(song.get_id());
                queueJSON.put(s);
            }

            return queueJSON;
        }

        @Override
        synchronized void buildQueueFromJSON(JSONArray jsonArray) {
            try {
                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONArray s = jsonArray.getJSONArray(i);
                    String username = (s.getString(0).equals(Group.userName)) ?
                            null : s.getString(0);
                    Cursor cursor = Library.getSong(username, s.getLong(1));
                    cursor.moveToFirst();
                    playQueue.appendSong(Song.toSong(cursor));
                    cursor.close();
                }
            } catch (JSONException e) {
                throw new RuntimeException("could not decode play queue. JSON exception: \n"
                + e);
            }
        }

        @Override
        void removeFromPlayQueue(String memberName) {
            playQueue.removeRemoteSongs(memberName);
        }

        @Override
        public String getCurrentPlaying() {
            if(playQueue.getQueue().size() == 0) return "-";
            return playQueue.getCurrentPlaying().getTitle();
        }
        
        public void initialize(Context context,@NonNull final InitListener initListener,
                               EventListener eventListener) {
            this.context = context;
            this.eventListener = eventListener;
            this.serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    binder = (PlayerService.PlayerServiceBinder) service;
                    initListener.initialized();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            
            context.bindService(new Intent(context, PlayerService.class), serviceConnection,
                    Context.BIND_ABOVE_CLIENT);
        }

        @Override
        void leave() {
            binder = null;
            context.unbindService(serviceConnection);
            handler.killHandler(SYNC_PLAY_THREAD);
            super.leave();
        }

        public PlayQueue getPlayQueue() {
            return playQueue;
        }

        public void addSong(Song song, final AddSongListener listener) {
            JSONArray songInfo = new JSONArray();
            songInfo.put((song.isRemote()) ? song.getLibraryUsername() : Group.userName);
            songInfo.put(song.get_id());
            if(isOwner()) {
                addSongRequest(songInfo.toString());
                listener.songAdded();
            } else {
                Group.getInstance().sendSyncRequest(ownerName, ACTION_ADD_SONG, songInfo.toString(),
                        new ResponseListener() {
                            @Override
                            public void onResponseReceived(Object responseData) {
                                listener.songAdded();
                            }

                            @Override
                            public void onRequestFailed() {
                                listener.songAddFailed();
                            }
                        });
            }
        }

        public void play(final int index) {
            final JSONArray songInfo = new JSONArray();
            Song song = playQueue.getSongAt(index);
            songInfo.put((song.isRemote()) ? song.getLibraryUsername() : Group.userName);
            songInfo.put(song.get_id());

            final BroadcastListener broadcastListener = new BroadcastListener() {
                @Override
                public void onBroadcastComplete(int successes, int failures) {
                    syncAndPlay(songInfo);
                }
            };

            final JSONArray data = new JSONArray();
            data.put(index);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getBinder().changeTrackAndPrepare(index);
                        broadcastMessage(ACTION_PREPARE_SONG, data, broadcastListener);
                    } catch (NullBinderException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void syncAndPlay(JSONArray songInfo) {
            JSONArray data = new JSONArray();
            Handler handler = MusicSession.this.handler.getHandler(SYNC_PLAY_THREAD);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    binder.play();
                }
            };

            data.put(System.currentTimeMillis() + 1000 - clockOffset);
            handler.postDelayed(r, 1000);

            broadcastMessage(ACTION_PLAY, data, null);
        }

        public void pause() {
            try {
                getBinder().pause();

                int seek = getBinder().getTrueSeek();
                JSONArray data = new JSONArray();
                data.put(seek);

                broadcastMessage(ACTION_PAUSE_SONG, data, null);
            } catch (NullBinderException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object onNewRequest(String username, int action, Object requestData) {
            if(action == ACTION_ADD_SONG) {
                return addSongRequest((String) requestData);
            } else if(action == ACTION_APPEND_SONG) {
                return appendSongRequest((String)requestData);
            } else if(action == ACTION_PREPARE_SONG) {
                return prepareSongRequest((String)requestData);
            } else if(action == ACTION_PLAY) {
                return playSongRequest((String)requestData);
            } else if(action == ACTION_PAUSE_SONG) {
                return pauseSongRequest((String)requestData);
            } else {
                return super.onNewRequest(username, action, requestData);
            }
        }

        synchronized private String addSongRequest(String jsonArrayString) {
            try {
                JSONArray songInfo = new JSONArray(jsonArrayString);
                appendSongRequest(jsonArrayString);
                final CountDownLatch countDownLatch = new CountDownLatch(memberList.size()-1);
                broadcastMessage(ACTION_APPEND_SONG, songInfo, new BroadcastListener() {
                    @Override
                    public void onBroadcastComplete(int successes, int failures) {
                        countDownLatch.countDown();
                    }
                });
                countDownLatch.await();
                return "success";
            } catch (InterruptedException | JSONException e) {
                return null;
            }
        }

        private String appendSongRequest(String jsonArrayString) {
            try {
                JSONArray songInfo = new JSONArray(jsonArrayString);
                String username = (songInfo.getString(0).equals(Group.userName)) ?
                        null : songInfo.getString(0);
                Cursor cursor = Library.getSong(username, songInfo.getLong(1));
                cursor.moveToFirst();
                Song song = Song.toSong(cursor);
                cursor.close();
                playQueue.appendSong(song);
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(eventListener!=null) eventListener.queueUpdated();
                    }
                });
                return "success";
            } catch (JSONException e) {
                return null;
            }
        }

        private String prepareSongRequest(String dataString) {
            try {
                JSONArray data = new JSONArray(dataString);
                int index = data.getInt(0);
                getBinder().changeTrackAndPrepare(index);
                return "success";
            } catch(JSONException e) {
                return null;
            } catch (NullBinderException e) {
                e.printStackTrace();
                return null;
            }
        }

        private String playSongRequest(String data) {
            try {
                JSONArray jsonArray = new JSONArray(data);
                long execTime = jsonArray.getLong(0);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getBinder().play();
                        } catch (NullBinderException e) {
                            e.printStackTrace();
                        }
                    }
                };

                handler.getHandler(SYNC_PLAY_THREAD)
                        .postDelayed(r, execTime - System.currentTimeMillis() + clockOffset);
                return "success";
            } catch (JSONException e) {
                return null;
            }
        }

        private String pauseSongRequest(String dataString) {
            try {
                getBinder().pause();
                int seek = new JSONArray(dataString).getInt(0);
                getBinder().seek(seek);
                return "success";
            } catch (JSONException e) {
                return null;
            } catch (NullBinderException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class VideoSession extends Session {
        private static final String SYNC_PLAY_THREAD = "sync_play_thread";
        private static final int ACTION_PLAY = 1200;
        private static final int ACTION_PREPARE_VIDEO = 1201;
        private static final int ACTION_PAUSE_VIDEO = 1202;
        private EasyHandler handler = new EasyHandler();
        private EventListener eventListener;
        String currentVideoTitle;

        public interface EventListener {
            void prepareVideo(Video video);
            void play();
            int pause();
            void seek(int seek);
        }

        private VideoSession() {
            handler = new EasyHandler();
            handler.createHandler(SYNC_PLAY_THREAD);
        }

        @Override
        Session newSession(JSONArray jsonArray) {
            return fromJSON(jsonArray);
        }

        VideoSession(String ownerName) {
            super.ownerName = ownerName;
            memberList.add(ownerName);
            handler.createHandler(SYNC_PLAY_THREAD);
        }

        public void setEventListener(EventListener listener) {
            eventListener = listener;
        }

        static VideoSession fromJSON(String jsonArrayString) {
            try {
                JSONArray jsonArray = new JSONArray(jsonArrayString);
                return VideoSession.fromJSON(jsonArray);
            } catch(JSONException e) {
                return null;
            }
        }

        static VideoSession fromJSON(JSONArray jsonArray) {
            VideoSession videoSession = new VideoSession();
            videoSession.setFields(jsonArray);
            return videoSession;
        }

        public void play(final Video video) {
            final JSONArray videoInfo = new JSONArray();
            currentVideoTitle = video.title;
            videoInfo.put(video.videoId);
            videoInfo.put((video.libraryUsername == null) ? Group.userName : video.libraryUsername);

            final BroadcastListener broadcastListener = new BroadcastListener() {
                @Override
                public void onBroadcastComplete(int successes, int failures) {
                    syncAndPlay(new Runnable() {
                        @Override
                        public void run() {
                            if(eventListener!=null) eventListener.play();
                        }
                    });
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(eventListener!=null) eventListener.prepareVideo(video);
                    broadcastMessage(ACTION_PREPARE_VIDEO, videoInfo, broadcastListener);
                }
            }).start();
        }

        public void syncAndPlay(Runnable play) {
            JSONArray data = new JSONArray();
            Handler handler = this.handler.getHandler(SYNC_PLAY_THREAD);

            data.put(System.currentTimeMillis() + 1000 - clockOffset);
            handler.postDelayed(play, 1000);

            broadcastMessage(ACTION_PLAY, data, null);
        }

        public void pause() {
            int seek = eventListener.pause();
            JSONArray data = new JSONArray();
            data.put(seek);

            broadcastMessage(ACTION_PAUSE_VIDEO, data, null);
        }

        @Override
        JSONArray getQueueAsJSON() {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(currentVideoTitle);
            return jsonArray;
        }

        @Override
        void buildQueueFromJSON(JSONArray jsonArray) {
            try {
                currentVideoTitle = jsonArray.getString(0);
            } catch (JSONException e) {/*ignore*/}
        }

        @Override
        void removeFromPlayQueue(String memberName) {}

        @Override
        public String getCurrentPlaying() {
            return (currentVideoTitle == null) ? "-" : currentVideoTitle;
        }

        @Override
        public Object onNewRequest(String username, int action, Object requestData) {
            if(action == ACTION_PREPARE_VIDEO) {
                return prepareVideoRequest((String) requestData);
            } else if(action == ACTION_PLAY) {
                return playRequest((String)requestData);
            } else if(action == ACTION_PAUSE_VIDEO) {
                return pauseRequest((String) requestData);
            } else {
                return super.onNewRequest(username, action, requestData);
            }
        }

        private String prepareVideoRequest(String jsonArrayString) {
            try {
                JSONArray data = new JSONArray(jsonArrayString);
                Cursor cursor = Library.getVideo((data.getString(1).equals(Group.userName)) ?
                        null : data.getString(1), data.getLong(0));
                if(!cursor.moveToFirst()) return null;
                Video video = Video.toVideo(cursor);
                cursor.close();
                if(eventListener != null) eventListener.prepareVideo(video);
                return "success";
            } catch(JSONException e) {
                return null;
            }
        }

        private String playRequest(String data) {
            try {
                JSONArray jsonArray = new JSONArray(data);
                long execTime = jsonArray.getLong(0);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if(eventListener != null) eventListener.play();
                    }
                };

                handler.getHandler(SYNC_PLAY_THREAD)
                        .postDelayed(r, execTime - System.currentTimeMillis() + clockOffset);
                return "success";
            } catch (JSONException e) {
                return null;
            }
        }

        private String pauseRequest(String dataString) {
            try {
                if(eventListener != null) eventListener.pause();
                int seek = new JSONArray(dataString).getInt(0);
                if(eventListener != null) {
                    eventListener.seek(seek);
                    eventListener.pause();
                }
                return "success";
            } catch (JSONException e) {
                return null;
            }
        }
    }
}
