package teefourteen.glideplayer.databases.library;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Albums;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by george on 21/10/16.
 */

public class LibraryHelper {

    private LibraryHelper() {
    }

    public static class AlbumArtHelper extends SQLiteOpenHelper {
        public final static String DATABASE_NAME="album_art";
        public final static int DATABASE_VERSION=1;
        public static final String TABLE_NAME = "AlbumArt";

        public class Columns implements BaseColumns {
            public static final String ALBUM_ART = "album_art";
        }

        public AlbumArtHelper(Context context) {
            super(context, new File(context.getCacheDir(),DATABASE_NAME).getAbsolutePath(),
                    null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String query = "CREATE TABLE " + TABLE_NAME +"("
                    + Columns._ID + " INTEGER PRIMARY KEY, "
                    + Columns.ALBUM_ART + " TEXT "
                    + ")";
            db.execSQL(query);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            super.onDowngrade(db, oldVersion, newVersion);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public static Cursor getAlbumSongs(ContentResolver resolver, long albumId) {
        String selection = Media.IS_MUSIC + " !=0"
                + " AND " + Media.ALBUM_ID + " = " + albumId;
        return querySongs(resolver, selection);
    }

    public static Cursor getSongs(ContentResolver resolver) {
        String selection = Media.IS_MUSIC + " != 0";
        return querySongs(resolver, selection);
    }

    public static Cursor querySongs(ContentResolver resolver, String selection) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String projection[] = {
                Media._ID, Media.DATA, Media.ALBUM, Media.ALBUM_ID, Media.ARTIST,
                Media.ARTIST_ID, Media.DURATION, Media.TITLE,
                Media.TRACK};

        return resolver.query(uri, projection, selection, null, Media.TITLE);
    }

    public static Cursor getAlbums(ContentResolver resolver) {
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String[] projection = {
                Albums._ID, Albums.ALBUM, Albums.ALBUM_ART,
                Albums.ARTIST, Albums.NUMBER_OF_SONGS
        };

        return resolver.query(uri, projection, null, null, Albums.ALBUM);
    }
    
    public static void cacheAlbumArt(Cursor albumCursor, SQLiteDatabase albumArtDb) {
        albumArtDb.delete(AlbumArtHelper.TABLE_NAME, null, null);

        if(albumCursor!=null && albumCursor.moveToFirst()) {
            do {
                long _id = getLong(albumCursor,Albums._ID);
                String albumArt = getString(albumCursor,Albums.ALBUM_ART);

                ContentValues values = new ContentValues();
                values.put(AlbumArtHelper.Columns._ID, _id);
                values.put(AlbumArtHelper.Columns.ALBUM_ART, albumArt);

                albumArtDb.insert(AlbumArtHelper.TABLE_NAME, null, values);
            }while (albumCursor.moveToNext());
        }
    }

    public static String getAlbumArt(long albumId, SQLiteDatabase albumArtDb) {
        Cursor cursor = albumArtDb.query(
                AlbumArtHelper.TABLE_NAME, new String[] {AlbumArtHelper.Columns.ALBUM_ART},
                AlbumArtHelper.Columns._ID + " = " + albumId, null, null, null, null
        );
        if(cursor!=null && cursor.moveToFirst()) {
            return getString(cursor,AlbumArtHelper.Columns.ALBUM_ART);
        }
        else return null;
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
}
