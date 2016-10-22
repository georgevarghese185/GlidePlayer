package teefourteen.glideplayer.databases.library;

import android.provider.BaseColumns;

/**
 * Created by george on 21/10/16.
 */

public final class LibraryContract {
    private LibraryContract() {}

    public static class SongTable implements BaseColumns {
        public static final String TABLE_NAME = "Song";
        public static final String COLUMN_NAME_ALBUM = "album";
        public static final String COLUMN_NAME_ALBUM_ID = "album_id";
        public static final String COLUMN_NAME_ALBUM_KEY = "album_key";
        public static final String COLUMN_NAME_ARTIST = "artist";
        public static final String COLUMN_NAME_ARTIST_ID = "artist_id";
        public static final String COLUMN_NAME_ARTIST_KEY = "artist_key";
        public static final String COLUMN_NAME_DATE_ADDED = "date_added";
        public static final String COLUMN_NAME_DURATION = "duration";
        public static final String COLUMN_NAME_FILE_PATH = "file_path";
        public static final String COLUMN_NAME_BOOKMARK = "bookmark";
        public static final String COLUMN_NAME_SIZE = "size";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TITLE_KEY = "title_key";
        public static final String COLUMN_NAME_TRACK_NUMBER = "track_number";
        public static final String COLUMN_NAME_YEAR = "year";
        
        public static String[] getAllColumnNames() {
            String[] columns = {
            COLUMN_NAME_ALBUM,
            COLUMN_NAME_ALBUM_ID,
            COLUMN_NAME_ALBUM_KEY,
            COLUMN_NAME_ARTIST,
            COLUMN_NAME_ARTIST_ID,
            COLUMN_NAME_ARTIST_KEY,
            COLUMN_NAME_DATE_ADDED,
            COLUMN_NAME_DURATION,
            COLUMN_NAME_FILE_PATH,
            COLUMN_NAME_BOOKMARK,
            COLUMN_NAME_SIZE,
            COLUMN_NAME_TITLE,
            COLUMN_NAME_TITLE_KEY,
            COLUMN_NAME_TRACK_NUMBER,
            COLUMN_NAME_YEAR
            };
            return columns;
        }
    }
}
