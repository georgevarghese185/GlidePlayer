package com.teefourteen.glideplayer.connectivity;

import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;

import java.io.File;
import java.io.IOException;

public class CacheFile {
    private static final String FILE_DOWNLOAD_THREAD_NAME = "cache_file_download_thread";

    private long size;
    private File file;
    private EasyHandler handler = new EasyHandler();
    private Connection connection;
    private boolean downloading = false;
    private boolean downloadSuccessful = false;
    private DownloadCompleteListener downloadCompleteListener = null;

    public interface DownloadCompleteListener {
        void onDownloadComplete();
        void onDownloadFailed();
    }

    public CacheFile(long size, File file, Connection connection) {
        this.size = size;
        this.file = file;
        this.connection = connection;
        downloading = true;
        handler.executeAsync(new Runnable() {
            @Override
            public void run() {
                startDownload();
            }
        }, FILE_DOWNLOAD_THREAD_NAME);
    }

    public CacheFile(File file) {
        this.file = file;
        this.downloading = false;
        this.downloadSuccessful = true;
    }

    private void startDownload() {
        try {
            file = connection.getNextFile(file.getAbsolutePath(), size);
            downloading = false;
            downloadSuccessful = true;
            connection.close();
            if(downloadCompleteListener != null) {
                downloadCompleteListener.onDownloadComplete();
            }
        } catch (IOException e) {
            if(downloadCompleteListener != null) {
                downloading = false;
                downloadSuccessful = false;
                downloadCompleteListener.onDownloadFailed();
                connection.close();
            }
        }
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
        if(downloading) {
            connection.close();
            Log.d("CacheFile","File download cancelled");
        }
    }
}
