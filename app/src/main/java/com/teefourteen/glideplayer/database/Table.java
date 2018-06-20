/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.database;

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

    Cursor getFullTable(SQLiteDatabase db, String condition) {
        if(condition == null || condition.isEmpty()) return getFullTable(db);
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + condition, null);
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
