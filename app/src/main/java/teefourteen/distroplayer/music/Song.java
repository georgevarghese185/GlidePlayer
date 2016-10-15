package teefourteen.distroplayer.music;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.ArrayList;

/**
 * Created by george on 12/10/16.
 */
public class Song {
    private long songId;
    private String filePath;
    private String title;
    private String album;
    private long albumId;
    private String artist;
    private long artistId;

    public Song(long songId, String filePath, String title, String album, long albumId, String artist, long artistId) {
        this.songId = songId;
        this.filePath = filePath;
        this.title = title;
        this.album = album;
        this.albumId = albumId;
        this.artist = artist;
        this.artistId = artistId;
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

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    static public ArrayList<Song> getSongArrayList(ContentResolver contentResolver) {
        ArrayList<Song> songList = new ArrayList<>();

        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ARTIST_ID
        };
        Cursor musicCursor = contentResolver.query(musicUri, projection, "IS_MUSIC != 0", null, null);
        if(musicCursor==null)
            return null;
        else if(musicCursor.moveToFirst()) {
            do {
                long songId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID));
                String filePath = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
                String title = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
                String album = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM));
                long albumId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID));
                String artist = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
                long artistId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_ID));

                if(album.equals("<unknown>")) album = "Unknown Album";
                if(artist.equals("<unknown>")) artist = "Unknown Artist";

                songList.add(new Song(songId, filePath, title, album, albumId, artist, artistId));
            }while (musicCursor.moveToNext());
            musicCursor.close();
        }
        else {
            musicCursor.close();
            return null;
        }

        return songList;
    }
}