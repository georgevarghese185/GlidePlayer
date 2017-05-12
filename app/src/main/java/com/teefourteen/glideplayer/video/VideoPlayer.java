package com.teefourteen.glideplayer.video;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.teefourteen.glideplayer.Player;
import com.teefourteen.glideplayer.connectivity.RemoteFileCache;

import java.io.IOException;


public class VideoPlayer extends Player<Video> implements MediaPlayer.OnVideoSizeChangedListener {
    private VideoSizeChangedListener sizeChangedListener;

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if(sizeChangedListener != null) {
            sizeChangedListener.videoSizeChanged(width, height);
        }
    }

    public interface VideoSizeChangedListener {
        void videoSizeChanged(int width, int height);
    }

    public VideoPlayer(Context context,
                       VideoSizeChangedListener sizeChangedListener) {
        super(context);
        this.sizeChangedListener = sizeChangedListener;
    }

    public boolean playMedia(Video media, SurfaceHolder holder) {
        try {
            boolean result;
            result = prepared || super.prepareMedia(media);
            if (result) {
                mediaPlayer.setOnVideoSizeChangedListener(this);
                mediaPlayer.setDisplay(holder);
                return playMedia(media);
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public boolean playMedia(SurfaceHolder holder) {
        if(prepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.setDisplay(holder);
            play();
            return true;
        } else return prepared;
    }

    public boolean prepareMedia(Video media, SurfaceHolder holder) throws IOException {
        boolean result = super.prepareMedia(media);
        if(result) {
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setDisplay(holder);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getFilePath(Video media) {
        return media.filePath;
    }

    @Override
    protected int getDuration(Video media) {
        return (int)media.duration;
    }

    @Override
    protected Uri getRemoteMediaUri(Video media) throws RemoteFileCache.BiggerThanCacheException {
        return RemoteFileCache.getInstance().getVideoUri(media);
    }
}
