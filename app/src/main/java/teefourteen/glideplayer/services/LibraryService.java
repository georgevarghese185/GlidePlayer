package teefourteen.glideplayer.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import teefourteen.glideplayer.EasyHandler;
import teefourteen.glideplayer.activities.SplashActivity;
import teefourteen.glideplayer.music.database.Library;

import static teefourteen.glideplayer.Global.songCursor;
import static teefourteen.glideplayer.fragments.library.AlbumsFragment.albumCursor;


public class LibraryService extends IntentService {
    EasyHandler handler = new EasyHandler(); //testing only

    private class test implements Runnable{
        Context context;
        File file;
        String ip;
        boolean reciever;

        test(Context context, boolean reciever, File file) {
            this.context = context;
            this.reciever = reciever;
            this.file = file;
        }
        test(Context context, boolean reciever, File file, String ip) {
            this.context = context;
            this.reciever = reciever;
            this.file = file;
            this.ip = ip;
        }

        @Override
        public void run() {
            Library lib = new Library(context, file);

            try {
                if(reciever) {
                    Socket socket = new Socket(ip, 23411);

                    lib.getFromStream(socket.getInputStream(), socket.getOutputStream());

                    socket.close();
                    lib.close();

                    SQLiteDatabase libraryDb = lib.getReadableDatabase();

                    songCursor = Library.getSongs(libraryDb);

                    albumCursor = Library.getAlbums(libraryDb);

                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                            new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
                } else {
                    ServerSocket serverSocket = new ServerSocket(23411);
                    Socket socket = serverSocket.accept();

                    lib.sendOverStream(socket.getInputStream(), socket.getOutputStream());

                    socket.close();
                    serverSocket.close();
                    lib.close();

                    SQLiteDatabase libraryDb = lib.getReadableDatabase();

                    songCursor = Library.getSongs(libraryDb);

                    albumCursor = Library.getAlbums(libraryDb);

                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                            new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
                }
            } catch (IOException e) {
                Log.d("socket fail", e.toString());
            } catch (JSONException e) {
                //nothing
            }
        }
    }

    public LibraryService() {
        super(SplashActivity.LIBRARY_INIT_THREAD_NAME);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        File file = new File(getObbDir().getAbsolutePath(), Library.DATABASE_NAME);
        File test = new File(getObbDir().getAbsolutePath(), "testlib.db");
        test.delete();

        Library library = new Library(this, file);

        library.initializeTables();
        library.close();


        if(intent.hasExtra("TEST_Mode_Receiver")) {
            handler.executeAsync(new test(this, true, test, intent.getStringExtra("TEST_Mode_Receiver")), "second lib");
        } else if(intent.hasExtra("TEST_Mode_Sender")){
            handler.executeAsync(new test(this, false, file), "original lib");
        } else {

            SQLiteDatabase libraryDb = library.getReadableDatabase();

            songCursor = Library.getSongs(libraryDb);

            albumCursor = Library.getAlbums(libraryDb);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                    new Intent(SplashActivity.LIBRARY_INITIALIZED_ACTION));
        }
    }
}
