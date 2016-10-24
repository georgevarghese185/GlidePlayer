package teefourteen.glideplayer.music;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import java.util.ArrayList;

import teefourteen.glideplayer.databases.library.SongTable;

/**
 * Created by george on 12/10/16.
 */
public class Song implements Parcelable{
    private long songId;
    private String filePath;
    private String title;
    private String album;
    private long albumId;
    private String artist;
    private long artistId;
    private long duration;

    public Song(long songId, String filePath, String title, String album, long albumId, String artist, long artistId, long duration) {
        this.songId = songId;
        this.filePath = filePath;
        this.title = title;
        this.album = album;
        this.albumId = albumId;
        this.artist = artist;
        this.artistId = artistId;
        this.duration = duration;
    }

    public long getSongId() {
        return songId;
    }

    public String getAlbum() {
        return album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public String getArtist() {
        return artist;
    }

    public long getArtistId() {
        return artistId;
    }

    public long getDuration() {
        return duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(songId);
        dest.writeString(filePath);
        dest.writeString(title);
        dest.writeString(album);
        dest.writeLong(albumId);
        dest.writeString(artist);
        dest.writeLong(artistId);
        dest.writeLong(duration);
    }

    static public Parcelable.Creator CREATOR = new Parcelable.Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel source) {
            long songId = source.readLong();
            String filePath = source.readString();
            String title = source.readString();
            String album = source.readString();
            long albumId = source.readLong();
            String artist = source.readString();
            long artistId = source.readLong();
            long duration = source.readLong();
            return new Song(songId, filePath, title, album, albumId, artist, artistId,duration);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    static public Song toSong(Cursor cursor) {
        return new Song(
                cursor.getLong(cursor.getColumnIndex(SongTable._ID)),
                cursor.getString(cursor.getColumnIndex(SongTable.FILE_PATH)),
                cursor.getString(cursor.getColumnIndex(SongTable.TITLE)),
                cursor.getString(cursor.getColumnIndex(SongTable.ALBUM)),
                cursor.getLong(cursor.getColumnIndex(SongTable.ALBUM_ID)),
                cursor.getString(cursor.getColumnIndex(SongTable.ARTIST)),
                cursor.getLong(cursor.getColumnIndex(SongTable.ARTIST_ID)),
                cursor.getLong(cursor.getColumnIndex(SongTable.DURATION)));
    }

}
