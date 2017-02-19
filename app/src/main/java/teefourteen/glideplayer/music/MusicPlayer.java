package teefourteen.glideplayer.music;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.music.database.Library;
import teefourteen.glideplayer.services.PlayerService;


public class MusicPlayer {
    public static final int MAX_SEEK_VALUE = 2000;

    private MediaPlayer mediaPlayer =null;
    private Context context=null;
    private boolean prepared = false;
    private boolean monitorSeek = false;
    private EasyHandler handler = new EasyHandler();
    private static final String SEEK_UPDATER_THREAD_NAME = "seek_updater_thread";
    private ArrayList<MediaPlayer.OnCompletionListener> onCompletionListenerList = new ArrayList<>();
    private ArrayList<SeekListener> seekListenerList = new ArrayList<>();

    public MusicPlayer(Context context) {
        this.context = context;
    }

    public interface SeekListener {
        /** newSeek between 0 and MAX_SEEK_VALUE inclusive */
        public void onSeekUpdated(int newSeek);
    }

    public void registerOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        if(!onCompletionListenerList.contains(listener)) {
            onCompletionListenerList.add(listener);
        }
    }

    public void unregisterOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        onCompletionListenerList.remove(listener);
    }

    public void registerSeekListener(SeekListener listener) {
        if (seekListenerList.size() == 0 && (mediaPlayer != null) && mediaPlayer.isPlaying()) {
            startSeekMonitor();
        }
        if(!seekListenerList.contains(listener)) {
            seekListenerList.add(listener);
        }
    }

    public void unregisterSeekListener(SeekListener listener) {
        if(seekListenerList.size() == 1) {
            stopSeekMonitor();
        }
        seekListenerList.remove(listener);
    }

    public boolean playSong(Song song) {
        try {
            if (!prepared)
                prepareSong(song);
            if(!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                startSeekMonitor();
            }
        }
        catch (IOException e) {
            return false;
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopSeekMonitor();
                callSeekListeners();

                for(MediaPlayer.OnCompletionListener listener : onCompletionListenerList) {
                    listener.onCompletion(mediaPlayer);
                }
            }
        });

        return true;
    }

    public void pauseSong() {
        mediaPlayer.pause();
        stopSeekMonitor();
        callSeekListeners();
    }

    public void seek(int seek){
        if(mediaPlayer!=null && prepared) {
            int actualSeek = (int) ((double) seek / MAX_SEEK_VALUE * mediaPlayer.getDuration());
            mediaPlayer.seekTo(actualSeek);
        }
    }

    public void reset() {
        stopSeekMonitor();
        callSeekListeners();

        if(prepared) {
            mediaPlayer.reset();
            prepared = false;
        }
    }

    //TODO: use listener for remote song, or return a different value for "downloading" instead of boolean.
    //TODO: or, try wait() notify()
    private void prepareSong(final Song song) throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if(song.getFilePath().equals(Library.REMOTE_SONG_MISSING_PATH)) {
            ShareGroup.getSong(song.getLibraryUsername(), song.get_id(),
                    new ShareGroup.GetSongListener() {
                @Override
                public void onGotSong(String songFilePath) {
                    try {
                        mediaPlayer.setDataSource(context, Uri.parse(songFilePath));
                        mediaPlayer.prepare();
                        prepared = true;
                        mediaPlayer.start();
                    } catch (IOException e) {
                        //TODO: redo this logic
                    }
                    prepared = true;
                }

                @Override
                public void onFailedGettingSong() {
                    //TODO: redo this logic
                }
            });
        } else {
            mediaPlayer.setDataSource(context, Uri.parse(song.getFilePath()));
            mediaPlayer.prepare();
            prepared = true;
        }
    }

    private void callSeekListeners() {
        if(mediaPlayer != null) {
            int currentPos = mediaPlayer.getCurrentPosition();

            for (SeekListener listener : seekListenerList) {
                listener.onSeekUpdated(currentPos * MAX_SEEK_VALUE / mediaPlayer.getDuration());
            }
        }
    }

    private void startSeekMonitor() {
        monitorSeek = true;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(monitorSeek) {
                        callSeekListeners();
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            //Do nothing?
                        }
                    } else {
                        break;
                    }
                }
            }
        };

        handler.executeAsync(r,SEEK_UPDATER_THREAD_NAME, true);
    }


    public void stopSeekMonitor() {
        monitorSeek = false;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
}
