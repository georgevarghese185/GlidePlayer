package com.teefourteen.glideplayer.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import com.teefourteen.glideplayer.dialogs.NeedPermissionsDialog;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.music.database.Library;
import com.teefourteen.glideplayer.services.LibraryService;

public class SplashActivity extends AppCompatActivity {
    public static final String LIBRARY_INIT_THREAD_NAME = "lib-init-thread";
    final static public int REQUEST_READ_EXTERNAL_STORAGE = 1;
    final public static String LIBRARY_INITIALIZED_ACTION = "library_initialized";
    private BroadcastReceiver libInitCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        libInitCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent mainActivityIntent = new Intent(context, MainActivity.class);
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivityIntent);
                finish();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(libInitCompleteReceiver,
                new IntentFilter("library_initialized"));

        setContentView(R.layout.activity_splash);

        if(readPermissionGranted())
                    startService(new Intent(getApplicationContext(), LibraryService.class));
    }

    private boolean readPermissionGranted() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
            return false;
        }
        else {
            createFiles();
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if(requestCode==REQUEST_READ_EXTERNAL_STORAGE && grantResults.length>0
                    && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                createFiles();
                startService(new Intent(this, LibraryService.class));
            }
            else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.currentThread().sleep(2000);
                        } catch (InterruptedException e) {}
                        new NeedPermissionsDialog().show(getSupportFragmentManager(), "new_alert_dialog");
                    }
                }).start();
            }
    }

    private void createFiles() {
        String cacheDir;
        if(getExternalCacheDir() == null) {
            Toast.makeText(this, "Failed to get Internal storage. Using app data storage.", Toast.LENGTH_LONG).show();
            cacheDir = getCacheDir().getAbsolutePath();
        } else  {
            cacheDir = getExternalCacheDir().getAbsolutePath();
        }

        Library.DATABASE_LOCATION = cacheDir + "/libraries";
        //before changing covers location, remember that the file move action in RemoteAlbumCoverLoader can only move files on the same mount point
        Library.REMOTE_COVERS_LOCATION = cacheDir + "/remote_album_covers";
        Library.FILE_SAVE_LOCATION = cacheDir + "/files";

        try {
            createDir(Library.DATABASE_LOCATION);
            createDir(Library.REMOTE_COVERS_LOCATION);
            createDir(Library.FILE_SAVE_LOCATION);
        } catch (IOException e) {
            Toast.makeText(this,"Failed to create directory", Toast.LENGTH_LONG).show();
            Log.d("splash fail", e.toString());
            finishAffinity();
        }
    }

    private void createDir(String dirPath)throws IOException {
        File file = new File(dirPath);
        if(!file.exists()) {
            if(!file.mkdir()) throw new IOException("failed to create " + dirPath);
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(libInitCompleteReceiver);
        super.onDestroy();
    }
}
