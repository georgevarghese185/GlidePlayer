package com.teefourteen.glideplayer.music;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.widget.Toast;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.connectivity.RemoteFileCache;
import com.teefourteen.glideplayer.database.Library;


public class MusicPlayer implements Closeable{
    public static final int MAX_SEEK_VALUE = 2000;
    public static final int SEEK_INTERVAL_MS = 300;

    private MediaPlayer mediaPlayer =null;
    private Context context=null;
    private boolean prepared = false;
    private int bufferedPercent;
    private int bufferedTime;
    private boolean pausedForBuffering = false;
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
        void onSeekUpdated(int newSeek);
        void onBufferingUpdated(int percent);
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
        seekListenerList.remove(listener);
    }

    private void play() {
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.start();
        startSeekMonitor();
    }

    public boolean playSong(Song song) {
        try {
            if (!prepared) {
                prepareSong(song);
                if(prepared) play();
                else return false;
            }
            else if(!mediaPlayer.isPlaying()) {
                play();
            }
        }
        catch (IOException e) {
            return false;
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                monitorSeek = false;
                updateSeek();

                for(MediaPlayer.OnCompletionListener listener : onCompletionListenerList) {
                    listener.onCompletion(mediaPlayer);
                }
            }
        });

        return true;
    }

    public void pauseSong() {
        mediaPlayer.pause();
        monitorSeek = false;
        updateSeek();
    }

    public void seek(int seek){
        if(mediaPlayer!=null && prepared) {
            int actualSeek = (int) ((double) seek / MAX_SEEK_VALUE * mediaPlayer.getDuration());
            trueSeek(actualSeek);
        }
    }

    public void trueSeek(int seek) {
        if(mediaPlayer != null && prepared) {
            if(seek > bufferedTime) {
                seek = ((bufferedTime - 1000) > 0)? bufferedTime - 1000 : 0;
            }
            mediaPlayer.seekTo(seek);
        }
    }

    public int getSeek() {
        if(prepared) {
            return mediaPlayer.getCurrentPosition() * MAX_SEEK_VALUE / mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public int getTrueSeek() {
        if(mediaPlayer != null && prepared) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void reset() {
        monitorSeek = false;
        updateSeek();

        if(prepared) {
            mediaPlayer.release();
            mediaPlayer = null;
            prepared = false;
        }
    }

    public boolean prepareSong(final Song song) throws IOException {
        if(mediaPlayer!=null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if(song.getFilePath().equals(Library.REMOTE_SONG_MISSING_PATH)) {
            try {
                mediaPlayer.setDataSource(context, RemoteFileCache.getInstance().getSongUri(song));
                //TODO: temporarily commented till buffering fixed.
//                mediaPlayer.setOnBufferingUpdateListener(
//                        new MediaPlayer.OnBufferingUpdateListener() {
//                    @Override
//                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                        bufferedPercent = percent;
//                        bufferedTime = (int)(song.getDuration() * percent/100.0);
//                        for(SeekListener listener : seekListenerList) {
//                            listener.onBufferingUpdated(
//                                    (int)(bufferedTime * MAX_SEEK_VALUE / song.getDuration()));
//                        }
//                    }
//                });

                //TODO: temp code
                bufferedPercent = 100;
                bufferedTime = (int)song.getDuration();
                //TODO: end of temp code

                mediaPlayer.prepare();
                prepared = true;
                return true;
            } catch (RemoteFileCache.BiggerThanCacheException e) {
                Toast.makeText(context, "File too big. Please increase cache size " +
                        "in Settings", Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            mediaPlayer.setDataSource(context, Uri.parse(song.getFilePath()));
            bufferedPercent = 100;
            bufferedTime = (int)song.getDuration();
            mediaPlayer.prepare();
            prepared = true;
            return true;
        }
    }

    private void updateSeek() {
        if(mediaPlayer != null && prepared) {
            int currentPos = mediaPlayer.getCurrentPosition();

            if((currentPos + SEEK_INTERVAL_MS) > bufferedTime) {
                mediaPlayer.pause();
                pausedForBuffering = true;
            } else if(pausedForBuffering) {
                mediaPlayer.start();
                pausedForBuffering = false;
            }

            if(bufferedPercent == 100 && seekListenerList.size() == 0) {
                monitorSeek = false;
            } else {
                for (SeekListener listener : seekListenerList) {
                    listener.onSeekUpdated(currentPos * MAX_SEEK_VALUE / mediaPlayer.getDuration());
                }
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
                        updateSeek();
                        try {
                            Thread.sleep(SEEK_INTERVAL_MS);
                        } catch (InterruptedException e) {
                            //Do nothing?
                        }
                    } else {
                        break;
                    }
                }
            }
        };

        handler.executeAsync(r,SEEK_UPDATER_THREAD_NAME);
    }

    public boolean isPlaying() {
        if(mediaPlayer == null) {
            return false;
        } else {
            return mediaPlayer.isPlaying();
        }
    }

    @Override
    public void close() {
        handler.closeAllHandlers();
        if(mediaPlayer!=null && prepared) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}
