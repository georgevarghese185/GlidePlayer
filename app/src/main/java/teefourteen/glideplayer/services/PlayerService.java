package teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.music.MusicPlayer;
import teefourteen.glideplayer.music.PlayQueue;

import static teefourteen.glideplayer.Global.playQueue;

public class PlayerService extends Service {

    //TODO: All stuff in media player guide
    private MusicPlayer player=null;
    private EasyHandler handler;
    private final IBinder binder = new PlayerServiceBinder();
    private SongListener listener;
    private static final String PLAYER_HANDLER_THREAD_NAME = "player_handler_thread";

    public interface SongListener {
        void onSongStarted();
        void onSongPlaybackFailed();
    }

    public class PlayerServiceBinder extends Binder {
        private final PlayerService service = PlayerService.this;

        public void registerSongListener(final SongListener listener) {
            service.listener = listener;
        }

        public void unregisterSongListener() {
            listener = null;
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
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void prev() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.prev();
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void changeTrack(final int songIndex) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.changeTrack(songIndex);
                }
            };
            handler.executeAsync(r, PLAYER_HANDLER_THREAD_NAME);
        }

        public void newQueue(final PlayQueue queue) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    service.newQueue(queue);
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

        if(listener != null) {
            if(playbackSuccessful) listener.onSongStarted();
            else listener.onSongPlaybackFailed();
        }
    }

    public void pause(){ player.pauseSong(); }

    public void next() {
        player.reset();
        playQueue.next();
        play();
    }

    public void prev() {
        player.reset();
        playQueue.prev();
        play();
    }

    public void changeTrack(int songIndex) {
        player.reset();
        playQueue.changeTrack(songIndex);
        play();
    }

    public void newQueue(PlayQueue queue) {
        player.reset();
        Global.playQueue = queue;
        play();
    }

    @Override
    public void onCreate() {
        player = new MusicPlayer(getApplicationContext());
        handler = new EasyHandler();
        handler.createHandler(PLAYER_HANDLER_THREAD_NAME);
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
