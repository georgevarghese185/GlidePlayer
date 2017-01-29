package teefourteen.glideplayer.music.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.provider.MediaStore;


public class AlbumTable extends Table {
    public static final String TABLE_NAME = "music_album";
    ContentResolver resolver;

    AlbumTable(ContentResolver resolver) {
        super(TABLE_NAME);
        this.resolver = resolver;
    }

    public class Columns implements BaseColumns {
        public static final String ALBUM_NAME = MediaStore.Audio.Albums.ALBUM;
        public static final String ALBUM_ART = MediaStore.Audio.Albums.ALBUM_ART;
        public static final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        public static final String NUMBER_OF_SONGS = MediaStore.Audio.Albums.NUMBER_OF_SONGS;
    }

    @Override
    String createTableQuery() {
        String query = "CREATE TABLE " + TABLE_NAME + "("
                + BaseColumns._ID + " INTEGER" + ", "
                + BaseColumns._COUNT + " INTEGER" + ", "
                + Columns.ALBUM_NAME + " TEXT" + ", "
                + Columns.ALBUM_ART + " TEXT" + ", "
                + Columns.ARTIST + " TEXT" + ", "
                + Columns.NUMBER_OF_SONGS + " INTEGER" + ")";

        return query;
    }

    @Override
    void initialize(SQLiteDatabase db) {
        db.delete(TABLE_NAME, null, null);

        Cursor cursor = getMediaStoreCursor();

        if (cursor != null) {
            cursor.moveToFirst();
            do {
                ContentValues values = putValues(cursor);

                insertValues(values, db);
            } while (cursor.moveToNext());

            cursor.close();
        }
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
        values.put(Columns.ALBUM_NAME, Library.getString(cursor, MediaStore.Audio.Albums.ALBUM));
        values.put(Columns.ALBUM_ART, Library.getString(cursor, MediaStore.Audio.Albums.ALBUM_ART));
        values.put(Columns.ARTIST, Library.getString(cursor, MediaStore.Audio.Albums.ARTIST));
        values.put(Columns.NUMBER_OF_SONGS, Library.getInt(cursor, MediaStore.Audio.Albums.NUMBER_OF_SONGS));

        return values;
    }
}
