package com.teefourteen.glideplayer.music.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public abstract class Table {
    public String TABLE_NAME;

    public interface RemoteColumns {
        String IS_REMOTE = "is_remote";
        String REMOTE_USERNAME = "libowner";
    }

    Table(String tableName) {
        TABLE_NAME = tableName;
    }

    abstract String createTableQuery();

    Cursor getFullTable(SQLiteDatabase db) {
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    abstract Cursor getMediaStoreCursor();

    void initialize(SQLiteDatabase db) {
        db.delete(TABLE_NAME, null, null);

        Cursor cursor = getMediaStoreCursor();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ContentValues values = putValues(cursor);

                insertValues(values, db);
            } while (cursor.moveToNext());

            cursor.close();
        }
    }

    abstract ContentValues putValues(Cursor cursor);

    void insertValues(ContentValues values, SQLiteDatabase db) {
        db.insert(TABLE_NAME, null, values);
    }
}
