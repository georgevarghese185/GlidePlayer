package com.teefourteen.glideplayer.music.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;


public class AlbumTable extends Table {
    public static final String localTableName = "album";
    public static final String remoteTableName = "remote_album";
    ContentResolver resolver;

    public class Columns implements BaseColumns, RemoteColumns {
        public static final String ALBUM_ID = "album_id";
        public static final String ALBUM_NAME = MediaStore.Audio.Albums.ALBUM;
        public static final String ALBUM_ART = MediaStore.Audio.Albums.ALBUM_ART;
        public static final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        public static final String NUMBER_OF_SONGS = MediaStore.Audio.Albums.NUMBER_OF_SONGS;
    }

    /** pass null for remote table */
    AlbumTable(ContentResolver resolver) {
        super((resolver == null) ? remoteTableName : localTableName);
        this.resolver = resolver;
    }

    @Override
    String createTableQuery() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + BaseColumns._ID + " INTEGER" + ", "
                + BaseColumns._COUNT + " INTEGER" + ", "
                + Columns.ALBUM_ID + " INTEGER" + ", "
                + Columns.ALBUM_NAME + " TEXT" + ", "
                + Columns.ALBUM_ART + " TEXT" + ", "
                + Columns.ARTIST + " TEXT" + ", "
                + Columns.NUMBER_OF_SONGS + " INTEGER" + ", "
                + Columns.IS_REMOTE + " INTEGER" + ", "
                + Columns.REMOTE_USERNAME + " TEXT" + ")";
    }

    @Override
    Cursor getMediaStoreCursor() {
        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS
        };

        return resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection,
                null, null, null);
    }

    @Override
    public ContentValues putValues(Cursor cursor) {
        ContentValues values = new ContentValues();

        values.put(Columns._ID, Library.getLong(cursor, MediaStore.Audio.Albums._ID));
        values.put(Columns.ALBUM_ID, Library.getLong(cursor, MediaStore.Audio.Albums._ID));
        values.put(Columns.ALBUM_NAME, Library.getString(cursor, MediaStore.Audio.Albums.ALBUM));
        values.put(Columns.ALBUM_ART, Library.getString(cursor, MediaStore.Audio.Albums.ALBUM_ART));
        values.put(Columns.ARTIST, Library.getString(cursor, MediaStore.Audio.Albums.ARTIST));
        values.put(Columns.NUMBER_OF_SONGS, Library.getInt(cursor, MediaStore.Audio.Albums.NUMBER_OF_SONGS));
        values.put(Columns.IS_REMOTE, 0);

        return values;
    }
}
