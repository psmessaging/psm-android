package com.psm.android.sms;

import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.ScheduleDb;
import com.psm.android.ScheduleMsgService;
import com.psm.android.Util;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.fb.FacebookNav.MyGestureDetector;
import com.psm.android.sms.SmsInbox.ISMSMsg;
import com.psm.util.IUtil;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemLongClickListener;

public class SmsConversation extends Activity implements OnItemClickListener, OnItemLongClickListener, OnScrollListener{

	private static final String MAX_LENGTH = "140";
	
	private static final String SMS_IDENTIFIER = "/?";
	private LayoutInflater mInflater;
	private ListView mList;
	private ConvListAdapter mAdapter;
	
	private View mFooter, mStealthFooter;
	private Handler mHandler;
	
	private ArrayList<ISMSMsg> mMessages;
	private int thread_id;
	private String to_address;
	private String to_name;
	
	private EditText mTxtMessage, mStealthMessage;
	
	private Context mContext;
	
	private BroadcastReceiver mReceiver;
	private int mOffset = 0;
	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
	
	private ArrayList<String> mIdentList = new ArrayList<String>();
	private HashMap<String, String> mInsideHash = new HashMap<String, String>();
	private ArrayList<String> mCheckedIds = new ArrayList<String>();
	private ArrayList<String> mPositions = new ArrayList<String>();
	private Bitmap to_Bitmap = null;
	
	private String to_ContactId = null;
	
	private TextView lastShownView = null;
	private boolean isLoggedin = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sms_conversation);
		mContext = this;
		mHandler = new Handler();
		
		Bundle extras = getIntent().getExtras();
		if(extras == null)
		{	
			finish();
			return;
		}
		else
		{
			if(extras.containsKey("com.psm.android.thread_id"))
			{
				thread_id = extras.getInt("com.psm.android.thread_id");
				NotificationManager nMn = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
				nMn.cancel("sms", thread_id);
			}
			else
			{
				finish();
				return;
			}
		}
		mInflater = getLayoutInflater();
		mList = (ListView)findViewById(R.id.sms_conv_list);
		mAdapter = new ConvListAdapter();
		gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        mList.setOnTouchListener(gestureListener);
        mList.setOnScrollListener(this);
        mList.setOnItemLongClickListener(this);
        
		addFooter();
		
		mList.setAdapter(mAdapter);
		
		SmsDatabase db = new SmsDatabase(this);
		db.open();
		to_address = db.getToAddress(thread_id);
		getToName();
		db.close();
		
		if(to_ContactId != null)
			to_Bitmap = getContactImage(to_ContactId);
		
		getConversation();
		mAdapter.notifyDataSetChanged();
		mList.setSelection(mMessages.size());
		
	}
	
	@Override
	protected void onResume() {
		
		setupBroadcastReceiver();
		if(mReceiver != null)
		{
			IntentFilter filter = new IntentFilter("com.psm.android.SMS_PROC");
			registerReceiver(mReceiver, filter); 
		}
		
		new Handler().postDelayed(new Runnable() {
			
			public void run() {
				((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(mTxtMessage.getWindowToken(), 0);
			}
		}, 100);

		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if(mReceiver != null)
			unregisterReceiver(mReceiver);
		super.onPause();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Util.log(mTxtMessage.getText().toString().length());
		return super.onKeyDown(keyCode, event);
	}
	
	private void getToName()
	{
		String[] fields = new String[] {
		        ContactsContract.PhoneLookup.DISPLAY_NAME,
		        ContactsContract.PhoneLookup._ID};
		
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(to_address));
		Cursor cursor = mContext.getContentResolver().query(uri, fields, null, null, null);
		if(cursor.moveToFirst())
		{
			to_name = 	cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
			to_ContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
		}
		else
			to_name = null;
		
	}
	private void getConversation()
	{
		isLoggedin = Util.mInsider.isLoggedIn();
		
		SmsDatabase db = new SmsDatabase(this);
		db.open();
		mMessages = db.getConversation(thread_id);
		db.markAllRead(thread_id);
		db.close();
		filterIds();
		translateMessages();
	}
	
	private void addFooter()
	{
		mFooter = mInflater.inflate(R.layout.sms_footer, null);
		FooterHolder holder = new FooterHolder();
		holder.btnSend = (Button)mFooter.findViewById(R.id.sms_conv_send);
		holder.btnSend.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				if(mTxtMessage.getText().length() == 0)
				{
					new AlertDialog.Builder(mContext).setMessage("Can't send nothing.")
						.setPositiveButton("Ok", null).create().show();
				}
				else
				{
					if(mStealthMessage.getText().length() > 0)
						sendStealthSMS();
					else
						sendSMS();
				}
				
			}
		});
		holder.txtMsg = (EditText)mFooter.findViewById(R.id.sms_conv_text);
		holder.txtSMsg = (EditText)mFooter.findViewById(R.id.sms_conv_text_secure);
		holder.btnSecure = (ImageButton)mFooter.findViewById(R.id.sms_conv_secure);
		holder.btnNonSecure = (ImageButton)mFooter.findViewById(R.id.sms_conv_nsecure);
		if(!Util.mInsider.isLoggedIn())
		{
			holder.btnNonSecure.setVisibility(View.GONE);
			holder.btnSecure.setVisibility(View.GONE);
		}
		else
		{
			holder.btnNonSecure.setVisibility(View.VISIBLE);
			holder.btnSecure.setVisibility(View.VISIBLE);
		}
			
		holder.txtLength = (TextView)mFooter.findViewById(R.id.sms_conv_length);
		holder.txtSMsg.setVisibility(View.GONE);
		mFooter.setTag(holder);
		mTxtMessage = holder.txtMsg;
		mStealthMessage = holder.txtSMsg;
		final ImageButton sec = holder.btnSecure;
		final ImageButton nsec = holder.btnNonSecure;
		
		holder.btnSecure.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mStealthMessage.setVisibility(View.VISIBLE);
				nsec.setBackgroundColor(Color.WHITE);
				sec.setBackgroundColor(Color.parseColor("#AAAAAA"));
				Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
						Animation.RELATIVE_TO_SELF, 0.0f, 
						Animation.RELATIVE_TO_SELF, 0.0f, 
						Animation.RELATIVE_TO_SELF, 0.0f);
				animation.setDuration(400);
				animation.setFillAfter(true);
				//animation.setFillBefore(true);
				animation.setFillEnabled(true);
				mStealthMessage.setAnimation(animation);
				mStealthMessage.requestFocus();
			}
		});
		
		holder.btnNonSecure.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mStealthMessage.setVisibility(View.GONE);
				sec.setBackgroundColor(Color.WHITE);
				nsec.setBackgroundColor(Color.parseColor("#AAAAAA"));
				Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
						Animation.RELATIVE_TO_SELF, 1.0f, 
						Animation.RELATIVE_TO_SELF, 0.0f, 
						Animation.RELATIVE_TO_SELF, 0.0f);
				animation.setDuration(400);
				animation.setFillAfter(true);
				//animation.setFillBefore(true);
				animation.setFillEnabled(true);
				mStealthMessage.setAnimation(animation);
				mTxtMessage.requestFocus();
			}
		});
		
		
		
		
		
		mTxtMessage.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				FooterHolder holder = (FooterHolder)mFooter.getTag();
				int length = mTxtMessage.getText().length();
				//holder.txtLength.setText(String.valueOf(length) + " / 160");
				//mList.setSelection(mMessages.size());
				//mTxtMessage.requestFocus();
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {				
			}
			
			public void afterTextChanged(Editable s) {
				FooterHolder holder = (FooterHolder)mFooter.getTag();
				int length = s.length();
				holder.txtLength.setText(String.valueOf(length) + " / 160");
				if(length > 0)
					holder.btnSend.setEnabled(true);
				else
					holder.btnSend.setEnabled(false);
				
				holder.txtLength.requestFocus();
			}
		});
		
		/*holder.txtMsg.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				FooterHolder holder = (FooterHolder)mFooter.getTag();
				int length = holder.txtMsg.getText().length();
				
				holder.txtLength.setText(String.valueOf(length) + " / 160");
				if(length > 0)
					holder.btnSend.setEnabled(true);
				else
					holder.btnSend.setEnabled(false);
				mList.setSelection(mMessages.size());
				mList.setSelected(false);
				v.requestFocus();
				return false;
			}
		});*/
		
		mList.addFooterView(mFooter);
	}
	
	
	
	private void addStealthFooter()
	{
		mStealthFooter = mInflater.inflate(R.layout.sms_footer, null);
		FooterHolder holder = new FooterHolder();
		holder.btnSend = (Button)mFooter.findViewById(R.id.sms_conv_send);
		holder.btnSend.setVisibility(View.GONE);
		holder.txtMsg = (EditText)mFooter.findViewById(R.id.sms_conv_text);
		holder.txtLength = (TextView)mFooter.findViewById(R.id.sms_conv_length);
		mStealthMessage = holder.txtMsg;
		mStealthMessage.setBackgroundColor(Color.argb(100, 200, 200, 200));
		mStealthFooter.setTag(holder);
		
		mList.addFooterView(mStealthFooter);
	}
	
	private void sendSMS()
	{
		
		SmsManager manager = SmsManager.getDefault();
		manager.sendTextMessage(to_address, null, 
						mTxtMessage.getText().toString(), null, null);
		
		SmsDatabase db = new SmsDatabase(this);
		db.open();
		db.addMeMessageToThread(mTxtMessage.getText().toString(), thread_id);
		db.close();
		mTxtMessage.setText("");
		mStealthMessage.setText("");
		getConversation();
		mAdapter.notifyDataSetChanged();
		mList.setSelection(mMessages.size()-1);
	}
	
	private String sendStealthSMS()
	{
		String path = "sms/add";
		Bundle params = new Bundle();		
		
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneId = tm.getLine1Number();
		String tophone = to_address;
		
		params.putString("pubmessage", URLEncoder.encode(mTxtMessage.getText().toString()));
		params.putString("message", URLEncoder.encode(mStealthMessage.getText().toString()));
		params.putString("iToken", Util.mInsider.getAccessToken());
		params.putString("to_phone", tophone);
		params.putString("from_phone", phoneId);
		try {
			
			String response = AHttpUtil.simplePost(Insider.I_BASE_PATH + path, params);
			Util.log(response);
			JSONObject jsonResponse = new JSONObject(response);
			String id = jsonResponse.getString("id");
			String fullMessage = SMS_IDENTIFIER + id + mTxtMessage.getText().toString();
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(to_address, null, 
							fullMessage, null, null);
			
			SmsDatabase db = new SmsDatabase(this);
			db.open();
			db.addMeMessageToThread(fullMessage, thread_id);
			db.close();
			mTxtMessage.setText("");
			mStealthMessage.setText("");
			getConversation();
			mAdapter.notifyDataSetChanged();
			mList.setSelection(mMessages.size());
		}catch(Exception ex){}
		return "";
		
	}
	
	private class ConvListAdapter extends BaseAdapter
	{

		public int getCount() {
			if(mMessages == null) return 0;
			
			return mMessages.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int pos, View convertview, ViewGroup arg2) {
			View view = convertview;
			if(view == null)
			{
				view = mInflater.inflate(R.layout.sms_cell_message, null);
				CellHolder holder = new CellHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.sms_cell_icon);
				holder.txtName = (TextView)view.findViewById(R.id.sms_cell_name);
				//holder.txtPhone = (TextView)view.findViewById(R.id.sms_cell_phone);
				//holder.txtMsg = (TextView)view.findViewById(R.id.sms_cell_text);
				holder.txtTime = (TextView)view.findViewById(R.id.sms_cell_date);
				view.setTag(holder);
			}
			
			CellHolder holder = (CellHolder)view.getTag();
			ISMSMsg msg = mMessages.get(pos);
			String message = msg.message;
			
			String name = to_name;
			String color = "#FFFFFF";
			
			
			if(msg.ident != null)//&& !mPositions.contains(String.valueOf(pos))) //if you want click setting to stay
			{
				String insiderMsg = mInsideHash.get(msg.ident);
				if(insiderMsg != null)
				{
					color = "#FFFF00";
					message = insiderMsg;
				}
				else
					color = "#FFFFFF";
				//holder.txtName.setTextColor(getResources().getColor(R.color.stealth_color));
			}
			else
			{
				//holder.txtName.setTextColor(Color.WHITE);
				color = "#FFFFFF";
			}
			
			if(message == null)
			{
				message = msg.message;
				color = "#FFFFFF";
			}
			holder.imgIcon.setImageResource(R.drawable.icon);
			if(msg.address != null && name != null)
			{
				holder.txtName.setText(Html.fromHtml("<b>" + name + " ( " + msg.address + " ): </b>"
						+ "<font color='" + color + "'>" + message +"</font>"));
				if(to_Bitmap != null);
					holder.imgIcon.setImageBitmap(to_Bitmap);
			}
			else if(name == null && msg.address != null)
			{
				holder.txtName.setText(Html.fromHtml("<b>" + msg.address + ": </b>" 
						+ "<font color='" + color + "'>" + message +"</font>"));
			}
			else
				holder.txtName.setText(Html.fromHtml("<b>Me: </b>" 
						+ "<font color='" + color + "'>" + message +"</font>"));
			
			holder.txtTime.setText(Util.formatSmsTime(msg.time, true));
			mPositions.remove(String.valueOf(pos));
			if(msg.ident != null)
				holder.txtName.setTag(String.valueOf(pos));
			else
				holder.txtName.setTag(null);
			
			holder.txtName.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if(v.getTag() != null)
					{
						int pos = Integer.parseInt((String)v.getTag());
						ISMSMsg msg = mMessages.get(pos);
						
						
						String name = to_name;
						String color = "#FFFFFF";
						String message = msg.message;
						
						if(mPositions.contains((String)v.getTag()))
						{
							color = "#FFFF00";
							message = mInsideHash.get(msg.ident);
							mPositions.remove((String)v.getTag());
						}
						else
							mPositions.add((String)v.getTag());
						
						if(msg.address != null && name != null)
							((TextView)v).setText(Html.fromHtml("<b>" + name + " ( " + msg.address + " ): </b>"
									+ "<font color='" + color + "'>" + message +"</font>"));
						else if(name == null && msg.address != null)
						{
							((TextView)v).setText(Html.fromHtml("<b>" + msg.address + ": </b>" 
									+ "<font color='" + color + "'>" + message +"</font>"));
						}
						else
							((TextView)v).setText(Html.fromHtml("<b>Me: </b>" 
									+ "<font color='" + color + "'>" + message +"</font>"));
					}
				}
			});
			return view;
		}
		
	}
	private class FooterHolder 
	{
		boolean isVisible = false;
		EditText txtMsg, txtSMsg;
		Button btnSend;
		TextView txtLength;
		ImageButton btnSecure,btnNonSecure;
		
	}
	
	private class CellHolder
	{
		boolean isInsider;
		ImageView imgIcon;
		TextView txtName;
		TextView txtPhone;
		TextView txtMsg;
		TextView txtTime;
		
	}
	
	private void deleteMessage(int id, int pos)
	{
		SmsDatabase mDb = new SmsDatabase(this);
		mDb.open();
		mDb.deleteTextMessage(id);
		mMessages.remove(pos);
		mDb.close();
		mAdapter.notifyDataSetChanged();
		int sel = pos-1;
		if(pos>-1 && mMessages.size() > 0)
			mList.setSelection(sel);
		
	}

	public void onItemClick(AdapterView<?> arg0, View view, final int pos, long arg3) {
		
		final ISMSMsg msg = mMessages.get(pos);
		
		new AlertDialog.Builder(this).setTitle("Message options")
		.setItems(new String[] {"Forward", "Delete Message"}, 
				new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0: //Forward
					//TODO: implement
					break;
				case 1:
					deleteMessage(msg.id, pos);
					break;
				default:
					break;
				}
			}
		}).show();
		
	}
	
	private void setupBroadcastReceiver()
	{
		if(mReceiver != null)
			return;
		mReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				
				mHandler.post(new Runnable() {
					
					public void run() {
						getConversation();
						mAdapter.notifyDataSetChanged();
						mList.setSelection(mMessages.size());
						NotificationManager nMn = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
						nMn.cancel("sms", thread_id);
					}
				});
				
			}
		};
			
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.sms_menu_conversation, menu);
				
		return true;//super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.sms_mnu_allmsg:
			//Intent intent = new Intent(SmsConversation.this, SmsInbox.class);
			//startActivity(intent);
			finish();
			break;
		case R.id.sms_mnu_call:
			Intent callIntent = new Intent(Intent.ACTION_DIAL);
	        callIntent.setData(Uri.parse("tel:" + to_address));
	        startActivity(callIntent);
			break;
		case R.id.sms_mnu_schedule:
			scheduleMessage();
			break;
		case R.id.sms_mnu_compose:
			Intent compose = new Intent(SmsConversation.this, SmsCompose.class);
			startActivity(compose);
			finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mOffset = firstVisibleItem;
		
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}
	
	public class MyGestureDetector extends SimpleOnGestureListener {
		
	    @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	        return false;
	    }
	    @Override
	    public boolean onDown(MotionEvent e) {
	    	return false;
	    }
	    @Override
	    public boolean onSingleTapConfirmed(MotionEvent e) {
	    	Util.log("singleTap");
	    	int pos = mList.pointToPosition((int)e.getX(), (int)e.getY());
	    	Util.log(pos - mOffset);
	    	/*if(mLastHolder != null)
	    		mLastHolder.btnRemove.setVisibility(View.INVISIBLE);*/
	    	return false;
	    }
	}
	
	private void filterIds()
	{
		ArrayList<String> ids = new ArrayList<String>();
		for(ISMSMsg msg : mMessages)
		{
			if(msg.message.startsWith(SMS_IDENTIFIER))
			{
				//filter out message so it doesn't look like an insid3r message
				msg.ident = msg.message.substring(2, 12);
				msg.message = msg.message.substring(12);
				if(!mIdentList.contains(msg.ident) && !ids.contains(msg.ident)
						&& !mInsideHash.containsKey(msg.ident) && !mCheckedIds.contains(msg.ident))
					ids.add(msg.ident);
			}
		}
		
		mIdentList.addAll(ids);
	}
	
	private void translateMessages()
	{
		if(!Util.mInsider.isLoggedIn())
			return;
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Loading...");
		dialog.show();
		
		new Thread(new Runnable() {
			
			public void run() {
				JSONArray jsonIds = new JSONArray();
				for(String string : mIdentList)
				{
					jsonIds.put(string);
				}
				mCheckedIds.addAll(mIdentList);
				mIdentList.clear();
				Util.log("Translate Count: " + String.valueOf(jsonIds.length()));
				if(jsonIds.length() == 0)
				{
					dialog.dismiss();
					return;
				}
				String url = Insider.getUrlPath("sms/batch");
				Bundle params = new Bundle();
				params.putString("iToken", Util.mInsider.getAccessToken());
				params.putString("keys", jsonIds.toString());
				try {
					final String results = AHttpUtil.simplePost(url, params);
					Util.log(results);
					mHandler.post( new Runnable() {
						public void run() {
							try {
								JSONObject jsonResults = new JSONObject(results);
								for (Iterator iterator = jsonResults.keys(); iterator.hasNext();) {
									String key = (String) iterator.next();
									try {
										mInsideHash.put(key, jsonResults.getJSONObject(key).getString("message"));
									}catch(Exception ex){Util.log("err3:" + ex.getMessage());}
								}
								mAdapter.notifyDataSetChanged();
							}
							catch(Exception ex) {Util.log("err4");}
						}
					});
				}catch(Exception ex) {Util.log("err5: " + ex.getLocalizedMessage() );}
				dialog.dismiss();
			}
		}).start();
		
		
	}

	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos,
			long arg3) {
		
		final ISMSMsg msg = mMessages.get(pos);
		Util.log("long click l");
		String[] items = {"Delete"};
		new AlertDialog.Builder(mContext).setItems(items, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0)
				{
					new AlertDialog.Builder(mContext).setTitle("Delete Message").setMessage("Delete this Message?")
						.setNegativeButton("No", null).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								SmsDatabase db = new SmsDatabase(mContext);
						        db.open();
						        db.deleteTextMessage(msg.id);
						        db.close();
						        mHandler.post(new Runnable() {
									
									public void run() {
										getConversation();
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

	private void scheduleMessage()
	{
		if(mTxtMessage.getText().toString().length() == 0)
		{
			new AlertDialog.Builder(mContext).setTitle("Can't Send Nothing")
				.setMessage("Your text message is empty.").setPositiveButton("Ok", null).create().show();
			return;
		}
		View view = mInflater.inflate(R.layout.schedule_dialog, null);
		final TimePicker timePicker = (TimePicker)view.findViewById(R.id.timePicker1);
		final DatePicker datePicker = (DatePicker)view.findViewById(R.id.datePicker1);
		
		new AlertDialog.Builder(mContext).setTitle("Set Date and Time").setView(view).setNegativeButton("Cancel", null)
			.setPositiveButton("Schedule", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					//Schedule Message
					ScheduleDb db = new ScheduleDb(mContext);
					db.open();
					int hour = timePicker.getCurrentHour();
					int min = timePicker.getCurrentMinute();
					int month = datePicker.getMonth();
					int day = datePicker.getDayOfMonth();
					int year = datePicker.getYear();
					Util.log(hour);
					Util.log(min);
					Util.log(month);
					Util.log(day);
					Util.log(year);

					SimpleDateFormat format = new SimpleDateFormat("H:m M/d/yyyy");
					try {
						String date = String.format("%d:%d %d/%d/%d", hour,min,(month+1),day,year);
						Date ndate = format.parse(date);
						long time = ndate.getTime();
						db.scheduleMessage(mTxtMessage.getText().toString(), time, to_address);
						new AlertDialog.Builder(mContext).setTitle("Scheduled")
							.setMessage("Your SMS Message is scheduled for:\n" + ndate.toLocaleString())
								.setPositiveButton("Ok", null).create().show();
						startService(new Intent(SmsConversation.this, ScheduleMsgService.class));
					}catch(Exception ex){}
					db.close();
					mTxtMessage.setText("");
					mStealthMessage.setText("");
				}
			}).create().show();
	}
	
	public Bitmap getContactImage(String contact_id)
	{
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contact_id));
	    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(), uri);
	    Bitmap bitmap = BitmapFactory.decodeStream(input);
	    return bitmap;
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
}
