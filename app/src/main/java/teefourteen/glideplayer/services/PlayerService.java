package teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ArrayAdapter;

import java.io.PrintWriter;
import java.util.ArrayList;

import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.music.MusicPlayer;
import teefourteen.glideplayer.music.PlayQueue;

import static teefourteen.glideplayer.Global.playQueue;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MusicPlayer.SeekListener{

    //TODO: All stuff in media player guide
    private MusicPlayer player=null;
    private EasyHandler handler;
    private final IBinder binder = new PlayerServiceBinder();
    private ArrayList<SongListener> songListenerList = new ArrayList<>();
    private static final String PLAYER_HANDLER_THREAD_NAME = "player_handler_thread";

    public interface SongListener {
        void onSongStarted();
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


    public void play(){
        boolean playbackSuccessful = player.playSong(playQueue.getCurrentPlaying());


        if(playbackSuccessful) {
            for(final SongListener listener : songListenerList) {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSongStarted();
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

    @Override
    public void onCreate() {
        player = new MusicPlayer(getApplicationContext());
        handler = new EasyHandler();
        handler.createHandler(PLAYER_HANDLER_THREAD_NAME);
        player.registerOnCompletionListener(this);
    }


    public void pause(){ player.pauseSong(); }

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
        play();
    }

    public void seek(int seek){
        player.seek(seek);
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
                    }
                });
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
        handler.closeAllHandlers();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
