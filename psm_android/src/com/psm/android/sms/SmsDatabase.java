package com.psm.android.sms;

import java.sql.SQLData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;


import com.psm.android.Util;
import com.psm.android.sms.SmsInbox.ISMSMsg;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class SmsDatabase  {
	
	private final Context mContext;
	private static final String DB_NAME = "smstest6";
	private static final int DB_VER 	= 1;
	
	private SmsDbHeler mDbHelper;
	private SQLiteDatabase mDb;
	
	public SmsDatabase(Context context) {
		this.mContext = context;
	}
	
	public SmsDatabase open() throws SQLException
	{
		mDbHelper = new SmsDbHeler(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close()
	{
		mDb.close();
	}
	
	public void addMeMessageToThread(String message, int thread_id)
	{
		SQLiteStatement stmt = mDb.compileStatement("INSERT INTO sms " +
				"(thread_id, date, read, body)" +
				" VALUES " + 
				" (?,?,?,?) ");
		stmt.bindLong(1, thread_id);
		stmt.bindLong(2, new Date().getTime());
		stmt.bindLong(3, 1);
		stmt.bindString(4, message);
		
		stmt.execute();
	}
	
	public boolean addTextMessage(String message, String address)
	{
		return addTextMessage(message, address, false);
		
	}
	public boolean addTextMessage(String message, String address, boolean meSwitch)
	{
		//TODO: Store reversed phone and do LIKE instead of EQUALS clause
		
		int thread_id = 0;
		Cursor c = mDb.rawQuery("SELECT _id, thread_id FROM sms WHERE address = ?", new String[] {address});
		if(c.moveToFirst())
		{
			Util.log(Arrays.toString(c.getColumnNames()));
			thread_id = (int)c.getLong(c.getColumnIndex("thread_id"));
			Util.log("using thread id: " + String.valueOf(thread_id));
		}
		
		if(thread_id == 0)
		{
			c = mDb.rawQuery("SELECT value FROM smssupport WHERE property = 1", null);
			if(c.moveToFirst())
			{
				thread_id = c.getInt(c.getColumnIndex("value"))+1;
				Util.log("new thread id: " + String.valueOf(thread_id));
				SQLiteStatement stmt = mDb.compileStatement("UPDATE smssupport SET value= " + String.valueOf(thread_id) + " WHERE property = 1" );
				stmt.execute();
			}
					
		}
		SQLiteStatement stmt = mDb.compileStatement("INSERT INTO sms " +
				"(thread_id, address, date, read, body, meswitch)" +
				" VALUES " + 
				" (?,?,?,?,?,?) ");
		stmt.bindLong(1, thread_id);
		stmt.bindString(2, address);
		stmt.bindLong(3, new Date().getTime());
		stmt.bindLong(4, 0);
		stmt.bindString(5, message);
		stmt.bindLong(6, meSwitch ? 1 : 0);
		stmt.execute();
		
		Cursor r = mDb.rawQuery("SELECT Count(_id) FROM sms", null);
		Util.log(r.moveToFirst());
		Util.log(r.getColumnCount());
		Util.log("result: " + r.getInt(0));
		Util.log("result: " + r.getString(0));
		return true;
	}
	
	public void deleteTextMessage(int _id)
	{
		mDb.execSQL("DELETE FROM sms WHERE _id = " + _id);
	}
	
	public void deleteThread(int thread_id)
	{
		mDb.execSQL("DELETE FROM sms WHERE thread_id = " + thread_id);
	}
	
	public int getThread_id(String address)
	{
		Cursor r = mDb.rawQuery("SELECT thread_id FROM sms WHERE address = " + address + " LIMIT 1", null);
		if(r.moveToFirst())
		{
			return r.getInt(0);
		}
		
		return 0;
	}
	
	public String getToAddress(int thread_id)
	{
		Cursor r = mDb.rawQuery("SELECT address FROM sms WHERE thread_id = " + String.valueOf(thread_id) + 
								" AND address NOT NULL GROUP BY thread_id", null);
		if(r.moveToFirst())
			return r.getString(r.getColumnIndex("address"));
		else
			return null;
		
	}
	public ArrayList<ISMSMsg> getInboxMessages()
	{
		ArrayList<ISMSMsg> array = new ArrayList<ISMSMsg>();
		
		Cursor c = mDb.rawQuery("SELECT _id, thread_id, address, body, date FROM sms GROUP BY thread_id ORDER BY date DESC", null);
		if(c.moveToFirst())
		{
			do{
				ISMSMsg msg = new ISMSMsg();
				msg.id = c.getInt(c.getColumnIndex("_id"));
				msg.address = c.getString(c.getColumnIndex("address"));
				msg.thread_id = c.getInt(c.getColumnIndex("thread_id"));
				msg.message = c.getString(c.getColumnIndex("body"));
				msg.time = c.getLong(c.getColumnIndex("date"));
				array.add(msg);
			}while(c.moveToNext());
		}
		
		return array;
	}
	
	public ArrayList<ISMSMsg> getConversation(int thread_id)
	{
		ArrayList<ISMSMsg> array = new ArrayList<ISMSMsg>();
		
		Cursor c = mDb.rawQuery("SELECT _id, thread_id, address, body, meswitch," +
				"date FROM sms WHERE thread_id = " + String.valueOf(thread_id) + " ORDER BY date", null);
		if(c.moveToFirst())
		{
			do{
				ISMSMsg msg = new ISMSMsg();
				msg.id = c.getInt(c.getColumnIndex("_id"));
				msg.address = c.getString(c.getColumnIndex("address"));
				msg.thread_id = c.getInt(c.getColumnIndex("thread_id"));
				msg.message = c.getString(c.getColumnIndex("body"));
				msg.time = c.getLong(c.getColumnIndex("date"));
				int meswitch = c.getInt(c.getColumnIndex("meswitch"));
				if(meswitch == 1)
					msg.address = null;
				
				array.add(msg);
			}while(c.moveToNext());
		}
		
		return array;
	}
	
	public HashMap<Integer, Integer> getInboxCount()
	{
		HashMap<Integer, Integer> array = new HashMap<Integer, Integer>();
		Cursor c = mDb.rawQuery("SELECT Count(_id) AS cnt, thread_id FROM sms GROUP BY thread_id ORDER BY date", null);
		if(c.moveToFirst())
		{
			do{
				array.put(c.getInt(c.getColumnIndex("thread_id")),c.getInt(c.getColumnIndex("cnt")));
			}while(c.moveToNext());
		}
		return array;
	}
	
	public HashMap<Integer, Integer> getUnreadCount()
	{
		HashMap<Integer, Integer> array = new HashMap<Integer, Integer>();
		Cursor c = mDb.rawQuery("SELECT Count(_id) AS cnt, thread_id FROM sms WHERE read=0 GROUP BY thread_id ORDER BY date", null);
		if(c.moveToFirst())
		{
			do{
				array.put(c.getInt(c.getColumnIndex("thread_id")),c.getInt(c.getColumnIndex("cnt")));
			}while(c.moveToNext());
		}
		return array;
	}
	
	public void markAllRead(int thread_id)
	{
		//mDb.execSQL("UPDATE sms Set read=1 WHERE thread_id = " )
		Cursor c = mDb.rawQuery("UPDATE sms SET read=1 WHERE thread_id = ?", new String[] { String.valueOf(thread_id) });
		Util.log(c.moveToFirst());
	}
	
	private static class SmsDbHeler extends SQLiteOpenHelper
	{

		public SmsDbHeler(Context context) {
			super(context, DB_NAME, null, DB_VER);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Util.log("create database");
			db.execSQL("CREATE TABLE sms " +
			"(\"_id\" INTEGER NOT NULL," +
			"thread_id INTEGER," +
			"address VARCHAR(55)," +
			"person INTEGER," +
			"date INTEGER," +
			"read INTEGER DEFAULT 0 NOT NULL," +
			"body VARCHAR(255)," +
			"meswitch INTEGER DEFAULT 0 NOT NULL," +
			"PRIMARY KEY (\"_id\"))");
			db.execSQL("CREATE TABLE smssupport (property INTEGER UNIQUE," +
						"value INTEGER)");
			db.execSQL("INSERT INTO smssupport (property, value) VALUES (1, 1)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
	}
}
