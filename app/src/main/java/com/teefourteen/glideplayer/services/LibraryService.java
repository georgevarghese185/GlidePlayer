package com.teefourteen.glideplayer.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;

import com.teefourteen.glideplayer.activities.SplashActivity;
import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.music.database.Library;


public class LibraryService extends IntentService {
    public LibraryService() {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        File file = new File(Library.DATABASE_LOCATION, Library.LOCAL_DATABASE_NAME);
        if(file.exists()) {
            file.delete();
        }

        Library.initialize(this);

        File lastQueue = new File(PlayerService.PLAY_QUEUE_FILE_PATH);

        if(lastQueue.exists()) {
            try {
                Global.playQueue = new PlayQueue(lastQueue);
            } catch (IOException e) {
                Global.playQueue = null;
                lastQueue.delete();
            }
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
    }
}
