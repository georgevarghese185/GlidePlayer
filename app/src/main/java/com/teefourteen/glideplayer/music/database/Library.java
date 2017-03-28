package com.teefourteen.glideplayer.music.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

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
    public static final String LOCAL_DATABASE_NAME = "music_library.db";
    public static final String REMOTE_SONG_MISSING_PATH = "@missing_remote_song@";
    public static String FILE_SAVE_LOCATION;
    public static String DATABASE_LOCATION;
    public static String REMOTE_COVERS_LOCATION;
    private static Library library;

    private File dbFile;
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase readableDb;
    private LibraryDbOpenHelper openHelper;
    private Table[] tables;
    private Table[] remoteTables;
    private Context context;

    public static void initialize(Context context) {
        library = new Library(context);
        library.initializeLocalLibrary();
        library.readableDb = library.openHelper.getReadableDatabase();
    }

    private Library(Context context) {
        File databaseFile = new File(DATABASE_LOCATION, LOCAL_DATABASE_NAME);
        openHelper = new LibraryDbOpenHelper(context, databaseFile);
        dbFile = databaseFile;
        this.context = context;

        //fill both Table arrays with same Table types and in the same order! This is used by the library get and send methods.
        tables = new Table[]{
                new SongTable(context.getContentResolver()),
                new AlbumTable(context.getContentResolver()),
                new ArtistTable(context.getContentResolver())
        };

        remoteTables = new Table[] {
                new SongTable(null),
                new AlbumTable(null),
                new ArtistTable(null)
        };
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

            for(Table table : remoteTables) {
                db.execSQL(table.createTableQuery());
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private void initializeLocalLibrary() {
        SQLiteDatabase libraryDb = openHelper.getWritableDatabase();

        libraryDb.beginTransaction();

        for(Table table : tables) {
            table.initialize(libraryDb);
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();
    }

    private SQLiteDatabase getReadableDb() {
        if(readableDb.isOpen()) {
            return readableDb;
        } else {
            readableDb = openHelper.getReadableDatabase();
            return readableDb;
        }
    }

    public static Cursor getSongs(String userName) { return querySongs(userName, null, null); }

    public static Cursor getSong(String userName ,long songId) {
        return querySongs(userName,
                " WHERE " + SongTable.localTableName + "." + SongTable.Columns.SONG_ID + "=?"
                , new String[]{String.valueOf(songId)});
    }

    public static Cursor getAlbumSongs(String userName ,long albumId) {
        String selection = " WHERE "
                + ((userName == null) ? SongTable.localTableName : SongTable.remoteTableName)
                + "." + SongTable.Columns.ALBUM_ID + "=?";
        String[] selectionArgs = {String.valueOf(albumId)};

        return querySongs(userName, selection, selectionArgs);
    }

    public static Cursor querySongs(String userName, String selection, String[] selectionArgs) {
        String songTable;
        String albumTable;
        String artistTable;

        if(userName != null) {
            songTable = SongTable.remoteTableName;
            albumTable = AlbumTable.remoteTableName;
            artistTable = ArtistTable.remoteTableName;

            if(selection == null) {
                selection = " WHERE ";
            } else {
                selection = selection + " AND ";
            }

            selection = selection + songTable + "." + SongTable.Columns.REMOTE_USERNAME + "=?"
                    + " AND " + albumTable + "." + AlbumTable.Columns.REMOTE_USERNAME  + "=?"
                    + " AND " + artistTable + "." + ArtistTable.Columns.REMOTE_USERNAME  + "=?";

            String newArgs[];
            if(selectionArgs != null) {
                newArgs = new String[selectionArgs.length + 3];
                System.arraycopy(selectionArgs, 0, newArgs, 0, selectionArgs.length);
                System.arraycopy(new String[]{userName, userName, userName}, 0, newArgs,
                        selectionArgs.length, 3);
            } else {
                newArgs = new String[]{userName, userName, userName};
            }

            selectionArgs = newArgs;
        } else {
            songTable = SongTable.localTableName;
            albumTable = AlbumTable.localTableName;
            artistTable = ArtistTable.localTableName;
        }

        String query = "SELECT * FROM "
                + songTable + " LEFT JOIN " + albumTable
                + " ON " + songTable + "." + SongTable.Columns.ALBUM_ID + "=" + albumTable + "." + AlbumTable.Columns.ALBUM_ID
                + " LEFT JOIN " + artistTable
                + " ON " + songTable + "." + SongTable.Columns.ARTIST_ID + "=" + artistTable + "." + ArtistTable.Columns.ARTIST_ID
                + ((selection==null)? "" : selection)
                + " ORDER BY " + songTable + "." + SongTable.Columns.TITLE;


        return library.getReadableDb().rawQuery(query,selectionArgs);
    }

    public static Cursor getAlbums(String userName) {
        String albumTable;
        String selection = null;
        String selectionArgs[] = null;
        if(userName != null) {
            albumTable = AlbumTable.remoteTableName;
            selection = " WHERE " + AlbumTable.Columns.IS_REMOTE + "=?"
                    + " AND " + AlbumTable.Columns.REMOTE_USERNAME + "=?";
            selectionArgs = new String[]{"1", userName};
        } else {
            albumTable = AlbumTable.localTableName;
        }
        
        String query = "SELECT * FROM " + albumTable
                + ((selection == null)? "" : selection)
                + " ORDER BY " + AlbumTable.Columns.ALBUM_NAME;

        return library.getReadableDb().rawQuery(query, selectionArgs);
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

    public static boolean sendOverStream(OutputStream out)throws JSONException, IOException {
        SQLiteDatabase libraryDb = library.openHelper.getReadableDatabase();

        PrintWriter printWriter = new PrintWriter(out, true);

        JSONObject jsonObject;

        for(int i = 0; i < library.tables.length; i++) {
            Cursor cursor = library.tables[i].getFullTable(libraryDb);
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


    public static boolean getFromStream(InputStream in, String userName)throws JSONException, IOException {
        SQLiteDatabase libraryDb = library.openHelper.getWritableDatabase();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        JSONObject jsonObject;

        libraryDb.beginTransaction();

        for(int i = 0; i < library.remoteTables.length; i++) {
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

                    if(column.equals(BaseColumns._ID) || column.equals(BaseColumns._COUNT)) {
                        continue;
                    } else if(column.equals(Table.RemoteColumns.IS_REMOTE)) {
                        values.put(Table.RemoteColumns.IS_REMOTE, 1);
                    } else if(value instanceof Long || value instanceof Integer) {
                        values.put(column, Long.parseLong(jsonObject.getString(column)));
                    } else if(value instanceof String) {
                        values.put(column, jsonObject.getString(column));
                    }
                }
                values.put(Table.RemoteColumns.REMOTE_USERNAME, userName);
                //insert row
                library.remoteTables[i].insertValues(values, libraryDb);
            }
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();

        libraryDb.beginTransaction();

        ContentValues v;

        //set userName
        v = new ContentValues();
        v.put(SongTable.Columns.FILE_PATH, REMOTE_SONG_MISSING_PATH);

        libraryDb.update(SongTable.remoteTableName,v,
                SongTable.Columns.REMOTE_USERNAME + "=?", new String[]{userName});

        Cursor cursor = libraryDb.query(true, AlbumTable.remoteTableName,
                new String[]{AlbumTable.Columns.ALBUM_ID, AlbumTable.Columns.ALBUM_ART},
                AlbumTable.Columns.REMOTE_USERNAME + "=?", new String[]{userName},
                null, null, null, null);

        for(boolean res = cursor.moveToFirst(); (res); res = cursor.moveToNext()) {
            long albumId = getLong(cursor,AlbumTable.Columns.ALBUM_ID);
            String albumArt = getString(cursor, AlbumTable.Columns.ALBUM_ART);
            if(albumArt != null) {
                String[] split = albumArt.split("/");

                albumArt = Library.REMOTE_COVERS_LOCATION + "/" + userName
                        + "/" + split[split.length - 1];

                v = new ContentValues();
                v.put(AlbumTable.Columns.ALBUM_ART, albumArt);

                libraryDb.update(AlbumTable.remoteTableName, v,
                        AlbumTable.Columns.REMOTE_USERNAME + "=?"
                                + " AND " + AlbumTable.Columns.ALBUM_ID + "=?",
                        new String[]{userName, String.valueOf(albumId)});
            }
        }

        cursor.close();

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();

        File remoteCoversLocation = new File(REMOTE_COVERS_LOCATION, userName);
        if(!remoteCoversLocation.exists()) {
            remoteCoversLocation.mkdir();
        }

        return true;
    }

    public static File getAlbumArt(long albumId) {

        Cursor cursor = library.getReadableDb().query(true, AlbumTable.localTableName,
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

    public static void deleteUser(String username) {
        SQLiteDatabase libraryDb = library.openHelper.getWritableDatabase();
        libraryDb.beginTransaction();

        for(Table remoteTable : library.remoteTables) {
            libraryDb.delete(remoteTable.TABLE_NAME,
                    Table.RemoteColumns.REMOTE_USERNAME + "=?",
                    new String[]{username});
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();
    }

    public static void clearRemoteTables() {
        SQLiteDatabase libraryDb = library.openHelper.getWritableDatabase();
        libraryDb.beginTransaction();

        for(Table remoteTable : library.remoteTables) {
            libraryDb.delete(remoteTable.TABLE_NAME, null, null);
        }

        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();
    }
}
