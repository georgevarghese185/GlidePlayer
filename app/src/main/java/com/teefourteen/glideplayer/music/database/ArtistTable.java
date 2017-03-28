package com.teefourteen.glideplayer.music.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

public class ArtistTable extends Table {
    public static final String localTableName = "artist";
    public static final String remoteTableName = "remote_artist";
    ContentResolver resolver;

    public static class Columns implements BaseColumns, RemoteColumns {
        public static final String ARTIST_ID = "artist_id";
        public static final String ARTIST_NAME = MediaStore.Audio.Artists.ARTIST;
        public static final String NUMBER_OF_ALBUMS = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS;
        public static final String NUMBER_OF_TRACKS = MediaStore.Audio.Artists.NUMBER_OF_TRACKS;
    }

    /** pass null for remote table */
    ArtistTable(ContentResolver resolver) {
        super((resolver == null) ? remoteTableName : localTableName);
        this.resolver = resolver;
    }

    @Override
    public String createTableQuery() {
        return  "CREATE TABLE " + TABLE_NAME + "("
                + BaseColumns._ID + " INTEGER" + ", "
                + BaseColumns._COUNT + " INTEGER" + ", "
                + Columns.ARTIST_ID + " INTEGER" + ", "
                + Columns.ARTIST_NAME + " TEXT" + ", "
                + Columns.NUMBER_OF_ALBUMS + " INTEGER" + ", "
                + Columns.NUMBER_OF_TRACKS + " INTEGER" + ", "
                + Columns.IS_REMOTE + " INTEGER" + ", "
                + Columns.REMOTE_USERNAME + " TEXT" + ")";
    }

    @Override
    Cursor getMediaStoreCursor() {
        String[] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        };

        return resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, projection,
                null, null, null);
    }

    @Override
    public ContentValues putValues(Cursor cursor) {
        ContentValues values = new ContentValues();

        values.put(Columns._ID, Library.getLong(cursor, MediaStore.Audio.Artists._ID));
        values.put(Columns.ARTIST_ID, Library.getLong(cursor, MediaStore.Audio.Artists._ID));
        values.put(Columns.ARTIST_NAME, Library.getString(cursor, MediaStore.Audio.Artists.ARTIST));
        values.put(Columns.NUMBER_OF_ALBUMS, Library.getInt(cursor, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
        values.put(Columns.NUMBER_OF_TRACKS, Library.getInt(cursor, MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
        values.put(Columns.IS_REMOTE, 0);

        return values;
    }
}
