package com.psm.android.fs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AHttpUtil;
import com.psm.android.PullToRefreshListView;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.PullToRefreshListView.OnRefreshListener;
import com.psm.android.R.color;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class FoursquareFriends extends Activity implements OnItemClickListener {

	private JSONObject mFriendList = null;
	private LayoutInflater mInflater;
	
	private Context mContext;
	private Handler mHandler;
	
	private PullToRefreshListView mList;
	private FriendListAdapter mAdapter;
	private ProgressBar mProgress;
	
	private JSONArray mJsonRecent;
	private ArrayList<String> mVIdList;
	
	private HashMap<String, JSONObject> mInsiderList = new HashMap<String, JSONObject>(); 
	private String mAfterTimestamp = "0";
	
	private HashMap<Integer, Boolean> mShowingWhat = new HashMap<Integer, Boolean>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_friends);
		
		mInflater = getLayoutInflater();
		mContext = this;
		mHandler = new Handler();
		
		mProgress = (ProgressBar)findViewById(R.id.fsf_progress);
		mList = (PullToRefreshListView)findViewById(R.id.fsf_list_friend);
		mAdapter = new FriendListAdapter();
		mList.setAdapter(mAdapter);
		getFriends();
		
		mList.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
			public void onRefresh() {
				getFriends(true);
				
			}
		});
		mList.setOnItemClickListener(this);
	}
	
	private void getFriends()
	{
		getFriends(false);
	}
	
	private void getFriends(final boolean forceReload)
	{
		mProgress.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			public void run() {
				//Retrieve friends
				//"users/friends"
				String path = "checkins/recent";
				Bundle params = new Bundle();
				params.putString("limit", "25");
				params.putString("afterTimestamp", mAfterTimestamp);
				String results = ACacheUtil.getUrl(Foursquare.getFullUrl(path, params), forceReload);
				if(results != null) Util.log(results);
				Util.log(Foursquare.getFullUrl(path, params));
				try {
					mJsonRecent = new JSONObject(results).getJSONObject("response").getJSONArray("recent");
					mVIdList = collectIds();
					doTranslate();
				} catch(Exception ex) {}
				mHandler.post(new Runnable() {
					public void run() {
						populate();
						mList.onRefreshComplete(new Date().toLocaleString());
						mProgress.setVisibility(View.INVISIBLE);
					}
				});
			}
		}).start();
	}
	
	private void populate()
	{
		mAdapter.notifyDataSetChanged();
		
	}
	
	private class FriendListAdapter extends BaseAdapter
	{
		
		public int getCount() {
			if(mJsonRecent == null) return 0;
			return mJsonRecent.length();
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
				
				view = mInflater.inflate(R.layout.foursquare_cell_friend, null);
				FriendViewHolder holder = new FriendViewHolder();
				holder.userIcon = (ImageView)view.findViewById(R.id.foursquare_cf_icon);
				holder.venueText = (TextView)view.findViewById(R.id.foursquare_cf_text);
				holder.venueAddress = (TextView)view.findViewById(R.id.foursquare_cf_subtext);
				holder.checkinDate = (TextView)view.findViewById(R.id.foursquare_cf_when);
				view.setTag(holder);
				
			}
			
			FriendViewHolder holder = (FriendViewHolder)view.getTag();
			try {
				
				
				JSONObject item = mJsonRecent.getJSONObject(position);
				JSONObject pItem = null;
				boolean contains = mShowingWhat.containsKey(position);
				//Util.log("Position: " + String.valueOf(position) + " value: " + String.valueOf(contains));
				if(mInsiderList.containsKey(item.getString("id")) && contains == false)
				{
					
					JSONObject priv = mInsiderList.get(item.getString("id"));
					String privateId = priv.getString("privateVenueId");
					holder.venueText.setTextColor(getResources().getColor(R.color.stealth_color));
					holder.venueAddress.setTextColor(getResources().getColor(R.color.stealth_color));
					String path = "venues/" + privateId;
					
					String response = ACacheUtil.getUrl(Foursquare.getFullUrl(path));
					pItem = new JSONObject(response).getJSONObject("response");
					
					Util.log("stealth: " + response);
					
				}
				else
				{
					holder.venueText.setTextColor(Color.WHITE);
					holder.venueAddress.setTextColor(Color.WHITE);
				}
				
				
				Util.log(item);
				JSONObject venue = pItem == null ? item.getJSONObject("venue") : pItem.getJSONObject("venue");
				JSONObject user = item.getJSONObject("user");
				
				getBitmap(user.getString("photo"), holder.userIcon);
				
				holder.venueText.setText(user.getString("firstName") + " " + user.getString("lastName").charAt(0) +
						". @ " + venue.getString("name"));
				JSONObject location = venue.getJSONObject("location");
				holder.venueAddress.setText("");
				holder.checkinDate.setText("");
				String address = "";
				if(!location.has("crossStreet"))
				{
					if(location.has("address"))
						address = location.getString("address") + "\n";
				}
				else
					address = location.getString("address") + " (" + location.getString("crossStreet") + ")" + "\n";
				
				if(location.has("city") && location.has("state"))
					address += location.getString("city") + ", " + location.getString("state");
				
				holder.venueAddress.setText(address);
				Date date = new Date(item.getLong("createdAt")*1000);
				holder.checkinDate.setText(date.toLocaleString());
				holder.checkinId = item.getString("id");
				holder.userId = user.getString("id");
			} catch(Exception ex) { Util.log(ex.getMessage()); }
			
			return view;
			
		}
		
	}
	
	private void getBitmap(final String url, final ImageView imageview)
	{
		new Thread(new Runnable() {
			
			public void run() {
				
				final Bitmap map = ACacheUtil.getBitmap(url);
				mHandler.post(new Runnable() {
					public void run() {
						imageview.setImageBitmap(map);
					}
				});
			}
		}).start();
	}
	
	private class FriendViewHolder
	{
		String checkinId;
		String userId;
		ImageView userIcon;
		TextView venueText;
		TextView venueAddress;
		TextView checkinDate;
		
	}
	
	/*
	 * Translate Messages
	 * 
	 */
	
	private ArrayList<String> collectIds()
	{
		ArrayList<String> array = new ArrayList<String>();
		for(int x = 0;x < mJsonRecent.length();x++)
		{
			try {
			JSONObject obj = mJsonRecent.getJSONObject(x);
			array.add(obj.getString("id"));
			}catch(Exception ex) {}
		}
		return array;
	}
	
	private void doTranslate()
	{
		if(!Util.mInsider.isLoggedIn())
		{
			mInsiderList.clear();
			return;
		}
		
		JSONArray jsonIds = new JSONArray();
		for(String string : mVIdList)
		{
			jsonIds.put(string);
		}
		Bundle params = new Bundle();
		params.putString("iToken", Util.mInsider.getAccessToken());
		params.putString("ids", jsonIds.toString());
		try {
			String response = AHttpUtil.simplePost(Insider.I_BASE_PATH + "msg/foursquare/batch", params);
			JSONObject obj = new JSONObject(response).getJSONObject("data");
			for (Iterator iterator = obj.keys(); iterator
					.hasNext();) {
				String string = (String) iterator.next();
				mInsiderList.put(string, obj.getJSONObject(string));
			}			
		}catch(Exception ex) {}
	}

	public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {
		
		
		if(mShowingWhat.containsKey(pos-1))
		{
			Util.log("A1");
			mShowingWhat.remove(pos-1);
			mAdapter.notifyDataSetChanged();
		}
		else
		{
			Util.log("A2");
			mShowingWhat.put(pos-1, true);
			mAdapter.notifyDataSetChanged();
		}
	}
	
	
}
