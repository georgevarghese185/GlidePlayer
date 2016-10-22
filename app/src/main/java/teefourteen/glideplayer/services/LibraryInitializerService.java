package teefourteen.glideplayer.services;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;

import teefourteen.glideplayer.NeedPermissionsDialog;
import teefourteen.glideplayer.activities.MainActivity;
import teefourteen.glideplayer.activities.SplashActivity;
import teefourteen.glideplayer.databases.library.LibraryContract;
import teefourteen.glideplayer.databases.library.LibraryHelper;

/**
 * Created by george on 21/10/16.
 */

public class LibraryInitializerService extends IntentService {
    public LibraryInitializerService () {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        SQLiteDatabase libraryDb =
                new LibraryHelper(getApplicationContext()).getWritableDatabase();
        libraryDb.delete(LibraryContract.SongTable.TABLE_NAME, null, null);

        Cursor mediaStoreCursor = getMediaStoreCursor();
        if (mediaStoreCursor == null)
            return;

        try {
            mediaStoreCursor.moveToFirst();
            do {
                ContentValues values = putMediaStoreValues(mediaStoreCursor);
                libraryDb.insert(LibraryContract.SongTable.TABLE_NAME, null, values);
            } while (mediaStoreCursor.moveToNext());
        }
        catch (NullPointerException e) {

        }
        mediaStoreCursor.close();
        libraryDb.close();

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
    }

    private Cursor getMediaStoreCursor() {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.Audio.AudioColumns.ALBUM_KEY,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ARTIST_ID,
                MediaStore.Audio.AudioColumns.ARTIST_KEY,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.BOOKMARK,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.TITLE_KEY,
                MediaStore.Audio.AudioColumns.TRACK,
                MediaStore.Audio.AudioColumns.YEAR
        };
        return contentResolver.query(musicUri, projection, "IS_MUSIC != 0", null, MediaStore.Audio.AudioColumns.TITLE);
    }

    private ContentValues putMediaStoreValues(Cursor mediaStoreCursor) {
        ContentValues values = new ContentValues();
        values.put(LibraryContract.SongTable.COLUMN_NAME_ALBUM, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_ALBUM_ID, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_ALBUM_KEY, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_KEY)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_ARTIST, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_ARTIST_ID, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_ID)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_ARTIST_KEY, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_KEY)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_DATE_ADDED, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_DURATION, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_FILE_PATH, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_BOOKMARK, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.BOOKMARK)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_SIZE, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_TITLE, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_TITLE_KEY, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE_KEY)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_TRACK_NUMBER, mediaStoreCursor.getInt(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)));
        values.put(LibraryContract.SongTable.COLUMN_NAME_YEAR, mediaStoreCursor.getInt(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)));
        return values;
    }
}
