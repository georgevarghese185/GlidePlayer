package com.teefourteen.glideplayer.music.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;


public class SongTable extends Table {
    public static final String localTableName = "song";
    public static final String remoteTableName = "remote_song";
    ContentResolver resolver;

    public class Columns implements BaseColumns, RemoteColumns {
        public static final String SONG_ID = "song_id";
        public static final String ALBUM_ID = MediaStore.Audio.Media.ALBUM_ID;
        public static final String ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
        public static final String BOOKMARK = MediaStore.Audio.Media.BOOKMARK;
        public static final String DATE_ADDED = MediaStore.Audio.Media.DATE_ADDED;
        public static final String DATE_MODIFIED = MediaStore.Audio.Media.DATE_MODIFIED;
        public static final String DURATION = MediaStore.Audio.Media.DURATION;
        public static final String FILE_PATH = MediaStore.Audio.Media.DATA;
        public static final String SIZE = MediaStore.Audio.Media.SIZE;
        public static final String TITLE = MediaStore.Audio.Media.TITLE;
        public static final String TRACK = MediaStore.Audio.Media.TRACK;
        public static final String YEAR = MediaStore.Audio.Media.YEAR;
    }

    /** pass null for remote table */
    SongTable(ContentResolver resolver) {
        super((resolver == null) ? remoteTableName : localTableName);
        this.resolver = resolver;
    }

    @Override
    String createTableQuery() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + BaseColumns._ID + " INTEGER" + ", "
                + BaseColumns._COUNT + " INTEGER" + ", "
                + Columns.SONG_ID + " INTEGER" + ", "
                + Columns.ALBUM_ID + " INTEGER" + ", "
                + Columns.ARTIST_ID + " INTEGER" + ", "
                + Columns.BOOKMARK + " INTEGER" + ", "
                + Columns.DATE_ADDED + " INTEGER" + ", "
                + Columns.DATE_MODIFIED + " INTEGER" + ", "
                + Columns.DURATION + " INTEGER" + ", "
                + Columns.FILE_PATH + " TEXT" + ", "
                + Columns.SIZE + " INTEGER" + ", "
                + Columns.TITLE + " TEXT" + ", "
                + Columns.TRACK + " INTEGER" + ", "
                + Columns.YEAR + " INTEGER" + ", "
                + Columns.IS_REMOTE + " INTEGER" + ", "
                + Columns.REMOTE_USERNAME + " TEXT" + ")";
    }

    @Override
    Cursor getMediaStoreCursor() {
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.BOOKMARK,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR
        };

        return resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                "IS_MUSIC != 0", null, null);
    }

    public ContentValues putValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put(Columns._ID, Library.getLong(cursor, MediaStore.Audio.Media._ID));
        values.put(Columns.SONG_ID, Library.getLong(cursor, MediaStore.Audio.Media._ID));
        values.put(Columns.ALBUM_ID, Library.getLong(cursor, MediaStore.Audio.Media.ALBUM_ID));
        values.put(Columns.ARTIST_ID, Library.getLong(cursor, MediaStore.Audio.Media.ARTIST_ID));
        values.put(Columns.BOOKMARK, Library.getLong(cursor, MediaStore.Audio.Media.BOOKMARK));
        values.put(Columns.DATE_ADDED, Library.getLong(cursor, MediaStore.Audio.Media.DATE_ADDED));
        values.put(Columns.DATE_MODIFIED, Library.getLong(cursor, MediaStore.Audio.Media.DATE_MODIFIED));
        values.put(Columns.DURATION, Library.getLong(cursor, MediaStore.Audio.Media.DURATION));
        values.put(Columns.FILE_PATH, Library.getString(cursor, MediaStore.Audio.Media.DATA));
        values.put(Columns.SIZE, Library.getLong(cursor, MediaStore.Audio.Media.SIZE));
        values.put(Columns.TITLE, Library.getString(cursor, MediaStore.Audio.Media.TITLE));
        values.put(Columns.TRACK, Library.getInt(cursor, MediaStore.Audio.Media.TRACK));
        values.put(Columns.YEAR, Library.getInt(cursor, MediaStore.Audio.Media.YEAR));
        values.put(Columns.IS_REMOTE, 0);

        return values;
    }
}
