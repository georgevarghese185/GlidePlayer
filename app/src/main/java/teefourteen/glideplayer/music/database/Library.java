package teefourteen.glideplayer.music.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import teefourteen.glideplayer.music.Song;


public class Library {
    public static final String LOCAL_DATABASE_NAME = "music_library.db";
    public static final String REMOTE_SONG_MISSING_PATH = "@missing_remote_song@";
    public static String FILE_SAVE_LOCATION;
    public static String DATABASE_LOCATION;
    public static String REMOTE_DATABASE_LOCATION;
    public static String REMOTE_COVERS_LOCATION;

    private File dbFile;
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase libraryDb;
    private LibraryDbOpenHelper openHelper;
    private Table[] tables;
    private Context context;

    public Library(Context context, File databaseFile) {
        openHelper = new LibraryDbOpenHelper(context, databaseFile);
        dbFile = databaseFile;
        this.context = context;

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
        return new LibraryDbOpenHelper(context, dbFile).getReadableDatabase();
    }


    public void initializeTables() {
        libraryDb.beginTransaction();

        for(Table table : tables) {
            table.initialize(libraryDb);
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();
    }

    public void close() {
        libraryDb.close();
    }

    public static Cursor getSongs(SQLiteDatabase libraryDb) {
        return querySongs(libraryDb, null, null);
    }

    public static Cursor getSong(SQLiteDatabase libraryDb, long songId) {
        return querySongs(libraryDb,
                " WHERE " + SongTable.TABLE_NAME + "." + SongTable.Columns.SONG_ID + "=?"
                , new String[]{String.valueOf(songId)});
    }

    public static Cursor getAlbumSongs(long albumId, SQLiteDatabase libraryDb) {
        String selection = " WHERE " + SongTable.TABLE_NAME + "." + SongTable.Columns.ALBUM_ID + "=?";
        String[] selectionArgs = {String.valueOf(albumId)};

        return querySongs(libraryDb, selection, selectionArgs);
    }

    public static Cursor querySongs(SQLiteDatabase libraryDb, String selection,
                                    String[] selectionArgs) {
        String query = "SELECT * FROM "
                + SongTable.TABLE_NAME + " LEFT JOIN " + AlbumTable.TABLE_NAME
                + " ON " + SongTable.TABLE_NAME + "." + SongTable.Columns.ALBUM_ID + "=" + AlbumTable.TABLE_NAME + "." + AlbumTable.Columns.ALBUM_ID
                + " LEFT JOIN " + ArtistTable.TABLE_NAME
                + " ON " + SongTable.TABLE_NAME + "." + SongTable.Columns.ARTIST_ID + "=" + ArtistTable.TABLE_NAME + "." + ArtistTable.Columns.ARTIST_ID
                + ((selection==null)? "" : selection)
                + " ORDER BY " + SongTable.TABLE_NAME + "." + SongTable.Columns.TITLE;


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

    public boolean sendOverStream(InputStream in, OutputStream out)throws JSONException, IOException {
        PrintWriter printWriter = new PrintWriter(out, true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        JSONObject jsonObject;

        for(int i = 0; i < tables.length; i++) {
            Cursor cursor = tables[i].getFullTable(libraryDb);
            cursor.moveToFirst(); //TODO: if false do something

            //send number of rows
            printWriter.println(String.valueOf(cursor.getCount()));

            //send each row
            for(boolean res = cursor.moveToFirst(); res; res = cursor.moveToNext()) {
                jsonObject = new JSONObject();

                for(int j = 0; j < cursor.getColumnCount(); j++) {
                    String column = cursor.getColumnName(j);

                    if(cursor.getType(j) == Cursor.FIELD_TYPE_INTEGER) {
                        jsonObject.put(column, cursor.getLong(cursor.getColumnIndex(column)));
                    } else if(cursor.getType(j) == Cursor.FIELD_TYPE_STRING) {
                        jsonObject.put(column, cursor.getString(cursor.getColumnIndex(column)));
                    }
                }

                printWriter.println(jsonObject.toString());
            }

            cursor.close();
        }

        return true;
    }


    public boolean getFromStream(InputStream in, OutputStream out)throws JSONException, IOException {
        //TODO: clear album art cache on disconnect/close app
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        PrintWriter printWriter = new PrintWriter(out, true);

        JSONObject jsonObject;

        libraryDb.beginTransaction();

        for(int i = 0; i < tables.length; i++) {
            //get number of rows
            int rows = Integer.parseInt(reader.readLine());

            //get rows
            for(int j = 0; j<rows; j++) {
                jsonObject = new JSONObject(reader.readLine());
                ContentValues values =  new ContentValues();

                Iterator<String> names = jsonObject.keys();
                while(names.hasNext()) {
                    String column = names.next();
                    Object value = jsonObject.get(column);
                    if(value instanceof Long || value instanceof Integer) {
                        values.put(column, Long.parseLong(jsonObject.getString(column)));
                    } else if(value instanceof String) {
                        values.put(column, jsonObject.getString(column));
                    } else {
                        //TODO: something's wrong
                    }
                }

                //insert row
                tables[i].insertValues(values, libraryDb);
            }
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();



        libraryDb.beginTransaction();

        ContentValues v;

        v = new ContentValues();
        v.put(SongTable.Columns.FILE_PATH, REMOTE_SONG_MISSING_PATH);
        v.put(SongTable.Columns.IS_REMOTE, 1);
        v.put(SongTable.Columns.REMOTE_USERNAME, dbFile.getName());

        libraryDb.update(SongTable.TABLE_NAME,v, null, null);

        Cursor cursor = libraryDb.query(true, AlbumTable.TABLE_NAME,
                new String[]{AlbumTable.Columns.ALBUM_ID, AlbumTable.Columns.ALBUM_ART},
                null, null, null, null, null, null);

        for(boolean res = cursor.moveToFirst(); (res); res = cursor.moveToNext()) {
            long albumId = getLong(cursor,AlbumTable.Columns.ALBUM_ID);
            String albumArt = getString(cursor, AlbumTable.Columns.ALBUM_ART);
            if(albumArt != null) {
                String[] split = albumArt.split("/");

                albumArt = Library.REMOTE_COVERS_LOCATION + "/" + dbFile.getName()
                        + "/" + split[split.length - 1];

                v = new ContentValues();
                v.put(AlbumTable.Columns.ALBUM_ART, albumArt);

                libraryDb.update(AlbumTable.TABLE_NAME, v,
                        AlbumTable.Columns.ALBUM_ID + "=?", new String[]{String.valueOf(albumId)});
            }
        }

        cursor.close();

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();

        File remoteCoversLocation = new File(REMOTE_COVERS_LOCATION, dbFile.getName());
        if(!remoteCoversLocation.exists()) {
            remoteCoversLocation.mkdir();
        }

        return true;
    }

    public File getAlbumArt(long albumId) {
        Cursor cursor = libraryDb.query(true, AlbumTable.TABLE_NAME,
                new String[]{AlbumTable.Columns.ALBUM_ART},
                AlbumTable.Columns.ALBUM_ID + "=?",
                new String[]{String.valueOf(albumId)},
                null,null,null,null);

        String path;
        if(cursor.moveToFirst()) {
            path = getString(cursor, AlbumTable.Columns.ALBUM_ART);
        } else {
            path = null;
        }

        cursor.close();

        return (path == null) ? null : new File(path);
    }
}
