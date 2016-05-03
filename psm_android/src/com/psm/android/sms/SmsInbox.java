package com.psm.android.sms;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import com.psm.android.R;
import com.psm.android.SessionManager;
import com.psm.android.Util;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.IUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SmsInbox extends Activity implements OnItemClickListener, OnItemLongClickListener {

	private static final String SMS_IDENTIFIER = "/?";
	
	private Context mContext;
	private Handler mHandler;
	private LayoutInflater mInflater;
	
	private ListView mList;
	private InboxAdapter mAdapter;
	
	private ArrayList<String> mMessages = new ArrayList<String>();
	
	private ArrayList<ISMSMsg> mInboxMessages = new ArrayList<ISMSMsg>();
	private HashMap<Integer,Integer> mInboxCount;
	private HashMap<Integer,Integer> mUnreadCount;
	
	private BroadcastReceiver mReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sms_inbox);
		if(Util.mInsider == null)
		{
			Util.mSession.initFacebook();
			Util.mSession.initInsider();
			Util.mSession.initFoursquare();
			
			SessionManager.restoreFacebook(Util.mFacebook, this);
			SessionManager.restoreInsider(Util.mInsider, this);
			SessionManager.restoreFoursquare(Util.mFoursquare, this);
		}
		
		mInflater = getLayoutInflater();
		mContext = this;
		mHandler = new Handler();
		mList = (ListView)findViewById(R.id.sms_inbox_list);
		mAdapter = new InboxAdapter();
		mList.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
		mList.setOnItemClickListener(this);
		mList.setOnItemLongClickListener(this);
		
		Button btnCompose = (Button)findViewById(R.id.sms_inbox_compose);
		btnCompose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent(SmsInbox.this, SmsCompose.class);
				startActivity(intent);
			}
		});
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			if(extras.containsKey("com.psm.android.thread_id"))
			{
				Integer sthread_id = extras.getInt("com.psm.android.thread_id"); 
				SmsDatabase db = new SmsDatabase(this);
		        db.open();
		        db.markAllRead(sthread_id);
		        db.close();
		        
				Intent intent = new Intent(SmsInbox.this, SmsConversation.class);
				intent.putExtra("com.psm.android.thread_id", sthread_id);
				startActivity(intent);
			}
				
		}
		loadInbox();
		mAdapter.notifyDataSetChanged();
		
		//testNotification();
	}
	
	private void testNotification()
	{
		int thread_id = 3;//db.getThread_id(messages[0].getOriginatingAddress());
        NotificationManager nMn = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon, "", System.currentTimeMillis());
        Context acontext = getApplicationContext();
        CharSequence contentTitle = "New SMS Message";
        CharSequence contentText = "";
        Util.log("thread_id = " + String.valueOf(thread_id));
        Intent notificationIntent = new Intent(acontext, SmsConversation.class);
        notificationIntent.putExtra("com.psm.android.thread_id", thread_id);
        PendingIntent contentIntent = PendingIntent.getActivity(acontext, 0, notificationIntent, 0);
        notification.setLatestEventInfo(acontext, contentTitle, contentText, contentIntent);
        nMn.notify(thread_id, notification);
        
	}
	
	@Override
	protected void onResume() {
		setupBroadcastReceiver();
		if(mReceiver != null)
		{
			IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
			registerReceiver(mReceiver, filter);
		}
		
		loadInbox();
		mAdapter.notifyDataSetChanged();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if(mReceiver != null)
			unregisterReceiver(mReceiver);
		
		super.onPause();
	}
	private String[] getToInfo(String address)
	{
		String[] fields = new String[] {
		        ContactsContract.PhoneLookup.DISPLAY_NAME,
		        ContactsContract.PhoneLookup._ID,
		        ContactsContract.PhoneLookup.PHOTO_ID};
		String[] ret = new String[2];
		
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
		Cursor cursor = mContext.getContentResolver().query(uri, fields, null, null, null);
		if(cursor.moveToFirst())
		{
			ret[0] = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
			ret[1] = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
		}
		else
		{
			ret[0] = address;
		}
			return ret;
		
	}
	
	public Bitmap getContactImage(String contact_id)
	{
		Util.log("Contact Id: " + contact_id);
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contact_id));
	    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(), uri);
	    Bitmap bitmap = BitmapFactory.decodeStream(input);
	    return bitmap;
	}
	
	private void loadInbox()
	{
		//Load the top level messages
		SmsDatabase db = new SmsDatabase(this);
        db.open();
        mInboxMessages = db.getInboxMessages();
        mInboxCount = db.getInboxCount();
        mUnreadCount = db.getUnreadCount();
        db.close();
        filterMessages();
		
	}
	
	
	private class InboxAdapter extends BaseAdapter
	{

		public int getCount() {
			return mInboxMessages.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertview, ViewGroup arg2) {
			View view = convertview;
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.sms_cell_inboxmessage, null);
				ViewHolder holder = new ViewHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.sms_celli_icon);
				holder.txtName = (TextView)view.findViewById(R.id.sms_celli_name);
				holder.txtMessage = (TextView)view.findViewById(R.id.sms_celli_message);
				holder.txtTime = (TextView)view.findViewById(R.id.sms_celli_date);
				holder.txtUnread = (TextView)view.findViewById(R.id.sms_celli_txtunread);
				view.setTag(holder);
			}
			
			ViewHolder holder = (ViewHolder)view.getTag();
			ISMSMsg msg = mInboxMessages.get(position);
			int length = mInboxCount.get(msg.thread_id);
			int unread = mUnreadCount.containsKey(msg.thread_id) ? mUnreadCount.get(msg.thread_id) : 0;
			String[] info = getToInfo(msg.address);
			String name = info[0];
			//TODO: change to actual person, need to most likely do a db lookup
			if(name == null)
			{
				SmsDatabase db = new SmsDatabase(mContext);
				db.open();
				String to_address = db.getToAddress(msg.thread_id);
				db.close();
				info = getToInfo(to_address);
				name = info[0];
			}
			
			if(info[1] != null)
			{
				Bitmap bitmap = getContactImage(info[1]);
				if(bitmap != null)
					holder.imgIcon.setImageBitmap(bitmap);
				else
					holder.imgIcon.setImageResource(R.drawable.icon);
			}
			else
				holder.imgIcon.setImageResource(R.drawable.icon);
			if(unread > 0)
			{
				holder.txtUnread.setText(String.valueOf(unread));
				holder.txtUnread.setVisibility(View.VISIBLE);
				holder.txtName.setText(Html.fromHtml("<b>" + name + " (" + String.valueOf(length) + ")" + "</b>"));
				holder.txtMessage.setText(Html.fromHtml("<b>" + msg.message + "</b>"));
				holder.txtTime.setText(Util.formatSmsTime(msg.time));
			}
			else
			{
				holder.txtUnread.setVisibility(View.INVISIBLE);
				holder.txtName.setText( name + " (" + String.valueOf(length) + ")" );
				holder.txtMessage.setText( msg.message );
				holder.txtTime.setText(Util.formatSmsTime(msg.time));
			}
			return view;
		}
		
	}
	
	private class ViewHolder
	{
		int conversationId;
		ImageView imgIcon;
		TextView txtName;
		TextView txtMessage;
		TextView txtTime;
		TextView txtUnread;
	}
	
	public static class ISMSMsg
	{
		int 	id;
		int 	thread_id;
		long 	time;
		String 	address;
		String 	message;
		String 	ident; //10 digit identifier
	}
	

	public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
		Intent intent = new Intent(SmsInbox.this, SmsConversation.class);
		ISMSMsg msg = mInboxMessages.get(position);
		SmsDatabase db = new SmsDatabase(this);
        db.open();
        db.markAllRead(msg.thread_id);
        db.close();
        mAdapter.notifyDataSetChanged();
        
		intent.putExtra("com.psm.android.thread_id", msg.thread_id);
		startActivity(intent);
	}
	
	public boolean onItemLongClick(AdapterView<?> arg0, View viewConvert, int position,
			long arg3) {
		
		final ISMSMsg msg = mInboxMessages.get(position);
		String[] items = {"Delete"};
		new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0)
				{
					new AlertDialog.Builder(mContext).setTitle("Delete Conversation").setMessage("Delete this conversation?")
						.setNegativeButton("No", null).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								SmsDatabase db = new SmsDatabase(mContext);
						        db.open();
						        db.deleteThread(msg.thread_id);
						        db.close();
						        mHandler.post(new Runnable() {
									
									public void run() {
										loadInbox();
										mAdapter.notifyDataSetChanged();										
									}
								});
						        
							}
						}).create().show();
				}
			}
		}).create().show();
		return false;
	}
	
	
	private void setupBroadcastReceiver()
	{
		if(mReceiver != null)
			return;
		
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent data) {
				loadInbox();
				mAdapter.notifyDataSetChanged();
			}
		};
	}
	
	private void filterMessages()
	{
		for(ISMSMsg msg : mInboxMessages)
		{
			if(msg.message.startsWith(SMS_IDENTIFIER))
			{
				//filter out message so it doesn't look like an insid3r message
				msg.ident = msg.message.substring(2, 12);
				msg.message = msg.message.substring(12);
			}
		}
	}

	
}
