/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.micode.fileexplorer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 存储favorite数据，到数据库 SQLiteOpenHelper是一个帮助管理数据库和版本的工具类。
 * 通过继承并重载方法，快速实现了我们自己的Favorite表的CRUD。
 * 怎么感觉和FileOperationHelper类似，仍然是CRUD，只不过1个是数据库中的，1个是文件的。
 */
public class FavoriteDatabaseHelper extends SQLiteOpenHelper {
	// 下面6个字段是数据库的名字和版本号、表的名字和3个字段
	private final static String DATABASE_NAME = "file_explorer";

	private final static int DATABASE_VERSION = 1;

	private final static String TABLE_NAME = "favorite";

	public final static String FIELD_ID = "_id";

	public final static String FIELD_TITLE = "title";

	public final static String FIELD_LOCATION = "location";

	private boolean firstCreate;
	// 数据库变化的时候，会通知其它监听器
	private FavoriteDatabaseListener mListener;

	private static FavoriteDatabaseHelper instance;

	public interface FavoriteDatabaseListener {
		void onFavoriteDatabaseChanged();
	}

	// 这个构造方法和下面的静态获得实例的方法，不太和谐啊~
	// 乍一看，以为是单例模式呢，实则不是~
	public FavoriteDatabaseHelper(Context context,
			FavoriteDatabaseListener listener) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		instance = this;
		mListener = listener;
	}

	// 这个地方感觉只是方便存储了一个类的实例，但不能保证这个类只有1个实例
	public static FavoriteDatabaseHelper getInstance() {
		return instance;
	}

	// 数据库创建，1个sql
	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "Create table " + TABLE_NAME + "(" + FIELD_ID
				+ " integer primary key autoincrement," + FIELD_TITLE
				+ " text, " + FIELD_LOCATION + " text );";
		db.execSQL(sql);
		firstCreate = true;
	}

	// 升级的时候，直接删除以前的数据库，如果存在的话
	// 版本号，没用上啊
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String sql = " DROP TABLE IF EXISTS " + TABLE_NAME;
		db.execSQL(sql);
		onCreate(db);
	}

	// 是否为第1次创建
	public boolean isFirstCreate() {
		return firstCreate;
	}

	// 判断1个文件路径是否已经存在，或者说是否是Favorite文件
	public boolean isFavorite(String path) {
		String selection = FIELD_LOCATION + "=?";
		String[] selectionArgs = new String[] { path };
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs,
				null, null, null);
		if (cursor == null)
			return false;
		boolean ret = cursor.getCount() > 0;
		cursor.close();
		return ret;
	}

	// 获得Favorite表的游标
	public Cursor query() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db
				.query(TABLE_NAME, null, null, null, null, null, null);
		return cursor;
	}

	// 插入一条记录
	public long insert(String title, String location) {
		if (isFavorite(location))
			return -1;

		SQLiteDatabase db = this.getWritableDatabase();
		long ret = db.insert(TABLE_NAME, null, createValues(title, location));
		mListener.onFavoriteDatabaseChanged();
		return ret;
	}
	// 根据id，删除一条记录。如果需要，然后通知相关监听器
	public void delete(long id, boolean notify) {
		SQLiteDatabase db = this.getWritableDatabase();
		String where = FIELD_ID + "=?";
		String[] whereValue = { Long.toString(id) };
		db.delete(TABLE_NAME, where, whereValue);

		if (notify)
			mListener.onFavoriteDatabaseChanged();
	}

	// 根据位置删除1条记录，一定通知相关监听器
	public void delete(String location) {
		SQLiteDatabase db = this.getWritableDatabase();
		String where = FIELD_LOCATION + "=?";
		String[] whereValue = { location };
		db.delete(TABLE_NAME, where, whereValue);
		mListener.onFavoriteDatabaseChanged();
	}

	//更新1条记录 
	public void update(int id, String title, String location) {
		SQLiteDatabase db = this.getWritableDatabase();
		String where = FIELD_ID + "=?";
		String[] whereValue = { Integer.toString(id) };
		db.update(TABLE_NAME, createValues(title, location), where, whereValue);
		mListener.onFavoriteDatabaseChanged();
	}

	private ContentValues createValues(String title, String location) {
		ContentValues cv = new ContentValues();
		cv.put(FIELD_TITLE, title);
		cv.put(FIELD_LOCATION, location);
		return cv;
	}
}
