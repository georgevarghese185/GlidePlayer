package teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import teefourteen.glideplayer.music.MusicPlayer;
import teefourteen.glideplayer.music.PlayQueue;

import static teefourteen.glideplayer.activities.MainActivity.globalPlayQueue;
import static teefourteen.glideplayer.activities.PlayerActivity.playerActivityHandler;

public class PlayerService extends Service {

    //TODO: All stuff in media player guide
    static final public String EXTRA_SONG_COMMAND="song_command";
    static final public String EXTRA_SONG_INDEX ="song_index";
    static final public String EXTRA_PLAY_QUEUE ="play_queue";
    MusicPlayer player=null;
    private HandlerThread playerHandlerThread;
    private PlayerHandler playerHandler;
    private static final String PLAYER_HANDLER_THREAD_NAME = "player_handler_thread";
    public enum Command {
        CHANGE_TRACK, PLAY_NEXT, PLAY_PREV,
        NEW_QUEUE, PLAY, PAUSE
    }
    public static final int SONG_STARTED = 1;
    public static final int PLAYBACK_FAILED = 2;
    
    public class PlayerHandler extends Handler {
        PlayerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }

        private void play() {
            if(player.playSong(globalPlayQueue.getCurrentPlaying()))
                notifyPlayerActivity(SONG_STARTED);
            else
                notifyPlayerActivity(PLAYBACK_FAILED);
        }

        private void pause() {
            player.pauseSong();
        }

        private void next() {
            player.reset();
            globalPlayQueue.next();
            play();
        }

        private void prev() {
            player.reset();
            globalPlayQueue.prev();
            play();
        }

        private void changeTrack(int songIndex) {
            player.reset();
            globalPlayQueue.changeTrack(songIndex);
            play();
        }

        private void newQueue(PlayQueue playQueue){
            player.reset();
            globalPlayQueue = playQueue;
            play();
        }

        void notifyPlayerActivity(int messageContent) {
            if(playerActivityHandler!=null) {
                Message message = obtainMessage();
                message.arg1 = messageContent;
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
                    case PLAY:
                        playerHandler.play();
                        break;

                    case PAUSE:
                        playerHandler.pause();
                        break;

                    case PLAY_NEXT:
                        playerHandler.next();
                        break;

                    case PLAY_PREV:
                        playerHandler.prev();
                        break;

                    case CHANGE_TRACK:
                        playerHandler.changeTrack(intent.getIntExtra(EXTRA_SONG_INDEX, 0));
                        break;

                    case NEW_QUEUE:
                        playerHandler.newQueue(
                                (PlayQueue)intent.getParcelableExtra(EXTRA_PLAY_QUEUE));
                        break;
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
