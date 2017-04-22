package com.teefourteen.glideplayer.connectivity;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public class CacheFile {

    private long size;
    private File file;
    private Connection connection;
    private boolean downloading = false;
    private boolean downloadSuccessful = false;
    private DownloadCompleteListener downloadCompleteListener = null;

    public interface DownloadCompleteListener {
        void onDownloadComplete();
        void onDownloadFailed();
    }

    public CacheFile(long size, Connection connection) {
        this.size = size;
        this.connection = connection;
    }

    public CacheFile(File file) {
        this.file = file;
        this.size = file.length();
        this.downloading = false;
        this.downloadSuccessful = true;
    }

    void startDownload(final String filePath) {
        downloading = true;
        this.file = new File(filePath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            downloading = false;
            downloadSuccessful = false;
            if(downloadCompleteListener != null) {
                downloadCompleteListener.onDownloadFailed();
            }
            return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    connection.getNextFile(filePath, size);
                    downloading = false;
                    downloadSuccessful = true;
                    connection.close();
                    if (downloadCompleteListener != null) {
                        downloadCompleteListener.onDownloadComplete();
                    }
                } catch (IOException e) {
                    downloading = false;
                    downloadSuccessful = false;
                    if (downloadCompleteListener != null) {
                        downloadCompleteListener.onDownloadFailed();
                    }
                    connection.close();
                }
            }
        };

        new Thread(r).start();
    }

    public File getFile() {
        return file;
    }

    public long size() {
        return size;
    }

    public long downloadedSize() {
        return file.length();
    }

    public void registerDownloadCompleteListener(DownloadCompleteListener listener) {
        this.downloadCompleteListener = listener;
    }

    public void unregisterDownloadCompleteListener() {
        downloadCompleteListener = null;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public boolean isDownloadSuccessful() {
        return downloadSuccessful;
    }

    public void cancelDownload() {
        if(connection != null && !connection.getSocket().isClosed()) {
            connection.close();
            Log.d("CacheFile","File download cancelled");
        }
    }
}
