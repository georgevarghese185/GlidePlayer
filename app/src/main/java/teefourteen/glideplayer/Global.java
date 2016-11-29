package teefourteen.glideplayer;

import android.database.Cursor;

import teefourteen.glideplayer.music.PlayQueue;

/**
 * Created by George on 11/12/2016.
 */

public class Global {
    public static Cursor songCursor = null;
    public static PlayQueue playQueue = null;
    public static Connectivity connectivity;

    private Global(){}
}
