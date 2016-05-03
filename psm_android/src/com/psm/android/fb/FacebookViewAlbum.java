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
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemClickListener;

public class FacebookViewAlbum extends Activity implements OnItemClickListener{

	private Handler mHandler;
	private Context mContext;
	
	private GridView mGrid;
	private APAdapter mAdapter;
	
	JSONArray mAlbumPhotos;
	
	private String mAlbumId = null;
	private LinearLayout mProgress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_albums_view);
		mHandler = new Handler();
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			mAlbumId = extras.getString("com.psm.android.id");
		}
		else
		{
			finish();
			return;
		}
		mProgress = (LinearLayout)findViewById(R.id.fb_album_view_progress);
		mContext = this;
		mGrid = (GridView)findViewById(R.id.fb_album_view_grid);
		mAdapter = new APAdapter();
		mGrid.setAdapter(mAdapter);
		mGrid.setOnItemClickListener(this);
		getAlbumImages();
		
	}
	
	private void getAlbumImages()
	{
		mProgress.setVisibility(View.VISIBLE);
		mGrid.setVisibility(View.INVISIBLE);
		
		new Thread(new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				String url = AFacebook.getFullUrl(mAlbumId + "/photos", params);
				final String results = ACacheUtil.getUrl(url, true);
					mHandler.post(new Runnable() {
						public void run() {
							try {
								mProgress.setVisibility(View.INVISIBLE);
								mGrid.setVisibility(View.VISIBLE);
								mAlbumPhotos = new JSONObject(results).getJSONArray("data");
								mAdapter.notifyDataSetChanged();
							}catch(Exception ex) {}
						}
					});
			}
		}).start();
		
		
	}
	
	
	private class APAdapter extends BaseAdapter
	{

		public int getCount() {
			if(mAlbumPhotos == null) return 0;
			return mAlbumPhotos.length();
		}

		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		public View getView(int pos, View convertView, ViewGroup arg2) {
			ImageView view = (ImageView)convertView;
			if(view == null)
			{
				view = new ImageView(mContext);
				view.setPadding(10, 10, 10, 10);				
			}
			
			try {
				
				JSONObject photo = mAlbumPhotos.getJSONObject(pos);
				JSONArray imgArray = photo.getJSONArray("images");
				String source = imgArray.getJSONObject(imgArray.length()-1).getString("source");
				view.setImageBitmap(ACacheUtil.getBitmap(source));//mContext.getResources().getDrawable(R.drawable.ic_launcher));
				view.setTag(photo.getString("id"));
			}catch(Exception ex) {}
			
			return view;
		}
		
	}


	public void onItemClick(AdapterView<?> arg0, View view, int pos, long arg3) {
		Intent intent = new Intent(FacebookViewAlbum.this, FacebookViewPhoto.class);
		
		intent.putExtra("com.psm.android.id", (String)view.getTag());
		try {
			intent.putExtra("com.psm.android.json", mAlbumPhotos.getJSONObject(pos).toString());
		}catch(Exception ex) {}
		startActivity(intent);
		
		
	}
	
}
