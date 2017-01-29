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

    public void close() {
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
                + " ORDER BY " + SongTable.Columns.TITLE;


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

    public boolean sendOverStream(OutputStream out)throws JSONException {
        PrintWriter printWriter = new PrintWriter(out, true);
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

        }

        return true;
    }


    public boolean getFromStream(InputStream in)throws JSONException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
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
        return true;
    }
}
