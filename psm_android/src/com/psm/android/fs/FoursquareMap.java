package com.psm.android.fs;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.psm.android.ACacheUtil;
import com.psm.android.AItemizedOverlay;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;

public class FoursquareMap extends MapActivity {

	private JSONObject mVenue = null;
	
	private TextView mTxtDescription;
	private MapView mMapview = null;
	
	private MapController mController = null;
	private TextView mTxtUsers, mTxtCheckins, mDescription;
	private TextView mTxtName, mTxtAddress, mTxtCity, mTxtPhone;
	
	private ImageView mImageIcon;
	private Bitmap mVenueicon;
	
	private Handler mHandler;
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_map);
		
		Bundle extras = getIntent().getExtras();
		mContext = this;
		
		if(extras == null) finish();
		try {
			mVenue = new JSONObject( extras.getString("com.psm.android.venue") );
		}catch(Exception ex) {}
		
		if(mVenue == null) finish();
		mMapview = (MapView)findViewById(R.id.fsv_map_map);
		mTxtCheckins = (TextView)findViewById(R.id.fsv_map_totalcheckins);
		mTxtUsers = (TextView)findViewById(R.id.fsv_map_totalpeople);
		mDescription = (TextView)findViewById(R.id.fsv_map_description);
		
		mTxtName = (TextView)findViewById(R.id.fsv_map_name);
		mTxtAddress = (TextView)findViewById(R.id.fsv_map_address);
		mTxtCity = (TextView)findViewById(R.id.fsv_map_city);
		mTxtPhone = (TextView)findViewById(R.id.fsv_map_phone);
		mImageIcon = (ImageView)findViewById(R.id.fsv_map_image);
		
		mController = mMapview.getController();
		mController.setZoom(17);
		mHandler = new Handler();
		MyLocationOverlay overlay = new MyLocationOverlay(this, mMapview);
		overlay.enableCompass();
		mMapview.getOverlays().add(overlay);
		
		
		loadVenueMap();
		//loadIcon();
		
		mMapview.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Util.log("Map Click");
			}
		});
	}
	
	private void loadIcon()
	{
		new Thread(new Runnable() {
			public void run() {
				try {
					
					final Bitmap bitmap = 
							ACacheUtil.getBitmap(Foursquare.getIconUrl(
														mVenue.getJSONArray("categories")));
					mHandler.post(new Runnable() {
						public void run() {
							mImageIcon.setImageBitmap(bitmap);
							
						}
					});
				}catch(Exception ex) {}
			}
		}).start();
	}
	
	private void loadVenueMap()
	{
		try {
			
			final JSONObject location = mVenue.getJSONObject("location");
			final JSONObject stats = mVenue.getJSONObject("stats");
			final JSONObject contact = mVenue.getJSONObject("contact");
			
			final Bitmap bitmap = 
					ACacheUtil.getBitmap(Foursquare.getIconUrl(
												mVenue.getJSONArray("categories")));
			
			mImageIcon.setImageBitmap(bitmap);
			
			String checkinCount = stats.getString("checkinsCount");
			String checkinUsers = stats.getString("usersCount"); 
			String description = mVenue.has("description") ? mVenue.getString("description") : "";
			mTxtUsers.setText(checkinUsers);
			mTxtCheckins.setText(checkinCount);
			mDescription.setText(description);
			
			mTxtName.setText(mVenue.getString("name"));
			mTxtAddress.setText(location.getString("address"));
			mTxtCity.setText(location.getString("city"));
			mTxtPhone.setText(contact.getString("formattedPhone"));


			GeoPoint geo = Foursquare.getGeoPoint(location.getDouble("lat"), location.getDouble("lng"));
			mController.setCenter(geo);
			
			AItemizedOverlay items = new AItemizedOverlay(mImageIcon.getDrawable(), mContext);
			OverlayItem overlayitem2 = new OverlayItem(geo, mVenue.getString("name"), 
												contact.getString("formattedPhone"));
			items.addOverlay(overlayitem2);
			mMapview.getOverlays().add(items);
			
			/*
			ItemizedOverlay<OverlayItem> items = new ItemizedOverlay<OverlayItem>(null) {
				@Override
				protected OverlayItem createItem(int i) {
					GeoPoint geo;
					try {
						geo = Foursquare.getGeoPoint(location.getDouble("lat"), location.getDouble("lng"));
						mController.setCenter(geo);
						OverlayItem item = new OverlayItem(geo, "", "");
						return item;
					} catch (JSONException e) {}
					return null;
				}

				@Override
				public int size() {
					return 1;
				}
			};*/
			
		}catch(Exception ex){}
	}

	
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	
}
