package com.teefourteen.glideplayer.connectivity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.music.database.Library;


public class RemoteFileCache {
    public static final String TYPE_SONG = "song";
    private static final String CACHE_DATABASE_NAME = "cache.db";
    private static RemoteFileCache instance;

    private long maxSize;
    private File cacheDbFile;
    private SQLiteDatabase cacheDb;
    private StreamServer streamServer;
    private String serverUrl;
    private long cacheFileName = 0;
    private long cacheSize = 0;
    


    private RemoteFileCache(Context context, long maxSize) throws IOException {
        streamServer = new StreamServer();
        streamServer.start();
        serverUrl = "http://127.0.0.1:" + streamServer.port + "/";

        this.maxSize = maxSize;

        cacheDbFile = new File(Library.FILE_SAVE_LOCATION, CACHE_DATABASE_NAME);
        if(cacheDbFile.exists()) cacheDbFile.delete();

        CacheDatabase cacheDatabase = new CacheDatabase(context, cacheDbFile);
        cacheDb = cacheDatabase.getWritableDatabase();
    }

    static void createInstance(Context context, long maxSize) throws IOException {
        if(instance == null) {
            instance = new RemoteFileCache(context, maxSize);
        }
    }

    public static RemoteFileCache getInstance() {
        return instance;
    }

    void destroy() {
        new Thread(new Runnable() {
            @Override
            public void run() { streamServer.stop(); }
        }).start();
        instance.cacheDb.close();
        instance.cacheDbFile.delete();
        instance = null;
    }

    public CacheFile getAlbumArt(String username, long albumId) throws BiggerThanCacheException {
        CacheFile albumArt = Group.getInstance().getAlbumArt(username, albumId);
        if(albumArt != null) {
            if(!makeSpace(albumArt.size()))  {
                albumArt.cancelDownload();
                return null;
            }
            albumArt.startDownload(
                    Library.FILE_SAVE_LOCATION + "/" + String.valueOf(cacheFileName));
            addNewFile(albumArt);
            cacheFileName++;
        }

        return albumArt;
    }

    public Uri getSongUri(Song song)throws BiggerThanCacheException {
        if(!song.isRemote()) return null;
        Uri uri = null;
        String username = song.getLibraryUsername();
        long songId = song.get_id();

        String whereClause = CacheDatabase.SongFileTable.Columns.USERNAME + "=?"
                + " AND " + CacheDatabase.SongFileTable.Columns.SONG_ID +"=?";
        String[] whereArgs = {username, String.valueOf(songId)};

        Cursor cursor = cacheDb.query(true, CacheDatabase.SongFileTable.TABLE_NAME,
                new String[]{CacheDatabase.SongFileTable.Columns.FILE_NAME},
                whereClause, whereArgs,
                null, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            uri = Uri.parse(Library.FILE_SAVE_LOCATION + "/" +
            Library.getString(cursor, CacheDatabase.SongFileTable.Columns.FILE_NAME));
            cursor.close();
        } else {
//            TODO: commented out until buffering issues tested and fixed
//            if (cursor != null) cursor.close();
//            if(!makeSpace(song.getSize())) throw new BiggerThanCacheException();
//            uri = Uri.parse(serverUrl + TYPE_SONG + "/" + username + "/" + songId);


            //TODO: temporary code. Downloading entire file and returning file URI
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            CacheFile cacheFile = Group.getInstance().getSong(username, songId);
            if(cacheFile == null) return null;
            cacheFile.registerDownloadCompleteListener(new CacheFile.DownloadCompleteListener() {
                @Override
                public void onDownloadComplete() {
                    countDownLatch.countDown();
                }

                @Override
                public void onDownloadFailed() {
                    countDownLatch.countDown();
                }
            });

            cacheFile.startDownload( Library.FILE_SAVE_LOCATION + "/" + String.valueOf(cacheFileName++));

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                return null;
            }

            if(cacheFile.isDownloadSuccessful()) {
                uri = Uri.parse(cacheFile.getFile().getAbsolutePath());
            } else {
                return null;
            }
            addNewSongFile(cacheFile, username, songId);
            //TODO: end of temporary code.
        }

        return uri;
    }
    
    private void addNewFile(CacheFile file) {
        ContentValues v = new ContentValues();
        v.put(CacheDatabase.FileTable.Columns.FILE_NAME, file.getFile().getName());
        v.put(CacheDatabase.FileTable.Columns.FILE_SIZE, file.size());
        cacheDb.insert(CacheDatabase.FileTable.TABLE_NAME, null, v);
        cacheSize += file.size();
    }

    private void addNewSongFile(CacheFile file, String username, long songId) {
        addNewFile(file);
        ContentValues v = new ContentValues();
        v.put(CacheDatabase.SongFileTable.Columns.FILE_NAME, file.getFile().getName());
        v.put(CacheDatabase.SongFileTable.Columns.USERNAME, username);
        v.put(CacheDatabase.SongFileTable.Columns.SONG_ID, songId);
        cacheDb.insert(CacheDatabase.SongFileTable.TABLE_NAME, null, v);
    }

    public void deleteFile(String fileName) {
        Cursor cursor = cacheDb.query(true, CacheDatabase.FileTable.TABLE_NAME,
                new String[]{CacheDatabase.FileTable.Columns.FILE_SIZE},
                CacheDatabase.FileTable.Columns.FILE_NAME + "=?", new String[]{fileName},
                null, null, null, null);

        if(cursor!=null && cursor.moveToFirst()) {
            cacheSize -= Library.getLong(cursor, CacheDatabase.FileTable.Columns.FILE_SIZE);

            cacheDb.delete(CacheDatabase.SongFileTable.TABLE_NAME,
                    CacheDatabase.SongFileTable.Columns.FILE_NAME + "=?",
                    new String[]{fileName});

            cacheDb.delete(CacheDatabase.FileTable.TABLE_NAME,
                    CacheDatabase.FileTable.Columns.FILE_NAME + "=?",
                    new String[]{fileName});

            new File(Library.FILE_SAVE_LOCATION, fileName).delete();

            cursor.close();
        }
    }

    private boolean makeSpace(long size) throws BiggerThanCacheException {
        if(size > maxSize) throw new BiggerThanCacheException();
        if(cacheSize + size > maxSize) {
            Cursor cursor = cacheDb.query(true, CacheDatabase.FileTable.TABLE_NAME,
                    new String[]{CacheDatabase.FileTable.Columns.FILE_NAME,
                            CacheDatabase.FileTable.Columns.FILE_SIZE},
                    null, null, null, null, null, null);

            if(cursor == null) return false;

            if(cursor.moveToFirst()) {
                do {
                    deleteFile(Library.getString(cursor, CacheDatabase.FileTable.Columns.FILE_NAME));
                    cacheSize -= Library.getLong(cursor, CacheDatabase.FileTable.Columns.FILE_SIZE);
                } while (cursor.moveToNext() && cacheSize + size > maxSize);

                cursor.close();
            }

            return (cacheSize + size <= maxSize);
        }
        else return true;
    }


    public class BiggerThanCacheException extends Exception {
        
    }
    
    
    
    private static class CacheDatabase extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        
        CacheDatabase(Context context, File database) {
            super(context, database.getAbsolutePath(), null, DATABASE_VERSION);
        }
        
        class FileTable {
            static final String TABLE_NAME = "file";
            class Columns {
                static final String FILE_NAME = "file_name";
                static final String FILE_SIZE = "file_size";
            }
        }
        
        class SongFileTable {
            static final String TABLE_NAME = "song_file";
            class Columns {
                static final String USERNAME = "username";
                static final String SONG_ID = "song_id";
                static final String FILE_NAME =  "file_name";
            }
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            String query = "CREATE TABLE " + FileTable.TABLE_NAME + "("
                    + FileTable.Columns.FILE_NAME + " TEXT" + ", "
                    + FileTable.Columns.FILE_SIZE + " INTEGER" + ")";
            
            db.execSQL(query);
            
            query = "CREATE TABLE " + SongFileTable.TABLE_NAME + "("
                    + SongFileTable.Columns.USERNAME + " TEXT" + ", "
                    + SongFileTable.Columns.SONG_ID + " INTEGER" + ", "
                    + SongFileTable.Columns.FILE_NAME + " TEXT" + ")";
            
            db.execSQL(query);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
    

    private class StreamServer implements Runnable {

        private Thread thread;
        private boolean isRunning;
        private ServerSocket socket;
        private int port;
        private String TAG = "StreamServer";

        public StreamServer() throws IOException {
            // Create listening socket
            try {
                socket = new ServerSocket(0, 0, InetAddress.getByAddress(new byte[] {127,0,0,1}));
                socket.setSoTimeout(5000);
                port = socket.getLocalPort();
            } catch (UnknownHostException e) { // impossible
            } catch (IOException e) {
                Log.e(TAG, "IOException initializing server", e);
                throw e;
            }
        }

        public void start() {
            thread = new Thread(this);
            thread.start();
        }

        public void stop() {
            isRunning = false;
            thread.interrupt();
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Looper.prepare();
            isRunning = true;
            while (isRunning) {
                try {
                    Socket client = socket.accept();
                    if (client == null) {
                        continue;
                    }
                    Log.d(TAG, "client connected");

                    StreamFileTask task = new StreamFileTask(client);
                    String[] params = task.processRequest();
                    if (params != null) {
                        task.execute(params);
                    }

                } catch (SocketTimeoutException e) {
                    // Do nothing
                } catch (IOException e) {
                    Log.e(TAG, "Error connecting to client", e);
                }
            }
            Log.d(TAG, "Proxy interrupted. Shutting down.");
        }

        private class StreamFileTask extends AsyncTask<String, Void, Integer> {
            Socket client;
            int cbSkip;

            public StreamFileTask(Socket client) {
                this.client = client;
            }

            public String[] processRequest() {
                // Read HTTP headers
                String headers = "";
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String line;
                    while(!(line = reader.readLine()).equals("")) {
                        headers += line + "\n";
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading HTTP request header from stream:", e);
                    return null;
                }

                // Get the important bits from the headers
                String[] headerLines = headers.split("\n");
                String urlLine = headerLines[0];
                if (!urlLine.startsWith("GET ")) {
                    Log.e(TAG, "Only GET is supported");
                    return null;
                }

                Pattern pathPattern = Pattern.compile("GET /(.*) HTTP/[\\d .]+$");
                Matcher matcher = pathPattern.matcher(urlLine);
                matcher.find();

                String split[] = matcher.group(1).split("/");

                int charPos;
                // See if there's a "Range:" header
                for (int i=0 ; i<headerLines.length ; i++) {
                    String headerLine = headerLines[i];
                    if (headerLine.startsWith("Range: bytes=")) {
                        headerLine = headerLine.substring(13);
                        charPos = headerLine.indexOf('-');
                        if (charPos>0) {
                            headerLine = headerLine.substring(0,charPos);
                        }
                        cbSkip = Integer.parseInt(headerLine);
                    }
                }
                return split;
            }

            @Override
            protected Integer doInBackground(final String... params) {
                CacheFile cacheFile = null;

                String type = params[0];
                if(type.equals(TYPE_SONG)) {
                    cacheFile = Group.getInstance().getSong(params[1], Long.parseLong(params[2]));
                }

                if(cacheFile == null) return 0;

                final boolean downloadFailed[] = {false};
                final CacheFile reference = cacheFile;
                cacheFile.registerDownloadCompleteListener(new CacheFile.DownloadCompleteListener() {
                    @Override
                    public void onDownloadComplete() {
                        addNewSongFile(reference, params[1], Long.parseLong(params[2]));
                    }

                    @Override
                    public void onDownloadFailed() {
                        downloadFailed[0] = true;
                    }
                });

                cacheFile.startDownload(Library.FILE_SAVE_LOCATION + "/" + cacheFileName++);
                long fileSize = cacheFile.size();

                // Create HTTP header
                String headers = "HTTP/1.0 200 OK\r\n";
                headers += "Content-Type: " + "\r\n";   //blank since cache files have no extension. Can't guess MIME type.
                headers += "Content-Length: " + fileSize  + "\r\n";
                headers += "Connection: close\r\n";
                headers += "\r\n";

                // Begin with HTTP header
                int fc = 0;
                long cbToSend = fileSize - cbSkip;
                OutputStream output = null;
                byte[] buff = new byte[8 * 1024];
                try {
                    output = new BufferedOutputStream(client.getOutputStream(), 32*1024);
                    output.write(headers.getBytes());

                    // Loop as long as there's stuff to send
                    while (isRunning && cbToSend>0 && !downloadFailed[0] && !client.isClosed()) {

                        // See if there's more to send
                        File file = cacheFile.getFile();
                        fc++;
                        int cbSentThisBatch = 0;
                        if (file.exists()) {
                            FileInputStream input = new FileInputStream(file);
                            input.skip(cbSkip);
                            int cbToSendThisBatch = input.available();
                            while (cbToSendThisBatch > 0) {
                                int cbToRead = Math.min(cbToSendThisBatch, buff.length);
                                int cbRead = input.read(buff, 0, cbToRead);
                                if (cbRead == -1) {
                                    break;
                                }
                                cbToSendThisBatch -= cbRead;
                                cbToSend -= cbRead;
                                output.write(buff, 0, cbRead);
                                output.flush();
                                cbSkip += cbRead;
                                cbSentThisBatch += cbRead;
                            }
                            input.close();
                        }

                        // If we did nothing this batch, block for a second
                        if (cbSentThisBatch == 0) {
                            Log.d(TAG, "Blocking until more data appears");
                            Thread.sleep(100);
                        }
                    }
                    Log.d(TAG, (cbToSend==0)? "Finished sending" :
                            (downloadFailed[0])? "download failed" :
                                    (client.isClosed())? "Client socket closed" : "isRunning flipped");
                }
                catch (SocketException socketException) {
                    Log.e(TAG, "SocketException() thrown, proxy client has probably closed. This can exit harmlessly");
                }
                catch (Exception e) {
                    Log.e(TAG, "Exception thrown from streaming task:");
                    Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                // Cleanup
                try {
                    if (output != null) {
                        output.close();
                    }
                    client.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "IOException while cleaning up streaming task:");
                    Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                return 1;
            }

        }
    }
}
