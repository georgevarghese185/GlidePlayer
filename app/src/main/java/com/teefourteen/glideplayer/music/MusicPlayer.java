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
