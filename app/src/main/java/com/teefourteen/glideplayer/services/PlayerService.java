package com.teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.util.ArrayList;

import com.teefourteen.glideplayer.AppNotification;
import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.music.MusicPlayer;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.music.database.Library;

import static com.teefourteen.glideplayer.Global.SHARED_PREFS_NAME;
import static com.teefourteen.glideplayer.Global.playQueue;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MusicPlayer.SeekListener{

    //TODO: All stuff in media player guide
    private MusicPlayer player=null;
    private EasyHandler handler;
    private final IBinder binder = new PlayerServiceBinder();
    private boolean isBound = false;
    private ArrayList<SongListener> songListenerList = new ArrayList<>();
    private static final String PLAYER_HANDLER_THREAD_NAME = "player_handler_thread";
    private AppNotification playerNotification = AppNotification.getInstance(this);
    private SharedPreferences.Editor editor;

    public static final String PLAY_QUEUE_FILE_PATH = Library.DATABASE_LOCATION + "/" + "last_play_queue";
    public final static String EXTRA_PLAY_CONTROL = "play_control";
    private static final String LAST_SONG_INDEX = "last_song";
    private static final String LAST_SEEK = "last_seek";
    public final static String PLAY = "play";
    public final static String PAUSE = "pause";
    public final static String NEXT = "next";
    public final static String PREV = "prev";

    public interface SongListener {
        void onSongStarted(Song song);
        void onSongPlaybackFailed();
        void onTrackAutoChanged();
        void onSongStopped();
        /** seek value between 0 and MusicPlayer.MAX_SEEK_VALUE inclusive */
        void onSeekUpdate(int currentSeek);
        void onPlayQueueDestroyed();
    }

    public class PlayerServiceBinder extends Binder {
        private final PlayerService service = PlayerService.this;

        public void registerSongListener(final SongListener listener) {
            if(songListenerList.size() == 0) {
                player.registerSeekListener(service);
            }
            if(!songListenerList.contains(listener)) {
                songListenerList.add(listener);
            }
        }

        public void unregisterSongListener(SongListener listener) {
            songListenerList.remove(listener);
            if(songListenerList.size() == 0) {
                player.unregisterSeekListener(service);
            }
        }

        public void play(){
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.play();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void pause(){
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.pause();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void next() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.next();
                    service.play();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void prev() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.prev();
                    service.play();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void changeTrack(final int songIndex) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.changeTrack(songIndex);
                    service.play();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void newQueue(final PlayQueue queue) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.newQueue(queue);
                    service.play();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void restoreSavedQueue() {
            final SharedPreferences sharedPreferences =
                    getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);

            playQueue.changeTrack(sharedPreferences.getInt(LAST_SONG_INDEX, 0));
            try {
                player.prepareSong(playQueue.getCurrentPlaying());
                player.trueSeek(sharedPreferences.getInt(LAST_SEEK, 0));
                onSeekUpdated(player.getSeek());
            } catch (IOException e) {
                for (final SongListener listener : songListenerList) {
                    EasyHandler.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSongPlaybackFailed();
                        }
                    });
                }
            }
        }

        public void removeRemoteUserSongs(String userName) {
            if(playQueue != null) {
                boolean empty = playQueue.removeRemoteSongs(userName);
                if(empty) {
                    player.reset();
                    for(SongListener listener : songListenerList) {
                        listener.onPlayQueueDestroyed();
                    }
                    playQueue = null;
                    endService(true);
                }
            }
        }

        public void seek(final int seek) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.seek(seek);
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public int getSeek() {
            return player.getSeek();
        }

        public boolean isPlaying() {
            return player.isPlaying();
        }
    }


    @Override
    public void onCreate() {
        player = new MusicPlayer(getApplicationContext());
        handler = new EasyHandler();
        handler.createHandler(PLAYER_HANDLER_THREAD_NAME);
        player.registerOnCompletionListener(this);
        final SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.hasExtra(EXTRA_PLAY_CONTROL)) {
            String extraPlayControl = intent.getStringExtra(EXTRA_PLAY_CONTROL);

            switch (extraPlayControl) {
                case PLAY:
                    handler.executeAsync(new Runnable() {
                        @Override
                        public void run() {
                            ((PlayerServiceBinder)binder).restoreSavedQueue();
                            ((PlayerServiceBinder)binder).play();
                        }
                    }, PLAYER_HANDLER_THREAD_NAME);

                    break;
                case PAUSE:
                    ((PlayerServiceBinder)binder).pause();
                    break;
                case NEXT:
                    ((PlayerServiceBinder)binder).next();
                    break;
                case PREV:
                    ((PlayerServiceBinder)binder).prev();
                    break;
            }
        }

        return START_STICKY;
    }

    public void play(){
        boolean playbackSuccessful = player.playSong(playQueue.getCurrentPlaying());

        if(playbackSuccessful) {
            startForeground(0, null);
            playerNotification.displayPlayerNotification(playQueue.getCurrentPlaying(), true, true);

            for(final SongListener listener : songListenerList) {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSongStarted(playQueue.getCurrentPlaying());
                    }
                });
            }
        } else {
            for(final SongListener listener : songListenerList) {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSongPlaybackFailed();
                    }
                });
            }
        }
    }

    public void pause(){
        player.pauseSong();
        playerNotification.displayPlayerNotification(playQueue.getCurrentPlaying(), false, false);
        editor.putInt(LAST_SEEK, player.getTrueSeek()).apply();

        for(final SongListener listener : songListenerList) {
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onSongStopped();
                }
            });
        }

        if(!isBound) {
            endService(false);
        }
    }

    public void next() {
        player.reset();
        onSeekUpdated(0);
        playQueue.next();
        editor.putInt(LAST_SEEK, 0).apply();
        editor.putInt(LAST_SONG_INDEX, playQueue.getIndex()).apply();
    }

    public void prev() {
        player.reset();
        onSeekUpdated(0);
        playQueue.prev();
        editor.putInt(LAST_SEEK, 0).apply();
        editor.putInt(LAST_SONG_INDEX, playQueue.getIndex()).apply();
    }

    public void changeTrack(int songIndex) {
        player.reset();
        onSeekUpdated(0);
        playQueue.changeTrack(songIndex);
        editor.putInt(LAST_SEEK, 0).apply();
        editor.putInt(LAST_SONG_INDEX, playQueue.getIndex()).apply();
    }

    public void newQueue(PlayQueue queue) {
        player.reset();
        onSeekUpdated(0);
        Global.playQueue = queue;
        playQueue.saveQueueToFile(PLAY_QUEUE_FILE_PATH);
        editor.putInt(LAST_SEEK, 0).apply();
        editor.putInt(LAST_SONG_INDEX, playQueue.getIndex()).apply();
    }

    public void seek(int seek){
        player.seek(seek);
        editor.putInt(LAST_SEEK, player.getTrueSeek());
    }

    private void endService(boolean dismissNotification) {
        stopForeground(false);
        if(dismissNotification) {
            playerNotification.dismissPlayerNotification();
        } else {
            playerNotification.displayPlayerNotification(playQueue.getCurrentPlaying(), false, false);
        }
        stopSelf();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(!playQueue.isAtLastSong()) {
            next();

            for(final SongListener listener : songListenerList) {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onTrackAutoChanged();
                    }
                });
            }

            play();
        } else {
            for(final SongListener listener : songListenerList) {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSongStopped();
                        listener.onSeekUpdate(0);
                    }
                });
            }
            playerNotification.displayPlayerNotification(playQueue.getCurrentPlaying(), false, true);
            if(!isBound) {
                endService(false);
            }
        }
    }

    @Override
    public void onSeekUpdated(final int newSeek) {
        for(final SongListener listener : songListenerList) {
            EasyHandler.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    listener.onSeekUpdate(newSeek);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.close();
        handler.closeAllHandlers();
        stopForeground(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        isBound = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(!player.isPlaying()) {
            endService(true);
        }
        isBound = false;
        return true;
    }
}
