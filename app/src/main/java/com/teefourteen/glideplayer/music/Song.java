package com.teefourteen.glideplayer.music;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.teefourteen.glideplayer.music.database.AlbumTable;
import com.teefourteen.glideplayer.music.database.ArtistTable;
import com.teefourteen.glideplayer.music.database.Library;
import com.teefourteen.glideplayer.music.database.SongTable;


public class Song implements Parcelable {
    private long _id;
    private String filePath;
    private String title;
    private String album;
    private long albumId;
    private String albumArt;
    private String artist;
    private long artistId;
    private long duration;
    private String libraryUsername = Library.LOCAL_DATABASE_NAME;

    public Song(long _id, String filePath, String title, String album, long albumId,
                String albumArt, String artist, long artistId, long duration, String libraryUsername) {
        this._id = _id;
        this.filePath = filePath;
        this.title = title;
        if(album!=null && album.equals("<unknown>")) {
            this.album = "Unknown Album";
        } else {
            this.album = album;
        }
        this.albumId = albumId;
        this.albumArt = albumArt;
        if(artist!=null && artist.equals("<unknown>")) {
            this.artist = "Unknown Artist";
        } else {
            this.artist = artist;
        }
        this.artistId = artistId;
        this.duration = duration;
        this.libraryUsername = libraryUsername;
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

    public String getLibraryUsername(){
        return libraryUsername;
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

    public boolean isRemote() { return (libraryUsername != null);}

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
        dest.writeString(libraryUsername);
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
            String libraryUsername = source.readString();
            return new Song(_id, filePath, title, album, albumId, albumArt, artist, artistId,duration,
                    libraryUsername);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    static public Song toSong(Cursor cursor) {
        long _id = Library.getLong(cursor, SongTable.Columns.SONG_ID);
        String filePath = Library.getString(cursor, SongTable.Columns.FILE_PATH);
        String title = Library.getString(cursor, SongTable.Columns.TITLE);
        String album = Library.getString(cursor, AlbumTable.Columns.ALBUM_NAME);
        Long albumId = Library.getLong(cursor, SongTable.Columns.ALBUM_ID);
        String albumArt = Library.getString(cursor, AlbumTable.Columns.ALBUM_ART);
        String artist = Library.getString(cursor, ArtistTable.Columns.ARTIST_NAME);
        Long artistId = Library.getLong(cursor, SongTable.Columns.ARTIST_ID);
        Long duration = Library.getLong(cursor, SongTable.Columns.DURATION);
        String libraryUserName;
        if(Library.getInt(cursor, SongTable.Columns.IS_REMOTE) == 1) {
            libraryUserName = Library.getString(cursor, SongTable.Columns.REMOTE_USERNAME);
        } else  {
            libraryUserName = null;
        }

        return new Song(_id, filePath, title, album, albumId, albumArt, artist, artistId, duration,
                libraryUserName);
    }
}
