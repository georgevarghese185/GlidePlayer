package teefourteen.glideplayer.databases.library;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static teefourteen.glideplayer.activities.MainActivity.libraryDb;

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

    public static Cursor getAlbumSongs(long albumId) {
        return libraryDb.query(false, SongTable.TABLE_NAME,
                new String[]{SongTable._ID, SongTable.FILE_PATH, SongTable.TITLE, SongTable.ALBUM,
                        SongTable.ALBUM_ID, SongTable.ARTIST, SongTable.ARTIST_ID,
                        SongTable.DURATION}, SongTable.ALBUM_ID+"=?",
                new String[]{String.valueOf(albumId)},
                null, null, SongTable.TRACK_NUMBER, null);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
