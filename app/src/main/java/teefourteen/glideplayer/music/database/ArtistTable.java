package teefourteen.glideplayer.music.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.provider.MediaStore;

public class ArtistTable extends Table {
    public static final String TABLE_NAME = "music_artist";
    ContentResolver resolver;

    public static class Columns implements BaseColumns {
        public static final String ARTIST_NAME = MediaStore.Audio.Artists.ARTIST;
        public static final String NUMBER_OF_ALBUMS = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS;
        public static final String NUMBER_OF_TRACKS = MediaStore.Audio.Artists.NUMBER_OF_TRACKS;
    }

    ArtistTable(ContentResolver resolver) {
        super(TABLE_NAME);
        this.resolver = resolver;
    }

    @Override
    public String createTableQuery() {
        String query = "CREATE TABLE " + TABLE_NAME + "("
                + BaseColumns._ID + " INTEGER" + ", "
                + BaseColumns._COUNT + " INTEGER" + ", "
                + Columns.ARTIST_NAME + " TEXT" + ", "
                + Columns.NUMBER_OF_ALBUMS + " INTEGER" + ", "
                + Columns.NUMBER_OF_TRACKS + " INTEGER" + ")";

        return query;
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
    public void initialize(SQLiteDatabase db) {
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
    public ContentValues putValues(Cursor cursor) {
        ContentValues values = new ContentValues();

        values.put(Columns._ID, Library.getLong(cursor, MediaStore.Audio.Artists._ID));
        values.put(Columns.ARTIST_NAME, Library.getString(cursor, MediaStore.Audio.Artists.ARTIST));
        values.put(Columns.NUMBER_OF_ALBUMS, Library.getInt(cursor, MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
        values.put(Columns.NUMBER_OF_TRACKS, Library.getInt(cursor, MediaStore.Audio.Artists.NUMBER_OF_TRACKS));

        return values;
    }
}
