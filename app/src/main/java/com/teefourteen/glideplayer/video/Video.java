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

    public Video(long videoId, String title, long duration, String filePath, int width, int height,
                 long size) {
        this.videoId = videoId;
        this.title = title;
        this.duration = duration;
        this.filePath = filePath;
        this.width = width;
        this.height = height;
        this.size = size;
    }
    
    public static Video toVideo(Cursor cursor) {
        return new Video(
                Library.getLong(cursor, VideoTable.Columns.VIDEO_ID),
                Library.getString(cursor, VideoTable.Columns.TITLE),
                Library.getLong(cursor, VideoTable.Columns.DURATION),
                Library.getString(cursor, VideoTable.Columns.FILE_PATH),
                Library.getInt(cursor, VideoTable.Columns.WIDTH),
                Library.getInt(cursor, VideoTable.Columns.HEIGHT),
                Library.getLong(cursor, VideoTable.Columns.SIZE)
        );
    }
}
