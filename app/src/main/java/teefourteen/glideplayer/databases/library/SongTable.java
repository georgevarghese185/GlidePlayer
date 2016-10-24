package teefourteen.glideplayer.databases.library;

import android.provider.BaseColumns;

/**
 * Created by George on 10/24/2016.
 */
public class SongTable implements BaseColumns {
    private SongTable() {}

    public static final String TABLE_NAME = "Song";
    public static final String ALBUM = "album";
    public static final String ALBUM_ID = "album_id";
    public static final String ALBUM_KEY = "album_key";
    public static final String ARTIST = "artist";
    public static final String ARTIST_ID = "artist_id";
    public static final String ARTIST_KEY = "artist_key";
    public static final String DATE_ADDED = "date_added";
    public static final String DURATION = "duration";
    public static final String FILE_PATH = "file_path";
    public static final String BOOKMARK = "bookmark";
    public static final String SIZE = "size";
    public static final String TITLE = "title";
    public static final String TITLE_KEY = "title_key";
    public static final String TRACK_NUMBER = "track_number";
    public static final String YEAR = "year";

    public static String[] getAllColumnNames() {
        String[] columns = {
                ALBUM,
                ALBUM_ID,
                ALBUM_KEY,
                ARTIST,
                ARTIST_ID,
                ARTIST_KEY,
                DATE_ADDED,
                DURATION,
                FILE_PATH,
                BOOKMARK,
                SIZE,
                TITLE,
                TITLE_KEY,
                TRACK_NUMBER,
                YEAR
        };
        return columns;
    }
}
