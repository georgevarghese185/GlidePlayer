package teefourteen.glideplayer.music.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;


public class Library {
    public static final String LOCAL_DATABASE_NAME = "music_library.db";
    private File dbFile;
    public static String DATABASE_LOCATION;
    public static String REMOTE_COVERS_LOCATION;
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

    public boolean sendOverStream(InputStream in, OutputStream out)throws JSONException, IOException {
        PrintWriter printWriter = new PrintWriter(out, true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        JSONObject jsonObject;

        for(int i = 0; i < tables.length; i++) {
            Cursor cursor = tables[i].getFullTable(libraryDb);
            cursor.moveToFirst(); //TODO: if false do something

            //send number of rows
            printWriter.println(String.valueOf(cursor.getCount()));
            reader.readLine(); //wait for receiver

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

        //Send album art file

        //get album art
        Cursor cursor = libraryDb.query(true,
                AlbumTable.TABLE_NAME,
                new String[]{AlbumTable.Columns._ID, AlbumTable.Columns.ALBUM_ART},
                AlbumTable.Columns.ALBUM_ART + " IS NOT NULL", null,
                null, null, null, null);

        //send each file
        reader.readLine(); //wait for receiver
        for(boolean res = cursor.moveToFirst(); res; res = cursor.moveToNext()) {
            File file = new File(cursor.getString(
                    cursor.getColumnIndex(AlbumTable.Columns.ALBUM_ART)));

            if(!file.exists()) {
                continue;
            }

            printWriter.println("next_file");

            //send album_id, file name and size
            printWriter.println(cursor.getLong(cursor.getColumnIndex(AlbumTable.Columns._ID)));
            printWriter.println(file.getName());
            printWriter.println(file.length());

            FileInputStream  fin = new FileInputStream(file);
            byte[] buffer = new byte[8096];
            DataOutputStream dataOut = new DataOutputStream(out);
            int count;

            //send file
            reader.readLine(); //wait for receiver
            while ((count = fin.read(buffer, 0, buffer.length)) > 0) {
                dataOut.write(buffer, 0, count);
            }

            fin.close();

            //wait for receiver
            reader.readLine();
        }

        printWriter.println("end_of_files");

        cursor.close();

        return true;
    }


    public boolean getFromStream(InputStream in, OutputStream out)throws JSONException, IOException {
        //TODO: clear album art cache on disconnect/close app
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        PrintWriter printWriter = new PrintWriter(out, true);

        JSONObject jsonObject;

        libraryDb.beginTransaction();

        for(int i = 0; i < tables.length; i++) {

            printWriter.println("ready");
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

        ContentValues v = new ContentValues();
        v.put(AlbumTable.Columns.ALBUM_ART, "");
        libraryDb.update(AlbumTable.TABLE_NAME, v, null, null);



        //Get album art files
        File remoteCoversDir = new File(REMOTE_COVERS_LOCATION);
        if(!remoteCoversDir.exists()) {
            remoteCoversDir.mkdir();
        }
        File albumCoverDir = new File(remoteCoversDir, dbFile.getName());
        if(!albumCoverDir.exists()) {
            albumCoverDir.mkdir();
        }

        //get each file
        printWriter.println("ready"); //ready to receive
        while((reader.readLine()).equals("next_file")) {

            //get album_id, file name and size

            long album_id = Long.parseLong(reader.readLine());
            String filename = reader.readLine();
            int size = Integer.parseInt(reader.readLine());

            File file = new File(albumCoverDir.getAbsolutePath(), filename);
            if(file.exists()) {
                file.delete();
            }
            file.createNewFile();

            int count = 0;
            DataInputStream dataIn = new DataInputStream(in);
            FileOutputStream fileOut = new FileOutputStream(file);
            byte[] buffer = new byte[8096];
            int readBytes=0;

            printWriter.println("ready"); //ready to receive
            //get file

            for(;size > 0; size -= count) {
                count = dataIn.read(buffer,0,buffer.length);
                fileOut.write(buffer, 0, count);
            }

            fileOut.close();

            ContentValues values = new ContentValues();
            values.put(AlbumTable.Columns.ALBUM_ART, file.getAbsolutePath());
            libraryDb.execSQL("UPDATE " + AlbumTable.TABLE_NAME + " SET " + AlbumTable.Columns.ALBUM_ART + "="
                    +'"' + file.getAbsolutePath() +'"' + " WHERE " + AlbumTable.Columns._ID + "=" + album_id);

            Log.d("got album", "" + album_id);
            printWriter.println("ready"); //ready to receive
        }
        Log.d("got album", "finished");
        libraryDb.setTransactionSuccessful();
        libraryDb.endTransaction();

        Cursor cursor = libraryDb.query(false, AlbumTable.TABLE_NAME, new String[] { AlbumTable.Columns._ID,AlbumTable.Columns.ALBUM_NAME, AlbumTable.Columns.ALBUM_ART},
                null,null,null,null,null,null);

        cursor.close();
        return true;
    }
}
