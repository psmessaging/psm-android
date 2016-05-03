package com.psm.android;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.psm.android.R;
import com.psm.android.sms.SmsDatabase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.SmsManager;

public class ScheduleMsgService extends Service {

	NotificationManager mNM;
	private Context mContext;
	private Timer mTimer;
	private int mWaiting = 0;
	
	private Object sync_lock = null;
	
	@Override
	public void onCreate() {
		
		Util.log("Start Service");
		
		ScheduleDb mDb = new ScheduleDb(this);
		mDb.open();
		int wait = mDb.countWaitingMessages();
		int ready = mDb.countReadyToSend();
		mWaiting = wait;
		mDb.close();
		
		
		
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		if(wait > 0)
		{
			showNotification();
		}
		
		mContext = this;
		
		
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				checkMessages();
			}
		};
		
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(task, 1*1000, 60*1000);
	}
	
	public void checkMessages()
	{
		ScheduleDb mDb = new ScheduleDb(mContext);
		mDb.open();
		int wait = mDb.countWaitingMessages();
		Util.log("Waiting: " + String.valueOf(wait));
		
		int ready = mDb.countReadyToSend();
		Util.log("Ready: " + String.valueOf(ready));
		if(wait == 0 && ready == 0)
		{
			stopSelf();
		}
		else
		{
			ArrayList<String> readies = mDb.readyToSend();
			sendMessages(readies);
			wait = mDb.countWaitingMessages();
			if(wait == 0)
				stopSelf();
			else
			{
				mWaiting = wait;
				showNotification();
			}
		}
		mDb.close();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//if(mWaiting > 0)
			//showNotification();
		checkMessages();
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void sendMessages(ArrayList<String> ids)
	{
		
		ScheduleDb mDb = new ScheduleDb(this);
		mDb.open();
			for(String id : ids)
			{
				Util.log("Sending message: " + id);
				SmsManager manager = SmsManager.getDefault();
				Bundle msg = mDb.getMessage(id);
				
				
				manager.sendTextMessage(msg.getString("address"), null, 
								msg.getString("message"), null, null);
				
				SmsDatabase db = new SmsDatabase(this);
				db.open();
				int thread_id = db.getThread_id(msg.getString("address"));
				db.addMeMessageToThread(msg.getString("message"), thread_id);
				db.close();
				
				mDb.deleteMessage(id);
			}
		
		mDb.close();
	}
		
	Runnable mTask = new Runnable() {
		public void run() {
			
		}
	};
	
	
	
	@Override
	public void onDestroy() {
		Util.log("Stop Service");
		mTimer.cancel();
		mNM.cancel(4);
	}
	@Override
	public IBinder onBind(Intent arg0) {
		
		return null;
	}

	 private void showNotification() {
	        // In this sample, we'll use the same text for the ticker and the expanded notification
		 
	        CharSequence text = String.valueOf(mWaiting) + " insid3r Scheduled " + (mWaiting == 1 ? "Message." : "Messages.");

	        // Set the icon, scrolling text and timestamp
	        Notification notification = new Notification(R.drawable.icon_t, text,
	                0);

	        // The PendingIntent to launch our activity if the user selects this notification
	        Intent intent = new Intent(this, MainMan.class);
	        intent.putExtra("com.psm.android.smslist", true);
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	                intent, 0);

	        // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(this, "insid3r",
	                       text, contentIntent);
	        notification.flags |= Notification.FLAG_ONGOING_EVENT;  
	        notification.flags |= Notification.FLAG_NO_CLEAR;
	        // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        mNM.notify(4, notification);
	    }

	 private final IBinder mBinder = new Binder() {
	        @Override
	                protected boolean onTransact(int code, Parcel data, Parcel reply,
	                        int flags) throws RemoteException {
	            return super.onTransact(code, data, reply, flags);
	        }
	    };

    public static class ScheduledMessage
    {
    	public final static int TYPE_FACEBOOK_STATUS = 1;
    	public final static int TYPE_FACEBOOK_CHECKIN = 2;
    	public final static int TYPE_SMS = 3;
    	public final static int TYPE_FOURSQUARE = 4;
    	private int Type;
    }
}
