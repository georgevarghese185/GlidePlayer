package teefourteen.glideplayer.music;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

/**
 * Created by george on 17/10/16.
 */

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

    private void prepareSong(Song song) throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setDataSource(context, Uri.parse(song.getFilePath()));
        mediaPlayer.prepare();
        prepared = true;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
}
