package teefourteen.glideplayer.music.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;


public class Library {
    public static final String DATABASE_NAME = "music_library.db";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase libraryDb;
    private LibraryDbOpenHelper openHelper;
    private Table[] tables;

    public Library(Context context, File databaseFile) {
        openHelper = new LibraryDbOpenHelper(context, databaseFile);

        tables = new Table[]{
                new SongTable(context.getContentResolver()),
                new AlbumTable(context.getContentResolver()),
                new ArtistTable(context.getContentResolver())};

        libraryDb = openHelper.getWritableDatabase();
    }


    private class LibraryDbOpenHelper extends SQLiteOpenHelper {

        LibraryDbOpenHelper(Context context, File database) {
            super(context, database.getAbsolutePath(), null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for(Table table : tables) {
                db.execSQL(table.createTableQuery());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }


    public SQLiteDatabase getReadableDatabase() {
        return openHelper.getReadableDatabase();
    }


    public void initializeTables() {
        libraryDb.beginTransaction();

        for(Table table : tables) {
            table.initialize(libraryDb);
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();
    }

    public void doneUpdating() {
        libraryDb.close();
    }

    public static Cursor getSongs(SQLiteDatabase libraryDb) {
        return querySongs(libraryDb, null, null);
    }

    public static Cursor getAlbumSongs(long albumId, SQLiteDatabase libraryDb) {
        String selection = "WHERE song." + SongTable.Columns.ALBUM_ID + "=?";
        String[] selectionArgs = {String.valueOf(albumId)};

        return querySongs(libraryDb, selection, selectionArgs);
    }

    public static Cursor querySongs(SQLiteDatabase libraryDb, String selection,
                                    String[] selectionArgs) {
        String query = "SELECT * FROM "
                + SongTable.TABLE_NAME + " song LEFT JOIN " + AlbumTable.TABLE_NAME + " album"
                + " ON " + "song." + SongTable.Columns.ALBUM_ID + "=" + "album." + AlbumTable.Columns._ID
                + " LEFT JOIN " + ArtistTable.TABLE_NAME + " artist"
                + " ON " + "song." + SongTable.Columns.ARTIST_ID + "=" + "artist." + ArtistTable.Columns._ID
                + ((selection==null)? "" : selection)
                + " ORDER BY " + AlbumTable.Columns.ALBUM_NAME;


        return libraryDb.rawQuery(query,selectionArgs);
    }

    public static Cursor getAlbums(SQLiteDatabase libraryDb) {
        String query = "SELECT * FROM " + AlbumTable.TABLE_NAME;

        return libraryDb.rawQuery(query,null);
    }

    public static long getLong(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndex(column));
    }

    public static String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndex(column));
    }

    public static int getInt(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndex(column));
    }
}
