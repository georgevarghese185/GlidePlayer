package com.teefourteen.glideplayer.database;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

public class VideoTable extends Table{
    public static final String localTableName = "video";
    public static final String remoteTableName = "remote_video";
    ContentResolver resolver;

    public class Columns implements BaseColumns, RemoteColumns {
        public static final String VIDEO_ID = "song_id";
        public static final String DATE_ADDED = MediaStore.Audio.Media.DATE_ADDED;
        public static final String DATE_MODIFIED = MediaStore.Audio.Media.DATE_MODIFIED;
        public static final String DURATION = MediaStore.Audio.Media.DURATION;
        public static final String FILE_PATH = MediaStore.Audio.Media.DATA;
        public static final String HEIGHT = MediaStore.Video.Media.HEIGHT;
        public static final String SIZE = MediaStore.Audio.Media.SIZE;
        public static final String TITLE = MediaStore.Audio.Media.TITLE;
        public static final String WIDTH = MediaStore.Video.Media.WIDTH;
    }

    /** pass null for remote table */
    VideoTable(ContentResolver resolver) {
        super((resolver == null) ? remoteTableName : localTableName);
        this.resolver = resolver;
    }

    @Override
    String createTableQuery() {
        return "CREATE TABLE " + TABLE_NAME + "("
                + BaseColumns._ID + " INTEGER" + ", "
                + BaseColumns._COUNT + " INTEGER" + ", "
                + Columns.VIDEO_ID + " INTEGER" + ", "
                + Columns.DATE_ADDED + " INTEGER" + ", "
                + Columns.DATE_MODIFIED + " INTEGER" + ", "
                + Columns.DURATION + " INTEGER" + ", "
                + Columns.FILE_PATH + " TEXT" + ", "
                + Columns.HEIGHT + " INTEGER" + ", "
                + Columns.SIZE + " INTEGER" + ", "
                + Columns.TITLE + " TEXT" + ", "
                + Columns.WIDTH + " INTEGER" + ")";
    }

    @Override
    Cursor getMediaStoreCursor() {
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.WIDTH
        };

        return resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                null, null, null);
    }

    @Override
    ContentValues putValues(Cursor cursor) {
        ContentValues values = new ContentValues();

        values.put(Columns.VIDEO_ID, Library.getLong(cursor, Columns.VIDEO_ID));
        values.put(Columns.DATE_ADDED, Library.getLong(cursor, Columns.DATE_ADDED));
        values.put(Columns.DATE_MODIFIED, Library.getLong(cursor, Columns.DATE_MODIFIED));
        values.put(Columns.DURATION, Library.getLong(cursor, Columns.DURATION));
        values.put(Columns.FILE_PATH, Library.getString(cursor, Columns.FILE_PATH));
        values.put(Columns.HEIGHT, Library.getInt(cursor, Columns.HEIGHT));
        values.put(Columns.SIZE, Library.getLong(cursor, Columns.SIZE));
        values.put(Columns.TITLE, Library.getString(cursor, Columns.TITLE));
        values.put(Columns.WIDTH, Library.getInt(cursor, Columns.WIDTH));

        return values;
    }
}
