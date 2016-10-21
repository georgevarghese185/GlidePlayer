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
    private MediaPlayer player=null;
    private Context context=null;
    private boolean prepared = false;

    public MusicPlayer(Context context) {
        this.context = context;
    }

    public boolean playSong(Song song) {
        try {
            if (!prepared)
                prepareSong(song);
            if(!player.isPlaying())
                player.start();
        }
        catch (IOException e) {
            return false;
        }

        return true;
    }

    public void pauseSong() {
        player.pause();
    }

    public void reset() {
        if(prepared) {
            player.reset();
            prepared = false;
        }
    }

    private void prepareSong(Song song) throws IOException {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setDataSource(context, Uri.parse(song.getFilePath()));
        player.prepare();
        prepared = true;
    }
}
