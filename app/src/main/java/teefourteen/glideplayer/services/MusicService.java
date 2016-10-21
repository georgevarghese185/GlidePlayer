package teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import teefourteen.glideplayer.music.MusicPlayer;
import static teefourteen.glideplayer.activities.MainActivity.playQueue;
import static teefourteen.glideplayer.activities.PlayerActivity.playerActivityHandler;

public class MusicService extends Service {
    //TODO: All stuff in media player guide
    static final public String EXTRA_SONG_COMMAND="song_command";
    static final public String EXTRA_SONG_INDEX ="song_index";
    static final public String EXTRA_PLAY_QUEUE ="play_queue";
    MusicPlayer player=null;
    private HandlerThread playerHandlerThread;
    private PlayerHandler playerHandler;
    private static final String PLAYER_HANDLER_THREAD_NAME = "player_handler_thread";
    public enum Command {
        PLAY_SONG, PLAY_NEXT, PLAY_PREV,
        PLAY_NEW_QUEUE, PAUSE, RESUME
    }
    public static final int SONG_STARTED = 1;
    
    public class PlayerHandler extends Handler {
        PlayerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }

        public void playSong(final int index) {
            //PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING
            player.reset();
            playQueue.setCurrentPlaying(index);
            player.playSong(playQueue.getSongAt(index));
            notifySongStarted();
        }

        public void playNext() {
            //TODO: PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING
            player.reset();
            player.playSong(playQueue.next());
            notifySongStarted();
        }

        public void playPrev() {
            //PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING
            player.reset();
            player.playSong(playQueue.prev());
            notifySongStarted();
        }

        public void pause() {
            //PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING

            player.pauseSong();
        }

        public void resume() {
            //PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING

            player.playSong(playQueue.getCurrentPlaying());
            notifySongStarted();
        }

        void notifySongStarted() {
            if(playerActivityHandler!=null) {
                Message message = obtainMessage();
                message.arg1 = SONG_STARTED;
                playerActivityHandler.sendMessage(message);
            }
        }
    }

    @Override
    public void onCreate() {
        player = new MusicPlayer(getApplicationContext());
        playerHandlerThread = new HandlerThread(PLAYER_HANDLER_THREAD_NAME);
        playerHandlerThread.start();
        playerHandler = new PlayerHandler(playerHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        final Command command = (Command) intent.getSerializableExtra(EXTRA_SONG_COMMAND);
        playerHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (command) {
                    case PLAY_SONG:
                        int songIndex = intent.getIntExtra(EXTRA_SONG_INDEX, 0);
                        playerHandler.playSong(songIndex);
                        break;

                    case PLAY_NEXT:
                        playerHandler.playNext();
                        break;

                    case PLAY_PREV:
                        playerHandler.playPrev();
                        break;

                    case PLAY_NEW_QUEUE:
                        playQueue = intent.getParcelableExtra(EXTRA_PLAY_QUEUE);
                        playerHandler.playSong(0);
                        break;

                    case PAUSE:
                        playerHandler.pause();
                        break;

                    case RESUME:
                        playerHandler.resume();
                }
            }
        });

        return START_NOT_STICKY;
    }

    

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
