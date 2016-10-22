package teefourteen.glideplayer.databases.library;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import teefourteen.glideplayer.databases.library.LibraryContract.SongTable;

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
                + SongTable.COLUMN_NAME_ALBUM + " TEXT,"
                + SongTable.COLUMN_NAME_ALBUM_ID + " INTEGER,"
                + SongTable.COLUMN_NAME_ALBUM_KEY + " TEXT,"
                + SongTable.COLUMN_NAME_ARTIST + " TEXT,"
                + SongTable.COLUMN_NAME_ARTIST_ID + " INTEGER,"
                + SongTable.COLUMN_NAME_ARTIST_KEY + " TEXT,"
                + SongTable.COLUMN_NAME_DATE_ADDED + " INTEGER,"
                + SongTable.COLUMN_NAME_DURATION + " INTEGER,"
                + SongTable.COLUMN_NAME_FILE_PATH + " TEXT,"
                + SongTable.COLUMN_NAME_BOOKMARK + " INTEGER,"
                + SongTable.COLUMN_NAME_SIZE + " INTEGER,"
                + SongTable.COLUMN_NAME_TITLE + " TEXT,"
                + SongTable.COLUMN_NAME_TITLE_KEY + " TEXT,"
                + SongTable.COLUMN_NAME_TRACK_NUMBER + " INTEGER,"
                + SongTable.COLUMN_NAME_YEAR + " INTEGER"
                + ")";

        db.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
