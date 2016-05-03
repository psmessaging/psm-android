package com.psm.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.R;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class GroupListActivity extends Activity {

	private ListView mList;
	private groupAdapter mAdapter;
	private LayoutInflater mInflater;
	
	private Button mbtnAdd;
	
	private JSONArray mJsonGroups = new JSONArray();
	private ArrayList<JSONObject> mSortedGroups = new ArrayList<JSONObject>();
	private Context mContext;
	private Handler mHandle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.group_activity);
		mContext = this;
		mHandle = new Handler();
		mInflater = getLayoutInflater();
		
		mAdapter = new groupAdapter();
		mList = (ListView)findViewById(R.id.group_activity_list);
		mList.setAdapter(mAdapter);
		getGroups();
		
		mbtnAdd = (Button)findViewById(R.id.group_activity_add);
		mbtnAdd.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				Intent intent = new Intent(GroupListActivity.this, GroupCreate.class);
				startActivityForResult(intent, 4);
			}
		});
	}
	
	
	private class groupAdapter extends BaseAdapter
	{

		public int getCount() {
			return mSortedGroups.size();
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
				view = mInflater.inflate(R.layout.group_list_cell_item, null);
				groupHolder holder = new groupHolder();
				holder.name = (TextView)view.findViewById(R.id.glc_name);
				holder.icon = (ImageView)view.findViewById(R.id.glc_lock);
				view.setTag(holder);
				
			}
			groupHolder holder = (groupHolder)view.getTag();
			
			try {
				JSONObject obj = mSortedGroups.get(position);
				holder.name.setText(obj.getString("name") + " ( " + obj.getString("count") + " )");
				if(obj.has("private"))
				{
					if(obj.getBoolean("private"))
						holder.icon.setVisibility(View.VISIBLE);
					else
						holder.icon.setVisibility(View.GONE);
				}
				else
					holder.icon.setVisibility(View.GONE);
				
			}catch(Exception ex) {}
			return view;
		}
		
	}
	
	private class groupHolder
	{
		TextView name;
		ImageView icon;
	}
	
	private void getGroups()
	{
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Loading...");
		dialog.show();
		new Thread(new Runnable() {
			
			public void run() {
				
				Bundle bundle = new Bundle();
				bundle.putString("iToken", Util.mInsider.getAccessToken());
				bundle.putString("limit", "25");
				String path = Insider.getUrlPath("group/self");
				try {
					final String results = AHttpUtil.openUrl(path, bundle);
					Util.log(results);
					mHandle.post(new Runnable() {
						public void run() {
							try {
								mJsonGroups = new JSONArray(results);
								mSortedGroups.clear();
								for(int i = 0;i<mJsonGroups.length();i++)
								{
									mSortedGroups.add(mJsonGroups.getJSONObject(i));
								}
								Collections.sort(mSortedGroups, new NameComparator());
								mAdapter.notifyDataSetChanged();
							}catch(Exception ex){}
						}
					});
					
				}catch(Exception ex) {}
				dialog.dismiss();
			}
		}).start();
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == 4 && resultCode == RESULT_OK)
		{
			//TODO: change this to refresh the list after creating a group
			getGroups();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private static class NameComparator implements Comparator<JSONObject>{

		public int compare(JSONObject lhs, JSONObject rhs) {
			try {
				String name1 = lhs.getString("name").toLowerCase();
				String name2 = rhs.getString("name").toLowerCase();
				return ((name1 == name2) ? 0 : ((name1.compareTo(name2) > 0) ? 1 : -1 ));
			}catch(Exception ex) {}
			return 0;
		}
		
	}
}
