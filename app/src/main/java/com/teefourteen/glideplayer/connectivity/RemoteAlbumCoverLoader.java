package com.teefourteen.glideplayer.connectivity;

import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.teefourteen.glideplayer.AsyncImageLoader;
import com.teefourteen.glideplayer.CancellableAsyncTaskHandler;

public class RemoteAlbumCoverLoader extends CancellableAsyncTaskHandler{
    private AsyncImageLoader asyncImageLoader;

    public RemoteAlbumCoverLoader(int maxNetworkThreads, AsyncImageLoader asyncImageLoader) {
        super(maxNetworkThreads);
        this.asyncImageLoader = asyncImageLoader;
    }

    public class RemoteCoverTask implements Task{
        private String username;
        private long albumId;
        private CacheFile albumArtFile = null;
        private String filePath;
        private ImageView imageView;
        private AsyncImageLoader.ImageLoadTask imageLoadTask = null;
        private CountDownLatch latch = new CountDownLatch(1);

        RemoteCoverTask(ImageView imageView, String username, long albumId, String filePath) {
            this.username = username;
            this.albumId = albumId;
            this.filePath = filePath;
            this.imageView = imageView;
        }

        @Override
        public void doBackground() {
            albumArtFile = ShareGroup.getAlbumArt(username, albumId);
            if(albumArtFile == null) return;
            albumArtFile.registerDownloadCompleteListener(new CacheFile.DownloadCompleteListener() {
                @Override
                public void onDownloadComplete() {
                    latch.countDown();
                }

                @Override
                public void onDownloadFailed() {
                    latch.countDown();
                }
            });

            try {
                if(albumArtFile.isDownloading()) latch.await();
            } catch (InterruptedException e) {
                Log.d("doBackground()", "who dares disturb my slumber");
            }

            if (albumArtFile.isDownloadSuccessful()) {
                File newFile = new File(filePath);
                if(newFile.exists()) newFile.delete();
                FileInputStream fin;
                FileOutputStream fout;
                try {
                    newFile.createNewFile();
                    fin = new FileInputStream(albumArtFile.getFile());
                    fout = new FileOutputStream(newFile);
                    int length;
                    byte[] buffer = new byte[8096];
                    while((length = fin.read(buffer)) > 0) {
                        fout.write(buffer,0,length);
                    }
                    fin.close();
                    fout.close();
                    ShareGroup.deleteCacheFile(albumArtFile.getFile().getName());
                    albumArtFile = new CacheFile(newFile);
                } catch (IOException e) {
                    if(newFile.exists()) { newFile.delete(); }
                }
            }
        }

        @Override
        public void doOnMainThread() {
            if(albumArtFile !=null && albumArtFile.isDownloadSuccessful()) {
                imageLoadTask = asyncImageLoader.loadImageAsync(
                        imageView, albumArtFile.getFile().getAbsolutePath());
            }
        }

        public void cancel() {
            if(imageLoadTask != null) {
                asyncImageLoader.cancelTask(imageLoadTask);
            } else if(albumArtFile != null){
                albumArtFile.cancelDownload();
            }
        }
    }

    public RemoteCoverTask loadRemoteCover(ImageView imageView,
                                           String username, long albumId, String filePath) {

        RemoteCoverTask remoteCoverTask = new RemoteCoverTask(imageView, username, albumId,
                filePath);

        super.loadAsync(remoteCoverTask);

        return remoteCoverTask;
    }

    public synchronized void cancelTask(Task task) {
        super.cancelTask(task,false);
        ((RemoteCoverTask)task).cancel();
    }
}
