package com.teefourteen.glideplayer.music;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.connectivity.ShareGroup;
import com.teefourteen.glideplayer.music.database.Library;


public class MusicPlayer implements Closeable{
    public static final int MAX_SEEK_VALUE = 2000;
    public static final int SEEK_INTERVAL_MS = 300;

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
        void onSeekUpdated(int newSeek);
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

    public void trueSeek(int seek) {
        if(mediaPlayer != null && prepared) {
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
        stopSeekMonitor();
        callSeekListeners();

        if(prepared) {
            mediaPlayer.release();
            mediaPlayer = null;
            prepared = false;
        }
    }

    //TODO: use listener for remote song, or return a different value for "downloading" instead of boolean.
    //TODO: or, try wait() notify()
    public boolean prepareSong(final Song song) throws IOException {
        if(mediaPlayer!=null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        if(song.getFilePath().equals(Library.REMOTE_SONG_MISSING_PATH)) {
            final boolean[] fetched = {false};

            ShareGroup.getSong(song.getLibraryUsername(), song.get_id(),
                    new ShareGroup.GetSongListener() {
                @Override
                public void onGotSong(String songFilePath) {
                    try {
                        fetched[0] = true;
                        mediaPlayer.setDataSource(context, Uri.parse(songFilePath));
                        mediaPlayer.prepare();
                        prepared = true;
                        countDownLatch.countDown();
                    } catch (IOException e) {
                        prepared = false;
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onFailedGettingSong() {
                    countDownLatch.countDown();
                }
            });

            try {
                countDownLatch.await();
                if(fetched[0] && prepared) {
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException e) {
                return false;
            }
        } else {
            mediaPlayer.setDataSource(context, Uri.parse(song.getFilePath()));
            mediaPlayer.prepare();
            prepared = true;
            return true;
        }
    }

    private void callSeekListeners() {
        if(mediaPlayer != null && prepared) {
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


    public void stopSeekMonitor() {
        monitorSeek = false;
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
