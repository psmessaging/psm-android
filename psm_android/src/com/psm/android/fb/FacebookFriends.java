package com.psm.android.fb;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.GroupCreate;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.Util.JsonNameComparator;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class FacebookFriends extends Activity implements OnItemClickListener {

	private final static int MAX_THREADS = 10;
	
	private LayoutInflater mInflater;
	private Context mContext;
	private Handler mHandler;
	
	private ListView mList;
	private FFAdapter mAdapter;
	private EditText mEditName;
	
	private JSONArray mJsonFriends,mFilteredFriends = new JSONArray();
	private ArrayList<JSONObject> mSortedFriends = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> mSortedFilteredFrineds = new ArrayList<JSONObject>();
	
	private Button mGroupButton;
	
	private boolean isFiltered = false;
	
	private HashMap<String, Bitmap> mIcons = new HashMap<String, Bitmap>();
	private int mCurrentThreadCount = 0;
	private Stack<String> mQueue = new Stack<String>();
	
	private String mId = null;
	private Thread mThread = null;
	private boolean isQueued = false;
	
	private boolean doNavRefreshAfter = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_friends);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null)
		{
			Toast.makeText(this, "Unable to load friends list.", 2000).show();
			finish();
			return;
		}
		
					
		mGroupButton = (Button)findViewById(R.id.fb_friends_i3group);
		
		if(!Util.mInsider.isLoggedIn())
			mGroupButton.setVisibility(View.GONE);
		else
			mGroupButton.setVisibility(View.VISIBLE);
		
		mGroupButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				Intent intent = new Intent(FacebookFriends.this, GroupCreate.class);
				startActivity(intent);
			}
		});
		
		mId = extras.getString("com.psm.android.id");
		doNavRefreshAfter = extras.getBoolean("com.psm.android.donav");
		
		mInflater = getLayoutInflater(); 
		mHandler = new Handler();
		mContext = this;
		mEditName = (EditText)findViewById(R.id.fb_friend_editsearch);
		if(mId.compareTo("me") == 0)
			mEditName.setVisibility(View.VISIBLE);
		else
			mEditName.setVisibility(View.GONE);
		mEditName.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			public void afterTextChanged(Editable s) {
				String search = s.toString();
				if(search.length() > 0)
				{
					isFiltered = true;
					filterResults(search);
				}
				else
				{
					isFiltered = false;
				}
					
					
			}
		});
		mList = (ListView)findViewById(R.id.fb_friends_list);
		mAdapter = new FFAdapter();
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
		//mList.setOnItemClickListener(this);
		
		
		getFriends(mId);
		mAdapter.notifyDataSetChanged();
		
	}
	
	private void getFriends(String userId)
	{
		
		Bundle params = new Bundle();
		params.putString("access_token", AFacebook.getAccessToken());
		params.putString("limit", "50");
		String path = AFacebook.getFullUrl(userId + "/friends", params);
		String results = ACacheUtil.getUrl(path, false);
		Util.log(results);
		try {
			mJsonFriends = new JSONObject(results).getJSONArray("data");
			mSortedFriends.clear();
			for(int i =0;i < mJsonFriends.length();i++)
			{
				mSortedFriends.add(mJsonFriends.getJSONObject(i));
			}
			Collections.sort(mSortedFriends, new Util.JsonNameComparator());
		}catch(Exception ex) {}
		
	}
	
	private class FFAdapter extends BaseAdapter
	{
		
		public int getCount() {
			if(isFiltered)
			{
				
				if(mFilteredFriends == null) return 0;
				return mFilteredFriends.length();
			}
			else
			{
				if(mSortedFriends == null) return 0;
				return mSortedFriends.size();
			}
			
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
				view = mInflater.inflate(R.layout.facebook_cell_friend, null);
				FFHolder holder = new FFHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.fb_friendc_icon);
				holder.txtName = (TextView)view.findViewById(R.id.fb_friendc_name);
				view.setTag(holder);
			}
			FFHolder holder = (FFHolder)view.getTag();
			
			try {
				
				
				
				
				JSONObject jsonFriend;
				if(!isFiltered)
				{
					jsonFriend = mSortedFriends.get(pos);
					holder.fbid = jsonFriend.getString("id");
					holder.name = jsonFriend.getString("name");
				}
				else
				{
					jsonFriend = mFilteredFriends.getJSONObject(pos);
					holder.fbid = jsonFriend.getString("uid");
					holder.name = jsonFriend.getString("name");
				}
				
				holder.txtName.setText(jsonFriend.getString("name"));
				holder.imgIcon.setImageResource(R.drawable.facebookuser);
				
				
				getBitmap(holder.fbid, holder.imgIcon);
			}catch(Exception ex) {}
			
			return view;
		}
		
	}
	
	private void getBitmap(final String id, final ImageView imageview)
	{
		if(mCurrentThreadCount >= MAX_THREADS)
		{
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				mCurrentThreadCount++;
				if(!mIcons.containsKey(id))
				{
					String url = AFacebook.getFullUrl(id + "/picture", null);
					final Bitmap bitmap = ACacheUtil.getBitmap(url);
					mIcons.put(id, bitmap);
					mHandler.post(new Runnable() {
						public void run() {
							imageview.setImageBitmap(bitmap);
						}
					});
				}
				else
				{
					final Bitmap bitmap = mIcons.get(id);
					mHandler.post(new Runnable() {
						public void run() {
							imageview.setImageBitmap(bitmap);
						}
					});
				}
				mCurrentThreadCount--;
			}
		}).start();
	}
	
	
	
	private class FFHolder
	{
		String fbid;
		String name;
		
		ImageView imgIcon;
		TextView txtName;
	}

	public void onItemClick(AdapterView<?> list, View view, int pos, long arg3) {
		Intent data = new Intent();
		data.putExtra("com.psm.android.fbid", ((FFHolder)view.getTag()).fbid);
		data.putExtra("com.psm.android.name", ((FFHolder)view.getTag()).name);
		
		if(doNavRefreshAfter)
		{			
			data.setAction("com.psm.android.NAVFRIEND");
			sendBroadcast(data);
		}
		
		setResult(RESULT_OK, data);
		finish();
		
	}
	
	private void filterResults(final String name)
	{
		if(isQueued == true)
			return;
		
		new Thread( new Runnable() {
			public void run() {
				isQueued = true;
				Bundle bundle = new Bundle();
				String fql = "SELECT uid , name, pic_square FROM user WHERE uid IN " +
						"(SELECT uid2 FROM friend WHERE uid1 = me()) AND strpos(lower(name),'" + name.toLowerCase() + "') >= 0 " +
						"ORDER BY name";
				bundle.putString("q", URLEncoder.encode(fql));
				String url = AFacebook.getFullUrl("fql", bundle);
				try {
					final String results = AHttpUtil.openUrl(url);
					Util.log("results " + results);
					mHandler.post(new Runnable() {
						public void run() {
							try {
								mFilteredFriends = new JSONObject(results).getJSONArray("data");
								mSortedFilteredFrineds.clear();
								for(int i=0;i < mFilteredFriends.length();i++)
								{
									mSortedFilteredFrineds.add(mFilteredFriends.getJSONObject(i));
								}
								Collections.sort(mSortedFilteredFrineds, new Util.JsonNameComparator());
								mAdapter.notifyDataSetChanged();
							}catch(Exception ex) {}
						}
					});
				}catch(Exception ex) {}
				isQueued = false;
			}
		}).start();
		
		
	}
	
	
}
