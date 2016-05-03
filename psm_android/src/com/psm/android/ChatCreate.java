package com.psm.android;

import java.util.ArrayList;

import com.psm.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ChatCreate extends Activity {

	private Context mContext;
	private LayoutInflater mInflater;
	private Handler mHandler;
	private static final int REQUEST_GROUP = 4;
	private Button mBtnAddInsider, mBtnAddGroup;
	
	private ListView mList;
	private chatcreateAdapter mAdapter;
	
	private ArrayList<CreateChatItem> mItems = new ArrayList<CreateChatItem>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chat_create);
		mContext = this;
		mInflater = getLayoutInflater();
		mHandler = new Handler();
		mBtnAddInsider = (Button)findViewById(R.id.chat_create_btnadd);
		mBtnAddGroup = (Button)findViewById(R.id.chat_create_group);
		
		mAdapter = new chatcreateAdapter();
		mList = (ListView)findViewById(R.id.chat_create_list);
		mList.setAdapter(mAdapter);
		
		mBtnAddInsider.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				askForInsider();
			}
		});
		
		mBtnAddGroup.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				askForGroup();
			}
		});
		
	}
	
	private void askForInsider()
	{
		final EditText txtView = new EditText(mContext);
		txtView.setHint("insid3r User/Id");
		new AlertDialog.Builder(mContext).setTitle("Add insider")
				.setView(txtView).setNegativeButton("Cancel", null)
					.setPositiveButton("Add", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String groupName = txtView.getText().toString();
							String groupId = txtView.getText().toString();
							CreateChatItem item = new CreateChatItem(CreateChatItem.TYPE_USER, groupName, groupId);
							mItems.add(item);
							mAdapter.notifyDataSetChanged();
						}
					}).create().show();
	}
	
	private void askForGroup()
	{
		Intent intent = new Intent(ChatCreate.this, GroupChooser.class);
		intent.putExtra("com.psm.android.hideeveryone", true);
		startActivityForResult(intent, REQUEST_GROUP);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_GROUP && resultCode == RESULT_OK)
		{
			String groupName = data.getExtras().getString("com.psm.android.groupname");
			String groupId = data.getExtras().getString("com.psm.android.groupid");
			CreateChatItem item = new CreateChatItem(CreateChatItem.TYPE_GROUP, groupName, groupId);
			
			mItems.add(item);
			mAdapter.notifyDataSetChanged();
		}
		
	}
	
	private class CreateChatItem
	{
		public final static int TYPE_GROUP = 0;
		public final static int TYPE_USER = 1;
		
		private int mType = 0;
		private String mName = "";
		private String mId = "";
		
		public int getType() { return mType; }
		public void setType(int type) {
			this.mType = type;
		}
		
		public String getName() { return mName; }
		public void setName(String mName) {
			this.mName = mName;
		}
		
		public String getId() {	return mId;	}
		public void setId(String id) {
			this.mId = id;
		}
		
		public CreateChatItem(int Type, String name, String id) {
			setType(Type);
			setName(name);
			setId(id);
		}
		
	}
	
	private class chatcreateAdapter extends BaseAdapter
	{

		public int getCount() {
			return mItems.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			if(view == null)
			{
				view = mInflater.inflate(R.layout.group_cell_item, null);
				cellHolder holder = new cellHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.group_cell_image);
				holder.txtName = (TextView)view.findViewById(R.id.group_cell_name);
				holder.btnRemove = (Button)view.findViewById(R.id.group_cell_btnremove);
				view.setTag(holder);
			}
			cellHolder holder = (cellHolder)view.getTag();
			final int pos = position;
			holder.btnRemove.setOnClickListener(new View.OnClickListener() {
				public void onClick(View arg0) {
					mItems.remove(pos);
					mAdapter.notifyDataSetChanged();
				}
			});
			CreateChatItem item = mItems.get(position);
			if(item.getType() == CreateChatItem.TYPE_GROUP)
				holder.imgIcon.setImageResource(R.drawable.group_t);
			else
				holder.imgIcon.setImageResource(R.drawable.icon_t);
			holder.txtName.setText(item.getName());
			return view;
		}
		
	}
	private class cellHolder
	{
		ImageView imgIcon;
		TextView txtName;
		Button btnRemove;
	}
}
