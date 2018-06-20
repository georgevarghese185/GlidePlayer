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

package com.teefourteen.glideplayer.video;

import android.database.Cursor;

import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.database.VideoTable;


public class Video {
    final public long videoId;
    final public String title;
    final public long duration;
    final public String filePath;
    final public int width;
    final public int height;
    final public long size;
    final public boolean isRemote;
    final public String libraryUsername;

    public Video(long videoId, String title, long duration, String filePath, int width, int height,
                 long size, String libraryUsername) {
        this.videoId = videoId;
        this.title = title;
        this.duration = duration;
        this.filePath = filePath;
        this.width = width;
        this.height = height;
        this.size = size;
        this.libraryUsername = libraryUsername;
        isRemote = (libraryUsername != null);
    }
    
    public static Video toVideo(Cursor cursor) {
        return new Video(
                Library.getLong(cursor, VideoTable.Columns.VIDEO_ID),
                Library.getString(cursor, VideoTable.Columns.TITLE),
                Library.getLong(cursor, VideoTable.Columns.DURATION),
                Library.getString(cursor, VideoTable.Columns.FILE_PATH),
                Library.getInt(cursor, VideoTable.Columns.WIDTH),
                Library.getInt(cursor, VideoTable.Columns.HEIGHT),
                Library.getLong(cursor, VideoTable.Columns.SIZE),
                Library.getString(cursor, VideoTable.Columns.REMOTE_USERNAME)
        );
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof Video) && (((Video) obj).videoId == videoId);
    }
}
