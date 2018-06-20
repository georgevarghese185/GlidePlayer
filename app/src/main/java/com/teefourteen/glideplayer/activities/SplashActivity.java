/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import com.teefourteen.glideplayer.connectivity.network.Client;
import com.teefourteen.glideplayer.connectivity.network.server.GPServer;
import com.teefourteen.glideplayer.connectivity.network.server.Request;
import com.teefourteen.glideplayer.connectivity.network.server.Response;
import com.teefourteen.glideplayer.dialogs.NeedPermissionsDialog;
import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.services.LibraryService;

import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {
    public static final String LIBRARY_INIT_THREAD_NAME = "lib-init-thread";
    final static public int REQUEST_READ_EXTERNAL_STORAGE = 1;
    final public static String LIBRARY_INITIALIZED_ACTION = "library_initialized";
    private BroadcastReceiver libInitCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GPServer server = new GPServer(
                8081, request -> {
                    for(Map.Entry<String, String> param : request.requestParams.entrySet()) {
                        Log.d("Server Test", param.getKey() + ":" + param.getValue());
                    }
                    File cache = getExternalCacheDir();
                    if(cache != null && cache.exists()) {
                        FileOutputStream fOut = null;
                        BufferedWriter writer = null;
                        try {
                            File file = new File(cache, "sometxt");
                            fOut = new FileOutputStream(file);
                            writer = new BufferedWriter(new OutputStreamWriter(fOut));

                            writer.write(request.requestParams.get("id"));
                            writer.flush();
                            writer.close();

                            return new Response(file);
                        } catch (Exception e) {
                            try {
                                fOut.close();
                                writer.close();
                            } catch (Exception e1) {/*watever*/}
                            return new Response("I DONT SERVER N00BS");
                        }
                    } else {
                        return new Response("I DONT SERVER N00BS");
                    }
                });

        try {
            server.start();
        } catch (IOException e) {
            Log.e("Server Test", "Error while starting server", e);
        }

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... objects) {
                Client c = new Client("1", "192.168.0.4", 8081);
                HashMap<String, String> req = new HashMap<>(1);
                req.put("id", "1234");
                Response r = server.requestFile(c, "/media/song", req, new File(getExternalCacheDir(), "sometxt").getAbsolutePath());
                return r;
            }
        }.execute((Object) null);
//        libInitCompleteReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Intent mainActivityIntent = new Intent(context, MainActivity.class);
//                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(mainActivityIntent);
//                finish();
//            }
//        };
//
//        LocalBroadcastManager.getInstance(this).registerReceiver(libInitCompleteReceiver,
//                new IntentFilter("library_initialized"));
//
//        setContentView(R.layout.activity_splash);
//
//        if(readPermissionGranted())
//                    startService(new Intent(getApplicationContext(), LibraryService.class));
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
