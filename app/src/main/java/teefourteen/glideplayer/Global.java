package teefourteen.glideplayer;

import android.database.Cursor;

import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.music.PlayQueue;

/**
 * Created by George on 11/12/2016.
 */

public class Global {
    public static PlayQueue playQueue = null;
    public static final String SHARED_PREFS_NAME = "GlidePlayerPrefsFile";

    private Global(){}
}
