package teefourteen.glideplayer.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import teefourteen.glideplayer.music.MusicPlayer;
import teefourteen.glideplayer.music.Song;

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
        CHANGE_TRACK, PLAY_NEXT, PLAY_PREV,
        PLAY_NEW_QUEUE, PAUSE, RESUME
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

        private void play(Song song) {
            if(player.playSong(song))
                notifyPlayerActivity(SONG_STARTED);
            else
                notifyPlayerActivity(PLAYBACK_FAILED);
        }

        public void playNewSong() {
            //PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING
            player.reset();
            play(playQueue.getCurrentPlaying());
        }

        public void playNext() {
            //TODO: PUT CODE FOR CHECKING IF THREAD IS ALREADY RUNNING AND PLAYING IN REQUIRED METHODS
            player.reset();
            play(playQueue.next());
        }

        public void playPrev() {
            player.reset();
            play(playQueue.prev());
        }

        public void pause() {
            player.pauseSong();
        }

        public void resume() {
            player.playSong(playQueue.getCurrentPlaying());
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
                    case CHANGE_TRACK:
                        int songIndex = intent.getIntExtra(EXTRA_SONG_INDEX, 0);
                        playQueue.changeTrack(songIndex);
                        playerHandler.playNewSong();
                        break;

                    case PLAY_NEXT:
                        playerHandler.playNext();
                        break;

                    case PLAY_PREV:
                        playerHandler.playPrev();
                        break;

                    case PLAY_NEW_QUEUE:
                        playQueue = intent.getParcelableExtra(EXTRA_PLAY_QUEUE);
                        playerHandler.playNewSong();
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
