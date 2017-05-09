package com.teefourteen.glideplayer.music;

import android.content.Context;
import android.net.Uri;

import com.teefourteen.glideplayer.Player;
import com.teefourteen.glideplayer.connectivity.RemoteFileCache;


public class MusicPlayer extends Player<Song>{

    public MusicPlayer(Context context) {
        super(context);
    }

    @Override
    protected String getFilePath(Song media) {
        return media.getFilePath();
    }

    @Override
    protected int getDuration(Song media) {
        return (int)media.getDuration();
    }

    @Override
    protected Uri getRemoteMediaUri(Song media) throws RemoteFileCache.BiggerThanCacheException {
        return RemoteFileCache.getInstance().getSongUri(media);
    }
}
