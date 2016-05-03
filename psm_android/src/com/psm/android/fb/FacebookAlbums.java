package com.psm.android.fb;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.R;
import com.psm.android.R.id;
import com.psm.android.R.layout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class FacebookAlbums extends Activity implements OnItemClickListener {

	private Context mContext;
	private Handler mHandler;
	private LayoutInflater mInflater;
	
	private ListView mList;
	private FbAlbumAdapter mAdapter;
	
	private JSONArray mJsonAlbums;
	
	private LinearLayout mProgress;
	
	private String mFbId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_albums);
		
		mContext = this;
		mHandler = new Handler();
		mInflater = getLayoutInflater();
		mAdapter = new FbAlbumAdapter();
		mProgress = (LinearLayout)findViewById(R.id.fb_album_progress);
		
		mList = (ListView)findViewById(R.id.fb_album_list);
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
		
		Bundle extras = getIntent().getExtras();
		if(extras != null)
			mFbId = extras.getString("com.psm.android.id");
		
		 if(mFbId == null)
			 getAlbums("me");
		 else
			 getAlbums(mFbId);
	}
	
	private void getAlbums(final String fbId)
	{

		mProgress.setVisibility(View.VISIBLE);
		mList.setVisibility(View.INVISIBLE);
		
		new Thread(new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				params.putString("access_token", AFacebook.getAccessToken());
				String path = AFacebook.getFullUrl(fbId + "/albums", params);
				final String results = ACacheUtil.getUrl(path, false);
				mHandler.post(new Runnable() {
					public void run() {
						try {
							mProgress.setVisibility(View.INVISIBLE);
							mList.setVisibility(View.VISIBLE);
							mJsonAlbums = new JSONObject(results).getJSONArray("data");
							mAdapter.notifyDataSetChanged();
						}catch(Exception ex) {}
					}
				});
			}
		}).start();
		
		
	}
	
	private class FbAlbumAdapter extends BaseAdapter
	{

		public int getCount() {
			
			if(mJsonAlbums == null) return 0;
			return mJsonAlbums.length();
			
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup viewgroup) {
			
			View view = convertView;
			if(view == null)
			{
				
				view = mInflater.inflate(R.layout.facebook_cell_albums, null);
				FBACHolder holder = new FBACHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.fb_cell_album_icon);
				holder.txtTitle = (TextView)view.findViewById(R.id.fb_cell_album_title);
				holder.txtCount = (TextView)view.findViewById(R.id.fb_cell_album_count);
				holder.txtDate = (TextView)view.findViewById(R.id.fb_cell_album_date);
				view.setTag(holder);
				
			}
			
			FBACHolder holder = (FBACHolder)view.getTag();
			try {
				JSONObject album = mJsonAlbums.getJSONObject(position);
				holder.id = album.getString("id");
				//if(album.has("cover_photo"))
					//holder.imgIcon.setImageBitmap(getCoverPhoto(album.getString("cover_photo")));
				//else
				//holder.imgIcon.setImageBitmap(getCoverPhoto(album.getString("id")));
				getCoverPhoto(album.getString("id"), holder.imgIcon );
				holder.txtTitle.setText(album.getString("name"));
				if(album.has("count"))
				{
					holder.txtCount.setText(album.getString("count") + " photos");
					if(album.has("updated_time"))
						holder.txtDate.setText(AFacebook.formatTime(album.getString("updated_time")));
				}
				else
				{
					holder.txtCount.setText("");
					holder.txtDate.setText("");
				}
					
			}catch(Exception ex) {}
			
			return view;
		}
		
	}
	
	private void getCoverPhoto(final String id, final ImageView imageView)
	{
		new Thread(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				final Bitmap bitmap = ACacheUtil.getBitmap(AFacebook.BASE_PATH + id + "/picture");
				mHandler.post(new Runnable() {
					public void run() {
						imageView.setImageBitmap(bitmap);
					}
				});
				
			}
		}).start();
		
	}
	
	private class FBACHolder
	{
		String id;
		ImageView imgIcon;
		TextView txtTitle;
		TextView txtCount;
		TextView txtDate;
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		FBACHolder holder = (FBACHolder)arg1.getTag();
		
		Intent intent = new Intent(FacebookAlbums.this, FacebookViewAlbum.class);
		intent.putExtra("com.psm.android.id", holder.id);
		startActivity(intent);
		
	}
}
