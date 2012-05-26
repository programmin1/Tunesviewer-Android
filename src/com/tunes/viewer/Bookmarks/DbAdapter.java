/* Bookmarks Database Adapter based on 
 * http://joesapps.blogspot.com/2011/02/customized-listview-data-display-in.html
 * Copyright (c) 2011 Joseph Fernandez <joefernandez.apps@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunes.viewer.Bookmarks;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {

    private static final String DB_NAME = "Bookmarks.db";
    private static final String TABLE = "tunesviewerbookmarks";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_URL = "url";
    
	private static final int DB_VERSION = 1;

	private final Context _context;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	
	private static final String CREATE_DB_TABLE_DATES = 
		"CREATE TABLE "+TABLE+" ("+ 
			COL_ID +" INTEGER PRIMARY KEY AUTOINCREMENT, "+ 
			COL_TITLE + " TEXT NOT NULL, " + 
			COL_URL + " TEXT NOT NULL" + 
			");";
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_DB_TABLE_DATES);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			// if the new version is higher than the old version, 
			// delete existing tables:
			if (newVersion > oldVersion) {
	            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
	            onCreate(db);
			} else {
				// otherwise, create the database
	            onCreate(db);
			}

		}

	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 */
	public DbAdapter(Context ctx) {
		this._context = ctx;
	}

    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(_context);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public boolean deleteTitle(long rowId) {
    	return mDb.delete(TABLE, COL_ID +
    			"=" + rowId, null) > 0;
    }
    
    public long insertItem(String title, String url, int countitems) {
    	ContentValues temp = new ContentValues();
    	temp.put(COL_TITLE, title);
    	temp.put(COL_URL, url);
    	return mDb.insert(TABLE, null, temp);
    }

    /**
     * Returns cursor to sorted bookmarks of database.
     * @return
     */
    public Cursor fetchBookmarks() {
    	Cursor cursor = 
    		mDb.query(TABLE, new String[] 
    		          {COL_ID, COL_TITLE, COL_URL}, 
    		          null, null, null, null, COL_TITLE);
    	
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Searches for a url in database, returns true if it has already been bookmarked.
     * @param _url
     * @return
     */
	public boolean hasUrl(String url) {
		Cursor c = mDb.query(TABLE, new String[] {COL_URL}, "url='"+url+"'", null, null, null, null);
		boolean result = c.moveToFirst();
		c.close();
		return result;
	}

    public void close() {
        mDbHelper.close();
    }
}
