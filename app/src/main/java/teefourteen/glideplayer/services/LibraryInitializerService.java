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

import teefourteen.glideplayer.activities.SplashActivity;
import teefourteen.glideplayer.databases.library.LibraryHelper;
import teefourteen.glideplayer.databases.library.SongTable;

/**
 * Created by george on 21/10/16.
 */

public class LibraryInitializerService extends IntentService {
    public LibraryInitializerService () {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        LibraryHelper libraryHelper = new LibraryHelper(getApplicationContext());
        SQLiteDatabase libraryDb = libraryHelper.getWritableDatabase();
        libraryDb.delete(SongTable.TABLE_NAME, null, null);

        Cursor mediaStoreCursor = getMediaStoreCursor();
        if (mediaStoreCursor == null)
            return;

        try {
            mediaStoreCursor.moveToFirst();
            do {
                ContentValues values = putMediaStoreValues(mediaStoreCursor);
                libraryDb.insert(SongTable.TABLE_NAME, null, values);
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
        values.put(SongTable.ALBUM, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)));
        values.put(SongTable.ALBUM_ID, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)));
        values.put(SongTable.ALBUM_KEY, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_KEY)));
        values.put(SongTable.ARTIST, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)));
        values.put(SongTable.ARTIST_ID, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_ID)));
        values.put(SongTable.ARTIST_KEY, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_KEY)));
        values.put(SongTable.DATE_ADDED, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED)));
        values.put(SongTable.DURATION, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));
        values.put(SongTable.FILE_PATH, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)));
        values.put(SongTable.BOOKMARK, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.BOOKMARK)));
        values.put(SongTable.SIZE, mediaStoreCursor.getLong(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE)));
        values.put(SongTable.TITLE, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)));
        values.put(SongTable.TITLE_KEY, mediaStoreCursor.getString(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE_KEY)));
        values.put(SongTable.TRACK_NUMBER, mediaStoreCursor.getInt(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK)));
        values.put(SongTable.YEAR, mediaStoreCursor.getInt(mediaStoreCursor.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)));
        return values;
    }
}
