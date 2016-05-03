package com.psm.android;

import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.R;
import com.psm.util.Insider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class GroupChooser extends Activity implements OnItemClickListener {

	private Handler mHandler;
	private LayoutInflater mInflater;
	
	private JSONArray jsonGroupArray;
	private ArrayList<JSONObject> sortedGroups = new ArrayList<JSONObject>();
	private boolean groupCache = true;
	
	
	private GroupListAdapter mAdapter;
	private ListView	mList;
	
	private LinearLayout mProgress;
	
	private boolean hideEveryone = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.group_chooser);
		
		mHandler = new Handler();
		mInflater = getLayoutInflater();
		mList = (ListView)findViewById(R.id.group_chooser_list);
		mProgress = (LinearLayout)findViewById(R.id.group_chooser_progress);
		mAdapter = new GroupListAdapter();
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
		
		if(getIntent().getExtras() != null)
		{
			if(getIntent().getExtras().containsKey("com.psm.android.hideeveryone"))
				hideEveryone = getIntent().getExtras().getBoolean("com.psm.android.hideeveryone");
		}
		getGroups();
	}
	
	private void getGroups()
	{
		
		mProgress.setVisibility(View.VISIBLE);
		new Thread(new Runnable() {
			
			public void run() {
				String path = Insider.getUrlPath("group/self");
				// + "?iToken=" + Util.mInsider.getAccessToken();
				Bundle params = new Bundle();
				params.putString("iToken", Util.mInsider.getAccessToken());
				final String results = ACacheUtil.getUrl(path, params, groupCache);
				mHandler.post(new Runnable() {
					public void run() {
						try {
							//mGroupProgress.setVisibility(View.GONE);
							jsonGroupArray = new JSONArray(results);
							groupCache = false;
							sortedGroups.clear();
							for(int i=0;i<jsonGroupArray.length();i++)
							{
								sortedGroups.add(jsonGroupArray.getJSONObject(i));
							}
							Collections.sort(sortedGroups, new Util.JsonNameComparator());
							mAdapter.notifyDataSetChanged();
						}catch(Exception ex) {}
						mProgress.setVisibility(View.INVISIBLE);
					}
				});
			}
		}).start();
		
	}
	
	private class GroupListAdapter extends BaseAdapter
	{

		public int getCount() {
			if(jsonGroupArray == null) return 0;
			if(!hideEveryone)
				return jsonGroupArray.length()+1;
			else
				return jsonGroupArray.length();
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
				view = mInflater.inflate(R.layout.group_cell_group, null);
				groupHolder holder = new groupHolder();
				holder.txtName = (TextView)view.findViewById(R.id.group_cell_group_text);
				holder.txtCount = (TextView)view.findViewById(R.id.group_cell_group_count);
				holder.imgCheck = (ImageView)view.findViewById(R.id.group_cell_group_check);
				holder.imgCheck.setVisibility(View.GONE);
				holder.lockIcon = (ImageView)view.findViewById(R.id.group_cell_group_lock);

				view.setTag(holder);
			}
			groupHolder holder = (groupHolder)view.getTag();
			if(position == 0 && !hideEveryone)
			{
				holder.groupId = null;
				holder.groupName = "Everyone";
				holder.txtName.setText("Everyone");
				holder.txtCount.setVisibility(View.INVISIBLE);
				holder.lockIcon.setVisibility(View.GONE);
			}
			else
			{
				try {
					
					JSONObject obj = sortedGroups.get(hideEveryone ? position : position-1);
					holder.groupId = obj.getString("id");
					holder.groupName = obj.getString("name");
					holder.groupCount = obj.getInt("count");
					holder.txtName.setText(obj.getString("name"));
					holder.txtCount.setVisibility(View.VISIBLE);
					holder.txtCount.setText(obj.getString("count"));
					if(obj.has("private"))
					{
						if(obj.getBoolean("private"))
							holder.lockIcon.setVisibility(View.VISIBLE);
						else
							holder.lockIcon.setVisibility(View.GONE);
					}
					else
						holder.lockIcon.setVisibility(View.GONE);
				}catch(Exception ex) {}
			}
			
			return view;
		}
		
	}

	private static class groupHolder
	{
		String groupId;
		String groupName;
		int groupCount;
		
		TextView txtName;
		TextView txtCount;
		ImageView imgCheck;
		ImageView lockIcon;
		
		
	}
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
		
		//Returns groupid, groupname, groupcount
		groupHolder holder = (groupHolder)view.getTag();
		Intent data = new Intent();
		data.putExtra("com.psm.android.groupid", holder.groupId);
		data.putExtra("com.psm.android.groupname", holder.groupName);
		data.putExtra("com.psm.android.groupcount", holder.groupCount);
		setResult(RESULT_OK, data);
		finish();
	}
	
	
	
}
