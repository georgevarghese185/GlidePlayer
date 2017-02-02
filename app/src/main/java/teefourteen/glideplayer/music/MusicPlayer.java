package teefourteen.glideplayer.music;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.music.database.Library;


public class MusicPlayer {
    private MediaPlayer mediaPlayer =null;
    private Context context=null;
    private boolean prepared = false;

    public MusicPlayer(Context context) {
        this.context = context;
    }

    public boolean playSong(Song song) {
        try {
            if (!prepared)
                prepareSong(song);
            if(!mediaPlayer.isPlaying())
                mediaPlayer.start();
        }
        catch (IOException e) {
            return false;
        }

        return true;
    }

    public void pauseSong() {
        mediaPlayer.pause();
    }

    public void reset() {
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

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
}
