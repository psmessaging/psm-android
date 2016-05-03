package com.psm.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class CountryCodeDatabase {

	private final Context mContext;

	private static final String DB_NAME = "/data/data/com.psm.android/databases/ccodesdb4";
	private static final int DB_VER 	= 1;
	
	private CCDbHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	
	public CountryCodeDatabase(Context context)
	{
		this.mContext = context;
	}
	
	public CountryCodeDatabase open() throws SQLException
	{
		File dst = new File(DB_NAME);
		if(!dst.exists())
		{
			try {
				InputStream in = mContext.getAssets().open("ccode.lite");
				OutputStream out = new FileOutputStream(dst);
			    byte[] buf = new byte[1024];
			    int len;
			    while ((len = in.read(buf)) > 0) {
			        out.write(buf, 0, len);
			    }
			    in.close();
			    out.close();
			}catch(Exception ex) {}
		}

		mDb = SQLiteDatabase.openDatabase(DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
	    
		return this;
	}
	
	public void close()
	{
		mDb.close();
	}
	
	
	public ArrayList<CCItem> getCountry(String country)
	{
		ArrayList<CCItem> array = new ArrayList<CCItem>();
		
		Cursor c = mDb.rawQuery("SELECT country, country_abrev, country_code FROM ccodes WHERE country LIKE '" + country + "%' OR country_abrev LIKE '" + country + "'", null);
		
		if(c.moveToFirst())
		{
			do
			{
				CCItem item = new CCItem();
				item.prefix = c.getString(c.getColumnIndex("country_abrev"));
				item.country = c.getString(c.getColumnIndex("country"));
				item.code = c.getString(c.getColumnIndex("country_code"));
				array.add(item);
				
			}while(c.moveToNext());
		}
		
		return array;
	}
	
	private static class CCDbHelper extends SQLiteOpenHelper
	{

		private Context mContext;
		
		public CCDbHelper(Context context)
		{
			super(context, DB_NAME, null, DB_VER);
			mContext = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			String path = db.getPath();
			Util.log(path);
			
			try {
				
			}catch(Exception ex) { Util.log("error copying db: " + ex.getMessage());}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {			
		}
		
	}
	
	public static class CCItem
	{
		String country;
		String code;
		String prefix;
		
	}
}
