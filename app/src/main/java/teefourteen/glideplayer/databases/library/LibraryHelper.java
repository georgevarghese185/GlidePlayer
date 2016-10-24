package teefourteen.glideplayer.databases.library;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by george on 21/10/16.
 */

public class LibraryHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "music-library.db";

    public LibraryHelper(Context context) {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createQuery = "CREATE TABLE " + SongTable.TABLE_NAME + " ("
                + SongTable._ID + " INTEGER PRIMARY KEY,"
                + SongTable.ALBUM + " TEXT,"
                + SongTable.ALBUM_ID + " INTEGER,"
                + SongTable.ALBUM_KEY + " TEXT,"
                + SongTable.ARTIST + " TEXT,"
                + SongTable.ARTIST_ID + " INTEGER,"
                + SongTable.ARTIST_KEY + " TEXT,"
                + SongTable.DATE_ADDED + " INTEGER,"
                + SongTable.DURATION + " INTEGER,"
                + SongTable.FILE_PATH + " TEXT,"
                + SongTable.BOOKMARK + " INTEGER,"
                + SongTable.SIZE + " INTEGER,"
                + SongTable.TITLE + " TEXT,"
                + SongTable.TITLE_KEY + " TEXT,"
                + SongTable.TRACK_NUMBER + " INTEGER,"
                + SongTable.YEAR + " INTEGER"
                + ")";

        db.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
