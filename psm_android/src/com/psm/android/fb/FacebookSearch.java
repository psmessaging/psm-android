package com.psm.android.fb;

import java.net.URLEncoder;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.R.string;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;



public class FacebookSearch extends Activity implements OnItemClickListener{

	private LayoutInflater mInflater;
	private Context mContext;
	private Handler mHandler;
	
	private EditText mTxtSearch;
	private Button mBtnSearch;
	
	private ListView mList;
	
	private FbSearchAdapter mAdapter;
	
	private LocationManager mLocMan = null;
	private Location mLocation = null;
	private LocationListener mListener = null;
	
	private JSONArray mJsonPlaces;
	private String mPagingNext, mPagingPrev;
	private LinearLayout mProgress;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_search);
		mContext = this;
		mInflater = getLayoutInflater();
		mHandler = new Handler();
		mProgress = (LinearLayout)findViewById(R.id.fb_search_progress);
		mLocMan = (LocationManager)getSystemService(LOCATION_SERVICE);
		mTxtSearch = (EditText)findViewById(R.id.fb_search_text);
		mBtnSearch = (Button)findViewById(R.id.fb_search_btn);
		mList = (ListView)findViewById(R.id.fb_search_list);
		mAdapter = new FbSearchAdapter();
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
		mAdapter.notifyDataSetChanged();
		doSearch();
		
		mBtnSearch.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
				doSearch();
			}
		});
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
		//https://graph.facebook.com/search?q=coffee&type=place&center=37.76,-122.427&distance=1000
		mProgress.setVisibility(View.VISIBLE);
		mList.setVisibility(View.INVISIBLE);
		
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
		new Thread(new Runnable() {
			public void run() {				
				Location location = flocation;
				String latlng = String.valueOf(location.getLatitude()) + "," + 
											String.valueOf(location.getLongitude());
				String query = fquery;
				
				
				Bundle params = new Bundle();
				if(query != null)
				{
					if(query.length() > 0)
						params.putString("q", URLEncoder.encode(query));
				}
				
				params.putString("type", "place");
				params.putString("center", URLEncoder.encode(latlng));
				params.putString("distance", "3218");
				params.putString("limit", "25");
				final String results = ACacheUtil.getUrl(AFacebook.getFullUrl("search", params), true);
				Util.log(results);
				try {
					
					mHandler.post(new Runnable() {
						public void run() {
							try {
								mJsonPlaces = new JSONObject(results).getJSONArray("data");
								mAdapter.notifyDataSetChanged();
								mProgress.setVisibility(View.INVISIBLE);
								mList.setVisibility(View.VISIBLE);
							}catch(Exception ex){}
						}
					});
					
				}catch(Exception ex) {
					
					mHandler.post(new Runnable() {
						public void run() {
							mProgress.setVisibility(View.INVISIBLE);
							mList.setVisibility(View.VISIBLE);
						}
					});
				}
			}
		}).start();
		
		
		
	}
	
	private class FbSearchAdapter extends BaseAdapter
	{

		public int getCount() {
			if(mJsonPlaces == null) return 0;
			return mJsonPlaces.length();
		}

		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if(view == null)
			{
				view = mInflater.inflate(R.layout.facebook_cell_location, null);
				FBHolder holder = new FBHolder();
				holder.txtName = (TextView)view.findViewById(R.id.fb_cell_location_name);
				holder.txtOther = (TextView)view.findViewById(R.id.fb_cell_location_other);
				//holder.txtCity = (TextView)view.findViewById(R.id.fb_cell_location_citystate);
				view.setTag(holder);
			}
				
			FBHolder holder = (FBHolder)view.getTag();
			try {
				
				JSONObject place = mJsonPlaces.getJSONObject(position);
				holder.txtName.setText(place.getString("name"));
				JSONObject location = place.getJSONObject("location");
				String streetstuff = "";
				
				if(location.has("street"))
					streetstuff = location.getString("street") + "\n";
				if(location.has("city") && location.has("state"))
					streetstuff += location.getString("city") + ", " + 
										location.getString("state");
				holder.txtOther.setText(streetstuff);
				
			}catch(Exception ex) {}
			return view;
		}
		
	}
	
	private class FBHolder
	{
		TextView txtName;
		TextView txtOther;
		TextView txtCity;
		
	}

	public void onItemClick(AdapterView<?> adapter, View view, int position, long arg3) {
		try {
			
			Intent data = new Intent();
			data.putExtra("com.psm.android.venue", mJsonPlaces.getJSONObject(position).toString());
			setResult(RESULT_OK, data);
			finish();
			
		}catch(Exception ex) {}
	}
}
