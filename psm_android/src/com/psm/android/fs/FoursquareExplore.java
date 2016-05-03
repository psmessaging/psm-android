package com.psm.android.fs;

import java.net.URLEncoder;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.Holders;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.Holders.LocationCellHolder;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.R.string;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class FoursquareExplore extends Activity implements OnItemClickListener {

	private static final int[] TOP_BUTTONS = {R.id.fse_dist, R.id.fse_reco, R.id.fse_trend};
	private static int[] SUGGEST_BUTTONS = { R.id.fse_specials, R.id.fse_food, R.id.fse_coffee,
					R.id.fse_nightlife, R.id.fse_shops, R.id.fse_ae, R.id.fse_outdoor};
	
	private static final String[] STR_DIST = { "1/4 mi", "1/2 mi", "1 mi", "2 mi", "5 mi", "10 mi" };
	private static final int[] INT_DIST = { 402, 804, 1609, 3218, 8045, 16090 };
	private static final float mm = 1609.3f;
	private int selectedDistIndex = 4;
	
	private Thread mGetThread = null;
	private Handler mHandler = null;
	private LinearLayout mProgress = null;
	private HorizontalScrollView mRecoLayout = null;
	
	private LocationManager mLocMan = null;
	private Location mLocation = null;
	
	private LocationListener mListener = null;
	private LayoutInflater mInflater = null;
	
	private JSONArray mVenueList = null;
		
	private ListView mListView = null;
	private VenueAdapter mAdapter = null;
	
	private boolean isExplore = true;
	private boolean mLoaded = false;
	private String mPath[] = null;
	
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_explor);
		mProgress = (LinearLayout)findViewById(R.id.fse_prog);
		mRecoLayout = (HorizontalScrollView)findViewById(R.id.fse_recoscroll);
		mHandler = new Handler();
		mLocMan = (LocationManager)getSystemService(LOCATION_SERVICE);
		mInflater = getLayoutInflater();
		mListView = (ListView)findViewById(R.id.fse_venlist);
		mAdapter = new VenueAdapter();
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mContext = this;
		
		setupTopButtons();
		setupSuggestButtons();
		explorePath("venues/explore", null);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
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
			Toast.makeText(this, mContext.getString(R.string.no_location), Toast.LENGTH_LONG).show();
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
	
	/*
	 * 
	 * Setup Buttons
	 * 
	 */
	
	private void setupTopButtons()
	{
		int x = 0;
		for(int id : TOP_BUTTONS)
		{
			Button btn = (Button)findViewById(id);
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					topButtonClick((Button)v);
				}
			});
		}
		
	}
	
	/*
	 * 
	 * Button Action Methods
	 * 
	 */
	
	private void setupSuggestButtons()
	{
		for(int id : SUGGEST_BUTTONS)
		{
			ImageButton btn = (ImageButton)findViewById(id);
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					suggestButtonClick((ImageButton)v);
				}
			});
		}
	}
	
	private void topButtonClick(Button btn)
	{
		
		
		switch (btn.getId()) {
		case R.id.fse_dist:
			new AlertDialog.Builder(getParent()).setTitle("Explore within").setSingleChoiceItems(STR_DIST, selectedDistIndex, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(selectedDistIndex == which) dialog.dismiss();
					
					selectedDistIndex = which;
					Button btn = (Button)findViewById(TOP_BUTTONS[0]);
					btn.setText(STR_DIST[which]);
					dialog.dismiss();
					if(mPath != null)
						explorePath(mPath[0], mPath[1]);
					
				}
			}).show();
			
			break;
		case R.id.fse_reco:
			
			isExplore = true;
			mRecoLayout.setVisibility(View.VISIBLE);
			explorePath("venues/explore", null);
			
			btn.setBackgroundColor(Color.BLACK);
			((Button)findViewById(R.id.fse_trend)).setBackgroundResource(R.drawable.btnbg);
			break;
		
		case R.id.fse_trend:
			
			isExplore = false;
			mRecoLayout.setVisibility(View.GONE);
			explorePath("venues/trending", null);
			btn.setBackgroundColor(Color.BLACK);
			((Button)findViewById(R.id.fse_reco)).setBackgroundResource(R.drawable.btnbg);
			break;
			
		default:
			break;
		}
	}
	
	private void suggestButtonClick(ImageButton btn)
	{
		isExplore = true;
		String p = (String)btn.getTag();
		Util.log(p);
		explorePath("venues/explore", p);
	}
	
	
	/*
	 * 
	 * Infomation Methods
	 * 
	 */
	
	
	private void explorePath(final String path, final String section)
	{
		
		if(mGetThread != null)
			if(mGetThread.isAlive()) return;
		
		mPath = new String[] {path, section};
		
		mProgress.setVisibility(View.VISIBLE);
		mListView.setVisibility(View.INVISIBLE);
		
		if(getLastLocation() == null)
		{
			Toast.makeText(mContext, mContext.getString(R.string.no_location), Toast.LENGTH_LONG).show();
			mProgress.setVisibility(View.INVISIBLE);
			return;
		}
		
		mGetThread = new Thread( new Runnable() {
			
			public void run() {
				
				Bundle params = new Bundle();
				params.putString("ll", URLEncoder.encode(Foursquare.getLL(getLastLocation())));
				if(section != null)
				{
					params.putString("section", section);
					if(section.compareTo("specials") == 0)
					{
						params.putString("intent", "specials");
					}
				}
				
				params.putString("radius", String.valueOf(INT_DIST[selectedDistIndex]));
				params.putString("limit", "25");
				//Util.log(Foursquare.getFullUrl(path, params));
				final String results = ACacheUtil.getUrl(Foursquare.getFullUrl(path, params), false);
				if(results == null)
				{
					mHandler.post(new Runnable() {
						
						public void run() {
							Toast.makeText(getParent(), "Error: ", 3000);
							mProgress.setVisibility(View.INVISIBLE);
						}
					});
					return;
				}
					
				
					
					
					mHandler.post(new Runnable() {
						
						public void run() {
							try {
								if(isExplore)
									mVenueList = Foursquare.getItemList(new JSONObject(results));
								else
									mVenueList = Foursquare.getVenueList(new JSONObject(results));
							} catch (JSONException e) {}
							mAdapter.notifyDataSetChanged();
							if(mListView.getChildCount() > 0)
							{
								mListView.setSelection(0);
							}
							mProgress.setVisibility(View.INVISIBLE);
							mListView.setVisibility(View.VISIBLE);
						}
					});
			}
			
		});
		
		mGetThread.start();
		
	}
	
	
	private class VenueAdapter extends BaseAdapter
	{
		public int getCount() {
			if(mVenueList == null) return 0;
			return mVenueList.length();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int pos, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			if(view == null){
				view = mInflater.inflate(R.layout.foursquare_cell_location, null);
				Holders.LocationCellHolder holder = new Holders.LocationCellHolder();
				holder.categoryIcon = (ImageView)view.findViewById(R.id.fs_loc_icon);
				holder.txtTitle = (TextView)view.findViewById(R.id.fs_loc_name);
				holder.txtAddress = (TextView)view.findViewById(R.id.fs_loc_address);
				holder.txtDistance = (TextView)view.findViewById(R.id.fs_loc_dist);
				holder.tipLayout = (LinearLayout)view.findViewById(R.id.fs_loc_comment);
				holder.txtTip = (TextView)view.findViewById(R.id.fs_loc_comment_text);
				holder.txtTipDate = (TextView)view.findViewById(R.id.fs_loc_comment_added);
				holder.txtMessage = (TextView)view.findViewById(R.id.fs_loc_subtext);
				holder.prgBar = (ProgressBar)view.findViewById(R.id.fs_loc_prog);
				view.setTag(holder);
			}
			
			
			Holders.LocationCellHolder holder = (Holders.LocationCellHolder)view.getTag();
			
			
			holder.txtMessage.setVisibility(View.GONE);
			holder.tipLayout.setVisibility(View.GONE);
			JSONObject place = null;
			holder.id = null;
			
			try {
				if(isExplore)
				{
					place = mVenueList.getJSONObject(pos).getJSONObject("venue");
					JSONObject venue = mVenueList.getJSONObject(pos);
					if(venue.has("reasons"))
					{
						if(venue.getJSONObject("reasons").getInt("count") > 0)
						{
							JSONArray reasons = venue.getJSONObject("reasons").getJSONArray("items");
							holder.txtMessage.setVisibility(View.VISIBLE);
							holder.txtMessage.setText(reasons.getJSONObject(0).getString("message"));
						}
					}
					if(venue.has("tips"))
					{
						JSONArray jsonTips = venue.getJSONArray("tips");
						if(jsonTips.length() > 0)
						{
							holder.tipLayout.setVisibility(View.VISIBLE);
							JSONObject tipObj = jsonTips.getJSONObject(0);
							if(tipObj.has("text"))
								holder.txtTip.setText(tipObj.getString("text"));
							else
								holder.txtTip.setText("");
							
							//Date date = new Date(tipObj.getLong("createdAt")*1000);
							
							holder.txtTipDate.setText("added " + Util.formatFSTime(tipObj.getLong("createdAt")*1000) + " by "
									+ tipObj.getJSONObject("user").getString("firstName"));
							
						}
					}
				}
				else
				{
					place = mVenueList.getJSONObject(pos);
					
				}
				
				
				if(place != null)
				{
					holder.id = place.getString("id");
					
					if(place.has("name")) holder.txtTitle.setText(place.getString("name"));
						
					getBitmap(Foursquare.getIconUrl(place.getJSONArray("categories")), holder.categoryIcon, holder.prgBar);
					
					if(place.has("location"))
					{
						JSONObject location = place.getJSONObject("location");
						
						if(location.has("address"))	
						{
							if(location.has("crossStreet"))
								holder.txtAddress.setText( location.getString("address") + " (" + location.getString("crossStreet") + ")");
							else
								holder.txtAddress.setText(location.getString("address"));
						}
						else
							holder.txtAddress.setText( "" );
						
						double dist = location.getDouble("distance");
						dist /= mm;
						holder.txtDistance.setText( String.format("%.1f mi", dist) );
					}
					
					if(place.has("hereNow"))
					{
						JSONObject hereNow = place.getJSONObject("hereNow");
						int count = hereNow.getInt("count");
						if(count > 0)
						{
							String strCount = String.valueOf(count) + ( count == 1 ? " person" : " people" );
							holder.txtDistance.setText(holder.txtDistance.getText() + " " + strCount);
						}
					}
				}
				
				
				
			}catch(Exception ex){}
			
			return view;
		}
		
	}
	
	private void getBitmap(final String url, final ImageView view)
	{
		getBitmap(url, view, null);
	}
	
	private void getBitmap(final String url, final ImageView view, final ProgressBar bar)
	{
		if(bar != null)
			bar.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			public void run() {
				
				final Bitmap bitmap = ACacheUtil.getBitmap(url);
				mHandler.post(new Runnable() {
					public void run() {
						view.setImageBitmap(bitmap);
						if(bar != null) 
							bar.setVisibility(View.GONE);
					}
				});
			}
		}).start();
		
	}

	public void onItemClick(AdapterView<?> listView, View view, int pos, long arg3) {
		
		Holders.LocationCellHolder holder = (Holders.LocationCellHolder)view.getTag();
		
		String id = holder.id;
		Util.log(id);
		
		Intent intent = new Intent(FoursquareExplore.this, FoursquareVenue.class);
		intent.putExtra("com.psm.android.id", id);
		try {
			JSONObject obj = mVenueList.getJSONObject(pos);
			Util.log(obj.toString());
			intent.putExtra("com.psm.android.json", obj.toString());
		}catch(Exception ex) {}
		
		startActivity(intent);
	}
	
	
}
