package teefourteen.glideplayer.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import teefourteen.glideplayer.activities.SplashActivity;
import teefourteen.glideplayer.music.Library;
import teefourteen.glideplayer.fragments.library.AlbumsFragment;
import teefourteen.glideplayer.Global;

import static teefourteen.glideplayer.fragments.library.AlbumsFragment.albumCursor;

/**
 * Created by george on 21/10/16.
 */

public class LibraryService extends IntentService {
    public LibraryService() {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        Global.songCursor = Library.getSongs(this.getContentResolver());

        albumCursor = Library.getAlbums(this.getContentResolver());

        Library.AlbumArtHelper artHelper = new Library.AlbumArtHelper(this);
        SQLiteDatabase albumArtDb = artHelper.getWritableDatabase();
        Library.cacheAlbumArt(albumCursor, albumArtDb);
        albumArtDb.close();

        AlbumsFragment.albumArtDb =
                new Library.AlbumArtHelper(this).getReadableDatabase();

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
    }
}
