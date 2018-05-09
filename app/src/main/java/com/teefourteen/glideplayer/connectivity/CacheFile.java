package com.teefourteen.glideplayer.connectivity;

import android.util.Log;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.StateListener;

import java.io.File;
import java.io.IOException;

import static com.teefourteen.glideplayer.connectivity.CacheFile.DownloadState.DOWNLOADED;
import static com.teefourteen.glideplayer.connectivity.CacheFile.DownloadState.DOWNLOADING;
import static com.teefourteen.glideplayer.connectivity.CacheFile.DownloadState.FAILED;
import static com.teefourteen.glideplayer.connectivity.CacheFile.DownloadState.INIT;

public class CacheFile extends StateListener<CacheFile.DownloadListener, CacheFile.DownloadState>{

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

    public interface DownloadListener {
        void onDownloading();
        void onDownloadComplete();
        void onDownloadFail();
    }

    public enum DownloadState {
        INIT,
        DOWNLOADING,
        DOWNLOADED,
        FAILED
    }

    public CacheFile(long size, Connection connection) {
        super(null, INIT);

        this.size = size;
        this.connection = connection;
    }

    public CacheFile(File file) {
        super(null, INIT);

        this.file = file;
        this.size = file.length();
        this.downloading = false;
        this.downloadSuccessful = true;
    }

    void startDownload(final String filePath) {
        downloading = true;
        updateState(DOWNLOADING);
        this.file = new File(filePath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            downloading = false;
            updateState(FAILED);
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
                    updateState(DOWNLOADED);
                    downloading = false;
                    downloadSuccessful = true;
                    connection.close();
                    if (downloadCompleteListener != null) {
                        downloadCompleteListener.onDownloadComplete();
                    }
                } catch (IOException e) {
                    downloading = false;
                    updateState(FAILED);
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

    @Override
    protected void notifyListener(DownloadListener downloadListener, DownloadState downloadState) {
        EasyHandler.executeOnMainThread(() -> {
            switch (downloadState) {

                case DOWNLOADING:
                    downloadListener.onDownloading();
                    break;
                case DOWNLOADED:
                    downloadListener.onDownloadComplete();
                    break;
                case FAILED:
                    downloadListener.onDownloadFail();
                    break;
            }
        });
    }
}
