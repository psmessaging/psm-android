package com.psm.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.psm.android.R;
import com.psm.android.sms.SmsCompose;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;


public class ScheduleList extends Activity implements OnItemLongClickListener{

	private ListView mList;
	private ScheduleAdapter mAdapter;
	private LayoutInflater mInflater;
	private Context mContext;
	private Button mCreateButton;
	
	private ArrayList<Bundle> mArray;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.schedule_layout);
		mInflater = getLayoutInflater();
		mContext = this;
		mCreateButton = (Button)findViewById(R.id.sl_layout_create);
		mCreateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent(ScheduleList.this, SmsCompose.class);
				intent.putExtra("com.psm.android.schedule", true);
				startActivityForResult(intent, 4);
			}
		});
		mAdapter = new ScheduleAdapter();
		mList = (ListView)findViewById(R.id.sl_list);
		mList.setAdapter(mAdapter);
		mList.setOnItemLongClickListener(this);
	}
	
	@Override
	protected void onResume() {
		loadMessages();
		super.onResume();
	}
	private void loadMessages()
	{
		ScheduleDb mDb = new ScheduleDb(mContext);
		mDb.open();
		mArray = mDb.waitingToSendSms();
		mDb.close();
		mAdapter.notifyDataSetChanged();
		
	}
	
	private class ScheduleAdapter extends BaseAdapter
	{

		public int getCount() {
			if(mArray == null) return 0;
			return mArray.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup arg2) {
			View view = convertView;
			if(view == null)
			{
				view = mInflater.inflate(R.layout.sl_cell, null);
				holderClass holder = new holderClass();
				holder.txtMessage = (TextView)view.findViewById(R.id.sl_cell_message);
				holder.txtTime = (TextView)view.findViewById(R.id.sl_cell_time);
				holder.txtTo = (TextView)view.findViewById(R.id.sl_cell_to);
				view.setTag(holder);
			}
			holderClass holder = (holderClass)view.getTag();
			Bundle bundle = mArray.get(position);	
			holder.id = bundle.getString("id");
			holder.txtMessage.setText(bundle.getString("message"));
			holder.txtTo.setText(PhoneNumberUtils.formatNumber(bundle.getString("address")));
			long time = bundle.getLong("time");
			String timeStr = new Date(time).toLocaleString();
			holder.txtTime.setText("Scheduled For: " + timeStr);
			return view;
		}
		
	}
	
	private class holderClass
	{
		String id;
		TextView txtMessage;
		TextView txtTime;
		TextView txtTo;
	}

	public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position,
			long arg3) {
		
		new AlertDialog.Builder(mContext).setItems(new String[] {"Delete"}, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface arg0, int arg1) {
				Bundle bundle = mArray.get(position);	
				String id = bundle.getString("id");
				ScheduleDb mDb = new ScheduleDb(mContext);
				mDb.open();
				mDb.deleteMessage(id);
				mDb.close();
				loadMessages();
				Intent service = new Intent(ScheduleList.this, ScheduleMsgService.class);
				startService(service);
			}
		}).create().show();
		
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == 4 && resultCode == RESULT_OK)
		{
			loadMessages();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
