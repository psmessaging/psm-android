package com.psm.android.sms;

import java.util.Date;

import com.psm.android.MainMan;
import com.psm.android.SessionManager;
import com.psm.android.Util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;

public class SmsListener extends BroadcastReceiver {

	private static final String SMS_IDENTIFIER = "/?";
	
	@Override
	public void onReceive(Context context, Intent data) {
		
		Bundle extras = data.getExtras();
		SharedPreferences prefs = context.getSharedPreferences(SessionManager.SMS_PREFS, Context.MODE_PRIVATE);
		boolean skipListen = !prefs.getBoolean("listen", true);		
		
		if (extras != null) {
            Object[] pdus = (Object[])extras.get("pdus");
            final SmsMessage[] messages = new SmsMessage[pdus.length];
            SmsDatabase db = new SmsDatabase(context);
            db.open();
            for (int i = 0; i < pdus.length; i++) {
            	boolean isISMS = false;
                messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
            	Util.log(messages[i].getOriginatingAddress());
            	Util.log(messages[i].getDisplayOriginatingAddress());
                Util.log("Message recieved: " + messages[i].getMessageBody());
                if(messages[i].getDisplayMessageBody().startsWith("//ANDROID") || messages[i].getDisplayMessageBody().startsWith("//android"))
                	continue;
                else if(messages[i].getDisplayMessageBody().startsWith(SMS_IDENTIFIER))
                {
                	if(!skipListen)
                		abortBroadcast();
                	isISMS = true;
                }
                
                if(skipListen && !isISMS)
                	continue;
                
                db.addTextMessage(messages[i].getDisplayMessageBody(), messages[i].getOriginatingAddress());
                
                int thread_id = db.getThread_id(messages[i].getOriginatingAddress());
                NotificationManager nMn = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new Notification(android.R.drawable.sym_action_email, "New SMS Message", System.currentTimeMillis());
                Context acontext = context.getApplicationContext();
                String name = getToName(messages[i].getOriginatingAddress(), acontext);
                CharSequence contentTitle = "New SMS Message";
                CharSequence contentText = "Click to view message.";
                Util.log("thread_id = " + String.valueOf(thread_id));
                Intent notificationIntent = new Intent(acontext, MainMan.class);
                notificationIntent.putExtra("com.psm.android.thread_id", thread_id);
                PendingIntent contentIntent = PendingIntent.getActivity(acontext, 0, notificationIntent, 0);
                notification.setLatestEventInfo(acontext, contentTitle, contentText, contentIntent);
                nMn.notify("sms", thread_id, notification);
            
            }
            db.close();
            Intent newbroad = new Intent("com.psm.android.SMS_PROC");
            context.sendBroadcast(newbroad);
        }
		
	}

	private String getToName(String phone, Context context)
	{
		String[] fields = new String[] {
		        ContactsContract.PhoneLookup.DISPLAY_NAME,
		        ContactsContract.PhoneLookup._ID};
		
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
		Cursor cursor = context.getContentResolver().query(uri, fields, null, null, null);
		if(cursor.moveToFirst())
			return cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
		else
			return phone;
		
	}
}
