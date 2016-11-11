package teefourteen.glideplayer.music;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore.Audio.AudioColumns;

import teefourteen.glideplayer.Library;
import teefourteen.glideplayer.fragments.AlbumsFragment;

/**
 * Created by george on 12/10/16.
 */
public class Song implements Parcelable{
    private long _id;
    private String filePath;
    private String title;
    private String album;
    private long albumId;
    private String albumArt;
    private String artist;
    private long artistId;
    private long duration;

    public Song(long _id, String filePath, String title, String album, long albumId, String albumArt, String artist, long artistId, long duration) {
        this._id = _id;
        this.filePath = filePath;
        this.title = title;
        this.album = album;
        this.albumId = albumId;
        this.albumArt = albumArt;
        this.artist = artist;
        this.artistId = artistId;
        this.duration = duration;
    }

    public long get_id() {
        return _id;
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

    public String getAlbumArt() {
        return albumArt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(_id);
        dest.writeString(filePath);
        dest.writeString(title);
        dest.writeString(album);
        dest.writeLong(albumId);
        dest.writeString(albumArt);
        dest.writeString(artist);
        dest.writeLong(artistId);
        dest.writeLong(duration);
    }

    static public Parcelable.Creator CREATOR = new Parcelable.Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel source) {
            long _id = source.readLong();
            String filePath = source.readString();
            String title = source.readString();
            String album = source.readString();
            long albumId = source.readLong();
            String albumArt  = source.readString();
            String artist = source.readString();
            long artistId = source.readLong();
            long duration = source.readLong();
            return new Song(_id, filePath, title, album, albumId, albumArt, artist, artistId,duration);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    static public Song toSong(Cursor cursor) {
        long _id = Library.getLong(cursor,AudioColumns._ID);
        String filePath = Library.getString(cursor,AudioColumns.DATA);
        String title = Library.getString(cursor,AudioColumns.TITLE);
        String album = Library.getString(cursor,AudioColumns.ALBUM);
        Long albumId = Library.getLong(cursor,AudioColumns.ALBUM_ID);
        String albumArt = Library.getAlbumArt(albumId,AlbumsFragment.albumArtDb);
        String artist = Library.getString(cursor,AudioColumns.ARTIST);
        Long artistId = Library.getLong(cursor,AudioColumns.ARTIST_ID);
        Long duration = Library.getLong(cursor,AudioColumns.DURATION);

        return new Song(_id, filePath, title, album, albumId, albumArt, artist, artistId, duration);
    }

}
