package teefourteen.glideplayer.music.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public abstract class Table {
    String TABLE_NAME;

    Table(String tableName) {
        TABLE_NAME = tableName;
    }

    abstract String createTableQuery();

    Cursor getFullTable(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    abstract Cursor getMediaStoreCursor();

    abstract void initialize(SQLiteDatabase db);

    abstract ContentValues putValues(Cursor cursor);

    void insertValues(ContentValues values, SQLiteDatabase db) {
        db.insert(TABLE_NAME, null, values);
    }
}
