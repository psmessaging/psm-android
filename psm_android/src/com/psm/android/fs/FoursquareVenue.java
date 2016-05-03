package com.psm.android.fs;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FoursquareVenue extends Activity {

	
	private Handler mHandler;
	private LayoutInflater mInflater;
	private Context mContext;
	
	private ExpandableListView mListView;
	private VenueItemsAdapter mAdapter;
	
	private String venueId;
	
	private JSONObject mjsonVenue;
	
	
	private TextView mTxtName,mTxtAddress,mTxtCity,mTxtHereNow,mTxtTips;
	private TextView mTxtTip, mTxtMayor;
	private ImageView mImgTip, mImgMayor;
	
	private Button mBtnCheckin;
	private ImageView mIcon;
	private ProgressBar mProgress;
	
	private ArrayList<Bitmap> mHereNow = new ArrayList<Bitmap>();
	private HashMap<String, Bitmap> mPhotos = new HashMap<String, Bitmap>();
	private final static int[] mHereNowImages = { R.id.fsv_hn_1, R.id.fsv_hn_2, R.id.fsv_hn_3,R.id.fsv_hn_4,R.id.fsv_hn_5,R.id.fsv_hn_6};
	private final static String[] mGroupTitles = { "people here now", "tips here", "photos" };
	
	
	private int pcount=0, tcount=0, phcount=0,mcount=0;
	
	private final static int[] mItemCount = { 1, 3, 1, 1 };
	
	private final static int mCount = 6;
	private PhotoAdapter mPhotoAdapter;// = new PhotoAdapter(mHereNow);
	private GridView mHereNowGridView;
	private JSONObject mJsonFromVenue = null;
	private LinearLayout mHereLayout,mTipLayout, mMayorLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_venue);
		
		if(getIntent().getExtras() != null)
			venueId = getIntent().getExtras().getString("com.psm.android.id");
		
		if(venueId == null) finish();
		try {
			mJsonFromVenue = new JSONObject(getIntent().getExtras().getString("com.psm.android.json"));
		}catch(Exception ex){}
		
		mHandler = new Handler();
		mContext = this;
		
		mInflater = getLayoutInflater();
		mAdapter = new VenueItemsAdapter();
		mListView = (ExpandableListView)findViewById(R.id.fsv_listview);
		mListView.setGroupIndicator(null);
		mListView.setAdapter(mAdapter);
		mTxtHereNow = (TextView)findViewById(R.id.fsv_herenow);
		mTxtTips = (TextView)findViewById(R.id.fsv_tips);
		mProgress = (ProgressBar)findViewById(R.id.fsv_progress);
		mHereLayout = (LinearLayout)findViewById(R.id.fsv_herenow_layout);
		mTipLayout = (LinearLayout)findViewById(R.id.fsv_tip_layout);
		mTxtTip = (TextView)findViewById(R.id.fsv_tiptext);
		mImgTip = (ImageView)findViewById(R.id.fsv_tipicon);
		mTxtMayor = (TextView)findViewById(R.id.fsv_mayor_name);
		mImgMayor = (ImageView)findViewById(R.id.fsv_mayor_icon);
		mMayorLayout = (LinearLayout)findViewById(R.id.fsv_mayor_layout);
		
		mTxtName = (TextView)findViewById(R.id.fsv_title);
		mTxtAddress = (TextView)findViewById(R.id.fsv_address);
		mTxtCity = (TextView)findViewById(R.id.fsv_city);
		mIcon = (ImageView)findViewById(R.id.fsv_icon);
		mBtnCheckin = (Button)findViewById(R.id.fsv_btncheckin);
		mHereNowGridView = (GridView)findViewById(R.id.fsv_herenow_grid);
		
		mPhotoAdapter = new PhotoAdapter(mHereNow);
		//mHereNowGridView.setAdapter(mPhotoAdapter);
		
		LinearLayout infoLayout = (LinearLayout)findViewById(R.id.fsv_layout);
		infoLayout.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showMap();
			}
		});
		
		
		mBtnCheckin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				if(mjsonVenue == null) return;
				Intent intent = new Intent(FoursquareVenue.this, FoursquareCheckin.class);
				intent.putExtra("com.psm.android.venue", mjsonVenue.toString());
				startActivity(intent);
				
			}
		});
		
		loadVenue(venueId);
		try {
			if(mJsonFromVenue != null)
			{
				Util.log(mJsonFromVenue.toString());
				JSONObject fvenue = mJsonFromVenue.getJSONObject("venue");
				mTxtName.setText(fvenue.getString("name"));
				JSONObject location = fvenue.getJSONObject("location");
				mTxtAddress.setText(location.getString("address"));
				mTxtCity.setText(location.getString("city") + ", " + location.getString("state"));
			}
		}catch(Exception ex) { Util.log( ex.getMessage() ); }
	}
	
	private void showMap()
	{
		if(mjsonVenue == null) return;
		
		Intent intent = new Intent(FoursquareVenue.this, FoursquareMap.class);
		intent.putExtra("com.psm.android.venue", mjsonVenue.toString());
		startActivity(intent);
		
		
	}
	
	private JSONObject getVenue()
	{
		return mjsonVenue;
	}
		
	
	private void loadIcon() {
		
		try {
			
			final Bitmap bitmap = 
					ACacheUtil.getBitmap(Foursquare.getIconUrl(mjsonVenue.getJSONArray("categories"), "88"));
			mHandler.post( new Runnable() {
				public void run() {
					mIcon.setImageBitmap(bitmap);
				}
			});
		} catch(Exception ex) {}
		
	}
	
	private void loadVenue(String id)
	{
		
		final String fullUrl = Foursquare.getFullUrl("venues/" + id);
		Util.log("Loadurl: " + fullUrl);
		
		
		mProgress.setVisibility(View.VISIBLE);
		
		new Thread( new Runnable() {
			public void run() {	
				try {
					String results = ACacheUtil.getUrl(fullUrl, true);
					
					if(results == null) {
						mHandler.post(new Runnable() {
							
							public void run() {
								Toast.makeText(mContext, "Error loading Venue", 3000).show();
								finish();
							}
						});
						return;
					}
					
					mjsonVenue = Foursquare.getVenue(new JSONObject(results));
					if(mjsonVenue == null) finish();
					loadIcon();
					pcount = mjsonVenue.getJSONObject("hereNow").getInt("count");
					tcount = mjsonVenue.getJSONObject("tips").getInt("count");
					mcount = mjsonVenue.getJSONObject("mayor").getInt("count");
					
					final JSONObject fvenue = mjsonVenue;
					
					mHandler.post( new Runnable() {
						public void run() {
							try {
								getHereNowPhotos();
								//mPhotoAdapter.notifyDataSetChanged();
								Util.log("venue: " + fvenue.toString());
								mTxtName.setText(fvenue.getString("name"));
								JSONObject location = mjsonVenue.getJSONObject("location");
								mTxtAddress.setText(location.getString("address"));
								mTxtCity.setText(location.getString("city") + ", " + location.getString("state"));
								if(pcount == 0)
									mHereLayout.setVisibility(View.GONE);
								else
									mHereLayout.setVisibility(View.VISIBLE);
								
								if(tcount == 0)
									mTipLayout.setVisibility(View.GONE);
								else
									mTipLayout.setVisibility(View.VISIBLE);
								
								if(mcount == 0)
									mMayorLayout.setVisibility(View.GONE);
								else
									mMayorLayout.setVisibility(View.VISIBLE);
								
								fillTip(fvenue, tcount);
								fillMayor(fvenue);
								
								mTxtHereNow.setText(String.valueOf(pcount) + (pcount == 1 ? " person" : " people") + " here now");
								mTxtTips.setText(String.valueOf(tcount) + (tcount == 1 ? " tip" : " tips") + " here");
							}catch(Exception ex) { Util.log( ex.getMessage() ); }
							mProgress.setVisibility(View.GONE);
							mAdapter.notifyDataSetChanged();
							
						}
					});
					
				}catch(Exception ex){
					Util.log(ex.getLocalizedMessage());
					finish();
					
				}
			}
			
		}).start();
		
	}
	
	
	//Here Now, Tips, Photos, Mayor
	private void fillTip(JSONObject venue, int tipCount)
	{
		if(tipCount > 0)
		{
			try {
				
				JSONArray tips = venue.getJSONObject("tips").getJSONArray("groups").getJSONObject(0).getJSONArray("items");
				JSONObject tip = tips.getJSONObject(0);
				JSONObject user = tip.getJSONObject("user");
				mTxtTip.setText(tip.getString("text"));
				String url = user.getString("photo");
				loadImage(mImgTip, url);
			}catch(Exception ex) {}
			
		}
	}
	
	private void fillMayor(JSONObject venue)
	{
		try {
			
			JSONObject mayor = venue.getJSONObject("mayor");
			if(mayor.getInt("count") > 0)
			{
				JSONObject user = mayor.getJSONObject("user");
				String name = user.has("firstName") ? user.getString("firstName") : "";
				name += user.has("lastName") ? " " + user.getString("lastName") : "";
				mTxtMayor.setText(name);
				String url = user.getString("photo");
				loadImage(mImgMayor, url);
			}
		}
		catch(Exception ex) {}
	}
	private class VenueItemsAdapter extends BaseExpandableListAdapter
	{
		
		
		public Object getChild(int pos, int arg1) {
			
			return 3; 
		}

		public long getChildId(int arg0, int arg1) {
			return 0;
		}

		
		public View getChildView(int pos, int arg1, boolean arg2, View convertView,
				ViewGroup arg4) {
			View view = convertView;
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.foursquare_cell_herenow, null);
				GridView holder = (GridView)view.findViewById(R.id.fsc_herenow_grid);
				view.setTag(holder);
			}
			
			GridView holder = (GridView)view.getTag();
			holder.setAdapter(mPhotoAdapter);
			mPhotoAdapter.notifyDataSetChanged();
			return view;
		}
		
		
		public int getChildrenCount(int position) {
			return 1;
		}

		public Object getGroup(int arg0) {
			return null;
		}

		public int getGroupCount() {
			return 3;
		}

		public long getGroupId(int position) {
			return 0;
		}

		public View getGroupView(int position, boolean arg1, View convertView,
				ViewGroup arg3) {
			
			View view = convertView;
			if(view == null)
			{
				TextView v = new TextView(mContext);
				view = v;
			}
			
			TextView v = (TextView)view;
			
			v.setText(String.valueOf(pcount) + " " + mGroupTitles[position]);
			v.setTextColor(Color.WHITE);
			mListView.expandGroup(position);
			return view;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isChildSelectable(int arg0, int arg1) {
			return false;
		}
		
		
		
	}
	
	private void getHereNowPhotos()
	{
		try {
			
			JSONObject mHereNow = mjsonVenue.getJSONObject("hereNow");
			JSONArray mGroups = mHereNow.getJSONArray("groups");
			int x = 0;
			
			for( int i = 0; i < mGroups.length() ; i++)
			{	
				
				int count = mGroups.getJSONObject(i).getInt("count");
				if(count > 0)
				{
					JSONArray mItems = mGroups.getJSONObject(i).getJSONArray("items");
					
					for(int k = 0; k < count; k++)
					{
						if(x > mCount-1) break;
						
						JSONObject user = mItems.getJSONObject(k).getJSONObject("user");
						String url = user.getString("photo");
						ImageView img = (ImageView)findViewById(mHereNowImages[x]);
						loadImage(img, url);
						Util.log(url);
						x++;
						
					}
				}
				if(x > mCount-1) break;
				
			}
					
		}catch(Exception ex){
			Util.log("ERR: " + ex.getMessage());
		}
	}
	
	private void loadImage(final ImageView imgView, final String url)
	{
		new Thread(new Runnable() {
			public void run() {
				final Bitmap bitmap = ACacheUtil.getBitmap(url);
				mHandler.post(new Runnable() {
					public void run() {
						imgView.setImageBitmap(bitmap);
					}
				});
			}
		}).start();
		
	}
	
	private class PhotoAdapter extends BaseAdapter
	{
		private ArrayList<Bitmap> mList;

		public PhotoAdapter(ArrayList<Bitmap> mList) {
			super();
			this.mList = mList;
		}
		
		public int getCount() {
			if(mList == null) return 0;
			return mList.size();
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
				view = new ImageButton(mContext);
				Util.log("USERICON");
			}
			
			Bitmap bitmap = mHereNow.get(position);
			((ImageButton)view).setImageBitmap(bitmap);
			return view;
		}
		
	}
	
	
}
