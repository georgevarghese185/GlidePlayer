package teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayList;

import teefourteen.glideplayer.AppNotification;
import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.music.MusicPlayer;
import teefourteen.glideplayer.music.PlayQueue;
import teefourteen.glideplayer.music.Song;

import static teefourteen.glideplayer.Global.playQueue;

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

    public final static String EXTRA_PLAY_CONTROL = "play_control";
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

        public void seek(final int seek) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.seek(seek);
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.hasExtra(EXTRA_PLAY_CONTROL)) {
            String extraPlayControl = intent.getStringExtra(EXTRA_PLAY_CONTROL);

            switch (extraPlayControl) {
                case PLAY:
                    ((PlayerServiceBinder)binder).play();
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
        playerNotification.displayPlayerNotification(playQueue.getCurrentPlaying(), false, true);

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
    }

    public void prev() {
        player.reset();
        onSeekUpdated(0);
        playQueue.prev();
    }

    public void changeTrack(int songIndex) {
        player.reset();
        onSeekUpdated(0);
        playQueue.changeTrack(songIndex);
    }

    public void newQueue(PlayQueue queue) {
        player.reset();
        onSeekUpdated(0);
        Global.playQueue = queue;
    }

    public void seek(int seek){
        player.seek(seek);
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
