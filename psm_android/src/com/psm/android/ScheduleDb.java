package com.psm.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;



public class ScheduleDb {

	private final Context mContext;
	private static final String DB_NAME = "schedule";
	private static final int DB_VER 	= 1;
	
	private SDbHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	
	public ScheduleDb(Context context) {
		this.mContext = context;
	}
	
	public ScheduleDb open() throws SQLException
	{
		mDbHelper = new SDbHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close()
	{
		mDb.close();
	}
	
	/***
	 * 
	 * 
	 * @author Sean Lyons
	 *
	 */
	public ArrayList<String> readyToSend()
	{
		ArrayList<String> ids = new ArrayList<String>();
		long now = System.currentTimeMillis();
		Cursor c = mDb.rawQuery("SELECT id,time,message,to_address FROM schedule WHERE time < ?", new String[] {String.valueOf(now)});
		while(c.moveToNext())
		{
			Util.log(Arrays.toString(c.getColumnNames()));
			String id = String.valueOf(c.getInt(c.getColumnIndex("id")));
			ids.add(id);
			Util.log(id);
		}
		return ids;
	}
	
	public ArrayList<String> waitingToSend()
	{
		ArrayList<String> ids = new ArrayList<String>();
		long now = System.currentTimeMillis();
		Cursor c = mDb.rawQuery("SELECT id,time,message,to_address FROM schedule WHERE time > ? ORDER BY time ASC", new String[] {String.valueOf(now)});
		while(c.moveToNext())
		{
			String id = String.valueOf(c.getInt(c.getColumnIndex("id")));
			ids.add(id);
		}
		return ids;
	}
	
	public ArrayList<Bundle> waitingToSendSms()
	{
		ArrayList<Bundle> messages = new ArrayList<Bundle>();
		ArrayList<String> readies = waitingToSend();
		
		for(String id : readies)
		{
			Bundle bundle = getMessage(id);
			messages.add(bundle);
		}
		
		return messages;
	}
	
	
	public int countReadyToSend()
	{
		long now = System.currentTimeMillis();
		int waiting = 0;
		Cursor c = mDb.rawQuery("SELECT count(id) as count FROM schedule where time < ?", new String[] {String.valueOf(now)});
		if(c.moveToFirst())
		{
			waiting = c.getInt(c.getColumnIndex("count"));
		}
		return waiting;
	}
	
	public int countWaitingMessages()
	{
		
		long now = System.currentTimeMillis();
		int waiting = 0;
		Cursor c = mDb.rawQuery("SELECT count(id) as count FROM schedule where time > ?", new String[] {String.valueOf(now)});
		if(c.moveToFirst())
		{
			waiting = c.getInt(c.getColumnIndex("count"));
		}
		return waiting;
	}
	public void scheduleMessage(String message, long time, String to_address)
	{
		SQLiteStatement stmt = mDb.compileStatement("INSERT INTO schedule " +
				"(time, message, to_address)" +
				" VALUES " + 
				" (?,?,?) ");
		stmt.bindLong(1, time);
		stmt.bindString(2, message);
		stmt.bindString(3, to_address.replaceAll("[-()]", ""));
		stmt.execute();
		
	}
	
	public Bundle getMessage(String id)
	{
		Bundle bundle = new Bundle();
		Cursor c = mDb.rawQuery("SELECT id, message, to_address, time FROM schedule where id = ?", new String[] {id});
		if(c.moveToFirst())
		{
			bundle.putString("id", c.getString(c.getColumnIndex("id")));
			bundle.putString("message", c.getString(c.getColumnIndex("message")));
			bundle.putString("address", c.getString(c.getColumnIndex("to_address")));
			bundle.putLong("time", c.getLong(c.getColumnIndex("time")));
		}
		return bundle;
	}
	
	public void deleteMessage(String id)
	{
		mDb.execSQL("DELETE FROM schedule WHERE id = " + id);
	}
	private class SDbHelper extends SQLiteOpenHelper
	{

		public SDbHelper(Context context) {
			super(context, DB_NAME, null, DB_VER);
			
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			
			db.execSQL("CREATE TABLE schedule" +
					"(id INTEGER NOT NULL," +
					"time INTEGER DEFAULT 0," +
					"message VARCHAR(255)," +
					"network INTEGER," +
					"to_address VARCHAR(25)," +
					"PRIMARY KEY (id))");
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}
		
	}
}
