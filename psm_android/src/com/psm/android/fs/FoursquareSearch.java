package com.psm.android.fs;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.R.string;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.inputmethodservice.InputMethodService;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class FoursquareSearch extends Activity implements OnItemClickListener {

	private Handler mHandler;
	private Context mContext;
	
	private FSListViewAdapter mAdapter;
	
	private EditText mTxtSearch;
	private ListView mListView;
	private LinearLayout mProgress;
	
	
	
	private JSONArray mJsonVenues;
	private LayoutInflater mInflater;
	
	private Button mBtnSearch;
	
	private LocationManager mLocMan = null;
	private Location mLocation = null;
	private LocationListener mListener = null;
	
	private boolean isFirstStep = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_search);
		
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			//See if it is first step of checkin process
			if(extras.containsKey("com.psm.android.checkin"))
				isFirstStep = extras.getBoolean("com.psm.android.checkin");
		}
		
		mHandler = new Handler();
		mContext = this;
		mInflater = getLayoutInflater();
		mLocMan = (LocationManager)getSystemService(LOCATION_SERVICE);
		
		mTxtSearch = (EditText)findViewById(R.id.fs_search_text);
		mListView = (ListView)findViewById(R.id.fs_search_list);
		mAdapter = new FSListViewAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mProgress = (LinearLayout)findViewById(R.id.fs_search_progress);
		
		mBtnSearch = (Button)findViewById(R.id.fs_search_button);
		mBtnSearch.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				try {
					//mLocation = AMapUtil.getLocationFromCity("Los Angeles, CA");
					((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
								.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
					doSearch();
				}catch(Exception ex){}
			}
		});
		doSearch();
	}
	
	/*
	 * GPS Providers
	 * 
	 */
	
	private String getBestGPSProvider()
	{
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setSpeedRequired(false);
		criteria.setAltitudeRequired(false);
		
		String provider = mLocMan.getBestProvider(criteria, true);
		return provider;
		
	}
	
	private Location getLastLocation()
	{
		if(mLocation != null) return mLocation;
		
		String provider = getBestGPSProvider();
		if(provider != null)
		{
			Location loc = mLocMan.getLastKnownLocation(provider);
			if(mLocation == null && loc != null)
				mLocation = loc;
			
			if(loc != null)
				return loc;
		}
		
		if(mLocation != null)
			return mLocation;
		else
			return null;
		
	}
	
	private void startGPSListen()
	{
		String provider = getBestGPSProvider();
		if(provider == null)
		{
			Toast.makeText(this, mContext.getString(R.string.no_location), Toast.LENGTH_SHORT).show();
			return;
		}
		mLocMan.requestLocationUpdates(provider, 30000, 100, mListener = new LocationListener() {
			
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			public void onProviderEnabled(String provider) {
			}
			
			public void onProviderDisabled(String provider) {
				mLocMan.removeUpdates(mListener);
			}
			
			public void onLocationChanged(Location location) {
				if(location != null) mLocation = location;
			}
		});
	}
	
	private void stopGPSListen()
	{
		
		Util.log("Stopping GPS Listener");
		if(mListener == null) return;
		mLocMan.removeUpdates(mListener);
		
	}
	
	/*
	 * 
	 * Events
	 */
	
	@Override
	protected void onResume() {
		startGPSListen();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		stopGPSListen();
		super.onPause();
	}
	
	private void doSearch()
	{
		
		mProgress.setVisibility(View.VISIBLE);		
		mListView.setVisibility(View.INVISIBLE);
		
		
		String txtSearch = mTxtSearch.getText().toString();
		while(txtSearch.endsWith(","))
		{
			if(txtSearch.length() > 0)
				txtSearch = txtSearch.substring(0, txtSearch.length()-1);
		}
		
		Location location = getLastLocation();
		String[] parsed = txtSearch.length() > 0 ? AHttpUtil.parseCatAndCity(txtSearch) : null;
		if(parsed != null && location == null)
		{
			try {
				if(parsed.length > 1)
				{
					if(parsed[1] != null)
						location = AHttpUtil.getLatitudeFromCity(parsed[1]);
					txtSearch = parsed[0];
				}
				else
				{
					location = AHttpUtil.getLatitudeFromCity(parsed[0]);
					txtSearch = "";
				}
			}catch(Exception ex) {Util.log(ex.toString());}
		}
		else
		{
			try {
				if(parsed.length > 1)
				{
					if(parsed[1] != null)
						location = AHttpUtil.getLatitudeFromCity(parsed[1]);
					txtSearch = parsed[0];
				}
			}catch(Exception ex) {}
		}
		
		if(getLastLocation() == null && location == null) 
		{
			mProgress.setVisibility(View.INVISIBLE);
			Toast.makeText(mContext, "Unable to determine location", 3000).show();
			return;
		}
		final Location flocation = location;
		final String fquery = txtSearch;
		new Thread( new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				String[] parsed = AHttpUtil.parseCatAndCity(mTxtSearch.getText().toString());
				params.putString("ll", URLEncoder.encode(Foursquare.getLL(flocation)));
				
				if(fquery != null)
				{
					if(fquery.length() > 0)
						params.putString("query", fquery);
				}
				
				//params.putString("radius", "16090");
				params.putString("limit", "25");
				params.putString("intent", "checkin");				
				
				
				Util.log(Foursquare.getFullUrl("venues/search", params));
				final String results = ACacheUtil.getUrl(Foursquare.getFullUrl("venues/search", params), true);
				Util.log(results);
				mHandler.post( new Runnable() {
					public void run() {
						try {
							mProgress.setVisibility(View.INVISIBLE);		
							mListView.setVisibility(View.VISIBLE);
							mJsonVenues = new JSONObject(results).getJSONObject("response").getJSONArray("venues");
							mAdapter.notifyDataSetChanged();
							mListView.setSelection(0);
						}catch(Exception ex){ 
							mJsonVenues = null;
						}
					}
				});
				
				
			}
		}).start();
		
		
		
		
	}
	
	private class FSListViewAdapter extends BaseAdapter
	{
		public int getCount() {
			if(mJsonVenues == null) return 0;
			return mJsonVenues.length();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			if(view == null)
			{
				view = mInflater.inflate(R.layout.foursquare_cell_venue, null);
				VHolder holder = new VHolder();
				holder.txtTitle = (TextView)view.findViewById(R.id.fs_loc_name);
				holder.txtAddress = (TextView) view.findViewById(R.id.fs_loc_address);
				holder.txtCityState = (TextView) view.findViewById(R.id.fs_loc_dist);
				holder.imgIcon = (ImageView) view.findViewById(R.id.fs_loc_icon);
				view.setTag(holder);
			}
			
			VHolder holder = (VHolder)view.getTag();
			try {
				
				JSONObject venue = mJsonVenues.getJSONObject(position);
				JSONObject location = venue.getJSONObject("location");
				holder.txtTitle.setText(venue.getString("name"));
				if(location.has("address"))
					holder.txtAddress.setText(location.getString("address"));
				else
					holder.txtAddress.setText("");
				if(location.has("city") && location.has("state"))
					holder.txtCityState.setText(location.getString("city") + ", " +
							location.getString("state"));
				else
					holder.txtCityState.setText("");
				
				
				
				JSONArray cats = venue.getJSONArray("categories");
				if(cats.length() > 0)
					getBitmap(Foursquare.getIconUrl(venue.getJSONArray("categories")), holder.imgIcon);
				else
					holder.imgIcon.setImageResource(R.drawable.icon);
				
			}catch(Exception ex) {
				Util.log(ex.getMessage());
			}
			return view;
		}
		
	}
	
	private void getBitmap(final String url, final ImageView view)
	{
		
		new Thread(new Runnable() {
			public void run() {
				final Bitmap bitmap = ACacheUtil.getBitmap(url);
				mHandler.post(new Runnable() {
					public void run() {
						view.setImageBitmap(bitmap);
					}
				});
			}
		}).start();
		
	}
	
	private class VHolder
	{
		TextView txtTitle;
		TextView txtAddress;
		TextView txtCityState;
		ImageView imgIcon;
		
	}

	public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
		
		Intent data = new Intent();
		try {
			if(!isFirstStep)
			{
				data.putExtra("com.psm.android.venue", mJsonVenues.getJSONObject(position).toString());
				setResult(RESULT_OK, data);
				finish();
			}
			else
			{
				Intent intent = new Intent(FoursquareSearch.this, FoursquareCheckin.class);
				intent.putExtra("com.psm.android.venue", mJsonVenues.getJSONObject(position).toString());
				startActivity(intent);
				finish();
			}
		} catch (JSONException e) {	}
		
	}
}
