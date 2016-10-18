package teefourteen.distroplayer.music;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MusicService extends Service {
    public enum Command {
        PLAY_SONG,
        PLAY_NEXT,
        PLAY_PREV,
        PLAY_NEW_QUEUE,
        PAUSE,
        RESUME
    }

    public MusicService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
