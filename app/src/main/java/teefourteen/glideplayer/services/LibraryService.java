package teefourteen.glideplayer.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;

import teefourteen.glideplayer.activities.SplashActivity;
import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.music.database.Library;

import static teefourteen.glideplayer.fragments.library.AlbumsFragment.albumCursor;


public class LibraryService extends IntentService {
    public LibraryService() {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        File file = new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME);

        Library library = new Library(this, file);

        library.initializeTables();
        library.close();

        SQLiteDatabase libraryDb = library.getReadableDatabase();

        Global.songCursor = Library.getSongs(libraryDb);

        albumCursor = Library.getAlbums(libraryDb);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
    }
}
