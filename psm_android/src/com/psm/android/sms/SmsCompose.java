package com.psm.android.sms;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.json.JSONObject;

import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.ScheduleDb;
import com.psm.android.ScheduleMsgService;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

public class SmsCompose extends Activity {

	private final static int PICK_CONTACT = 4;
	private EditText  mTextMessage, mStealthMessage;
	private AutoCompleteTextView mPhoneTo;
	
	private String mDisplayName, mPhoneNumber;
	private TextView mTxtLength, mTxtStealthLength;
	private Button mSendButton;
	
	private LayoutInflater mInflater;
	private Context mContext;
	
	private static final int COLUMN_DISPLAY_NAME 	= 1;
	private static final int COLUMN_ID				= 8;
	private RelativeLayout mStealthLayout;
	private boolean mScheduledMessage = false;
	
    public static final String[] CONTACT_PROJECTION = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sms_compose);
		
		mInflater = getLayoutInflater();
		mContext = this;
		
		ContentResolver content = getContentResolver();
        Cursor cursor = content.query(Contacts.CONTENT_URI,
                CONTACT_PROJECTION, null, null, null);
        ContactListAdapter adapter = new ContactListAdapter(this, cursor);
		mPhoneTo = (AutoCompleteTextView)findViewById(R.id.sms_comp_to);
		//mPhoneTo.setAdapter(adapter);
		
		mTextMessage = (EditText)findViewById(R.id.sms_comp_text);
		mStealthMessage = (EditText)findViewById(R.id.sms_comp_txtstealth);
		
		mTxtLength = (TextView)findViewById(R.id.sms_comp_txtlength);
		mTxtStealthLength = (TextView)findViewById(R.id.sms_comp_txtslength);
		mSendButton = (Button)findViewById(R.id.sms_comp_send);
		mStealthLayout = (RelativeLayout)findViewById(R.id.sms_comp_stealthlayout);
		
		if(Util.mInsider.isLoggedIn())
			mStealthLayout.setVisibility(View.VISIBLE);
		else
			mStealthLayout.setVisibility(View.GONE);
		
		mStealthMessage.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			public void afterTextChanged(Editable s) {
				int length = mStealthMessage.getText().toString().length();
				mTxtStealthLength.setText( String.valueOf(length) + " / 200");
			}
		});
		
		mTextMessage.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
			public void afterTextChanged(Editable arg0) {
				int length = mTextMessage.getText().toString().length();
				mTxtLength.setText( String.valueOf(length) + " / 160");
				if(length > 0)
					mSendButton.setEnabled(true);
				else
					mSendButton.setEnabled(false);
			}
		});
		/*mTextMessage.setOnKeyListener(new View.OnKeyListener() {
			
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				int length = mTextMessage.getText().toString().length();
				mTxtLength.setText( String.valueOf(length) + " / 160");
				
				if(length > 0)
					mSendButton.setEnabled(true);
				else
					mSendButton.setEnabled(false);
				
				return false;
			}
		});*/
		
		Button btnContact = (Button)findViewById(R.id.sms_comp_contact);
		btnContact.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				displayContacts();
			}
		});
		
		
		mSendButton.setOnClickListener( new View.OnClickListener() {
			
			public void onClick(View v) {
				//Check to see if there is insider text to send to prepend id
				if(mStealthMessage.getText().length() > 0)
				{
					sendStealthText();
				}
				else
				{
					if(mScheduledMessage == true)
						scheduleMessage();
					else
						sendText();
				}
			}
		});
		if(getIntent().getExtras() != null)
		{
			Bundle extras = getIntent().getExtras();
			if(extras.containsKey("com.psm.android.schedule"))
			{
				mScheduledMessage = true;
				mStealthLayout.setVisibility(View.GONE);
				mSendButton.setText("Schedule");
			}
			
			
		}
		
		
	}
	
	
	
	private void displayContacts()
	{
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		startActivityForResult(intent, PICK_CONTACT);

	}
	
	private void sendText()
	{
		if(mPhoneTo.getText().toString().length() > 3)
		{
			
			Util.log("Sending Text message");
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(mPhoneTo.getText().toString(), null, 
							mTextMessage.getText().toString(), null, null);
			
			String tophone = mPhoneTo.getText().toString();
			String fullMessage = mTextMessage.getText().toString();
			tophone = tophone.replaceAll("[-()]", "");
			SmsDatabase db = new SmsDatabase(this);
			db.open();
			int thread_id = db.getThread_id(tophone);
			System.out.println("thread_id: " + String.valueOf(thread_id));
			
			if(thread_id == 0)
				db.addTextMessage(fullMessage, tophone, true);
			else
				db.addMeMessageToThread(fullMessage, thread_id);
			
			thread_id = db.getThread_id(tophone);
			db.close();
			
			mPhoneTo.setText("");
			mTextMessage.setText("");
			mStealthMessage.setText("");
			Intent notificationIntent = new Intent(SmsCompose.this, SmsInbox.class);
            notificationIntent.putExtra("com.psm.android.thread_id", thread_id);
            startActivity(notificationIntent);
			finish();
		}
	}
	
	private void sendStealthText()
	{
		String path = "sms/add";
		Bundle params = new Bundle();		
		
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneId = tm.getLine1Number();
		String tophone = mPhoneTo.getText().toString();
		tophone = tophone.replaceAll("[-()]", "");
		
		params.putString("pubmessage", URLEncoder.encode(mTextMessage.getText().toString()));
		params.putString("message", URLEncoder.encode(mStealthMessage.getText().toString()));
		params.putString("iToken", Util.mInsider.getAccessToken());
		params.putString("to_phone", tophone);
		params.putString("from_phone", phoneId);
		try {
			
			String response = AHttpUtil.simplePost(Insider.I_BASE_PATH + path, params);
			Util.log(response);
			JSONObject jsonResponse = new JSONObject(response);
			String id = jsonResponse.getString("id");
			String fullMessage = Insider.SMS_IDENTIFIER + id + mTextMessage.getText().toString();
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(tophone, null, 
							fullMessage, null, null);
			SmsDatabase db = new SmsDatabase(this);
			db.open();
			int thread_id = db.getThread_id(tophone);
			System.out.println("thread_id: " + String.valueOf(thread_id));
			if(thread_id == 0)
				db.addTextMessage(fullMessage, tophone, true);
			else
				db.addMeMessageToThread(fullMessage, thread_id);
			thread_id = db.getThread_id(tophone);
			db.close();

			mPhoneTo.setText("");
			mTextMessage.setText("");
			mStealthMessage.setText("");
			Intent notificationIntent = new Intent(SmsCompose.this, SmsInbox.class);
            notificationIntent.putExtra("com.psm.android.thread_id", thread_id);
            startActivity(notificationIntent);
			finish();
		}catch(Exception ex){}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == PICK_CONTACT && resultCode == RESULT_OK)
		{
			Uri contactData = data.getData();
	        Cursor c =  managedQuery(contactData, null, null, null, null);
	        if (c.moveToFirst()) {
	        	String contactId = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
	        	String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	        	
	        	
	        	Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,null, null);
	        	String phoneNumber = "";
	            if(phones.moveToFirst())
	            	phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
	            
	            phones.close();
	        	
	            for(int x =0; x < c.getColumnCount();x++)
	        	{
	        		Util.log(String.valueOf(x) + ": " + c.getColumnName(x));
	        	}
	        	//String phonenumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
	        	mDisplayName = name;
	          	mPhoneTo.setText(phoneNumber);
	        }
	        c.close();
			//getNamePhone(data);
			//mPhoneTo.setText(mPhoneNumber);
		}
		
	}
	
	private void getNamePhone(Intent data)
	{
		Uri uri = data.getData();
		try {
			
			Cursor c = getContentResolver().query(uri, new String[]{ 
	                ContactsContract.CommonDataKinds.Phone.NUMBER,  
	                ContactsContract.CommonDataKinds.Phone.TYPE, 
	                ContactsContract.CommonDataKinds.Phone._ID},
	                
	            null, null, null);
			Util.log(c.getColumnCount());
			if(c != null)
			{
				if(c.moveToFirst())
				{
					Util.log("Column Count: " + c.getColumnCount());
					mPhoneNumber = c.getString(0);
					mDisplayName = "name";
					//mDisplayName = c.getString(1);
				}
			}
			
		}catch(Exception ex) {Util.log(ex.getMessage());}
	}
	
	public static class ContactListAdapter extends CursorAdapter implements Filterable {
		ContentResolver mContent;
		
        public ContactListAdapter(Context context, Cursor c) {
            super(context, c);
            mContent = context.getContentResolver();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            final TextView view = (TextView) inflater.inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
            view.setText(cursor.getString(COLUMN_DISPLAY_NAME));
            
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view).setText(cursor.getString(COLUMN_DISPLAY_NAME));
        }

        @Override
        public String convertToString(Cursor cursor) {
            return cursor.getString(COLUMN_DISPLAY_NAME);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            FilterQueryProvider filter = getFilterQueryProvider();
            if (filter != null) {
                return filter.runQuery(constraint);
            }
            
            Uri uri = Uri.withAppendedPath(
                    Contacts.CONTENT_FILTER_URI,
                    Uri.encode(constraint.toString()));
            return mContent.query(uri, CONTACT_PROJECTION, null, null, null);
        }

        
    }

    
	private void scheduleMessage()
	{
		if(mTextMessage.getText().toString().length() == 0)
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
						String tophone = mPhoneTo.getText().toString();
						tophone = tophone.replaceAll("[-()]", "");
						db.scheduleMessage(mTextMessage.getText().toString(), time, tophone);
						new AlertDialog.Builder(mContext).setTitle("Scheduled")
							.setMessage("Your SMS Message is scheduled for:\n" + ndate.toLocaleString())
								.setPositiveButton("Ok", null).create().show();
						startService(new Intent(SmsCompose.this, ScheduleMsgService.class));
						
					}catch(Exception ex){}
					db.close();
					mTextMessage.setText("");
					mStealthMessage.setText("");
					setResult(RESULT_OK);
					finish();
				}
			}).create().show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Schedule Message");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		scheduleMessage();
		return super.onOptionsItemSelected(item);
	}
}
