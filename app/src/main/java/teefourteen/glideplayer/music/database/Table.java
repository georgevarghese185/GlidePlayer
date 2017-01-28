package teefourteen.glideplayer.music.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public abstract class Table {
    abstract String createTableQuery();

    abstract Cursor getMediaStoreCursor();

    abstract void initialize(SQLiteDatabase db);

    abstract ContentValues putValues(Cursor cursor);
}
