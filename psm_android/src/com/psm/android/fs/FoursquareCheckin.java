package com.psm.android.fs;

import org.json.JSONObject;

import com.psm.android.AHttpUtil;
import com.psm.android.GroupChooser;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class FoursquareCheckin extends Activity{

	
	private JSONObject mJsonvenue, mStealthVenue;
	private Handler mHandler;
	
	private TextView mTxtName, mTxtAddress,mTxtCityState;
	private EditText mShoutText;
	
	private Button mBtnCheckin, mBtnSearch, mBtnGroup;
	
	private String mVenueid = null;
	private Context mContext;
	
	
	private ToggleButton mToggle, mToggleStealth;
	
	private TextView mSTxtName, mSTxtAddress, mSTxtCityState;	
	private TextView mSTxtChoose;
	private boolean isPrivate = true;
	private boolean isStealth = false;
	
	private LinearLayout mStealthLayout, mStealthAsk;
	
	private static final int SEARCH_REQUEST_CODE 	= 5;
	private static final int GROUP_REQUEST_CODE 	= 6;
	
	private String stealthGroupId, stealthGroupName, stealthGroupCount;
	private TextView mTxtStealthGroup;
	private CheckBox mChkTwitter, mChkFacebook;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_checkin);
		mContext = this;
		mHandler = new Handler();
		
		mTxtName = (TextView)findViewById(R.id.fsc_name_title);
		mTxtAddress = (TextView)findViewById(R.id.fsc_address);
		mTxtCityState = (TextView)findViewById(R.id.fsc_citystate);
		mSTxtChoose = (TextView)findViewById(R.id.fsc_stealth_choosetext);
		mTxtStealthGroup = (TextView)findViewById(R.id.fsc_stealth_groupname);
		mBtnCheckin = (Button)findViewById(R.id.fsc_checkin);
		mBtnGroup = (Button)findViewById(R.id.fsc_checkin_btngroup);
		
		mChkTwitter = (CheckBox)findViewById(R.id.fsc_stealth_chktwit);
		mChkFacebook = (CheckBox)findViewById(R.id.fsc_stealth_chkface);
		
		mToggle = (ToggleButton)findViewById(R.id.fsc_share);
		mStealthLayout = (LinearLayout)findViewById(R.id.fsc_stealth_layout);
		mStealthAsk = (LinearLayout)findViewById(R.id.fsc_stealth_ask);
		
		if(!Util.mInsider.isLoggedIn())
		{
			mStealthAsk.setVisibility(View.GONE);
		}
		
		mToggleStealth = (ToggleButton)findViewById(R.id.fsc_stealth);
		mToggle.setChecked(isPrivate);
		
		mSTxtName = (TextView)findViewById(R.id.fsc_stealth_title);
		mSTxtAddress = (TextView)findViewById(R.id.fsc_stealth_address);
		mSTxtCityState = (TextView)findViewById(R.id.fsc_stealth_citystate);
		
		mToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isPrivate = !isChecked;
			}
		});
		
		mToggleStealth.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isStealth = isChecked;
				showStealthCheckin(isStealth);
			}
		});
		
		mBtnSearch = (Button)findViewById(R.id.fsc_stealth_choose);
		mBtnSearch.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(FoursquareCheckin.this, FoursquareSearch.class);
				startActivityForResult(intent, SEARCH_REQUEST_CODE);
			}
		});
		
		mShoutText = (EditText)findViewById(R.id.fsc_caption);
		
		
		try {
			Bundle extras = getIntent().getExtras();
			if(extras != null)
			{
				if(extras.containsKey("com.psm.android.venue"))
					mJsonvenue = new JSONObject(getIntent().getExtras().getString("com.psm.android.venue"));
				else
					finish();
				
				if(extras.containsKey("com.psm.android.stealth"))
				{
					mStealthVenue = new JSONObject(getIntent().getExtras().getString("com.psm.android.stealth"));
					mStealthLayout.setVisibility(View.VISIBLE);
					fillStealth();
				}
			}
			
			
		}catch(Exception ex) {
			finish();
		}
		mBtnGroup.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				Intent activity = new Intent(FoursquareCheckin.this, GroupChooser.class);
				startActivityForResult(activity, GROUP_REQUEST_CODE);
			}
		});
		
		loadCheckin();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Util.log("save instance: " + mSTxtName.getText().toString());
		if(mJsonvenue != null)
			outState.putString("com.psm.android.venue", mJsonvenue.toString());
		
		if(mStealthVenue != null)
		{
			Util.log("save stealth");
			outState.putString("com.psm.android.stealth", mStealthVenue.toString());
		}
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		
		Bundle extras = savedInstanceState;
		try {
			if(extras != null)
			{
				if(extras.containsKey("com.psm.android.venue"))
					mJsonvenue = new JSONObject(extras.getString("com.psm.android.venue"));
				else
					finish();
				
				if(extras.containsKey("com.psm.android.stealth"))
				{
					Util.log("load stealth");
					mStealthVenue = new JSONObject(extras.getString("com.psm.android.stealth"));
					mStealthLayout.setVisibility(View.VISIBLE);
					fillStealth();
				}
			}
		}catch(Exception ex){}
		
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	
	@Override
	protected void onResume() {
		new Handler().postDelayed(new Runnable() {
			
			public void run() {
				((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(mShoutText.getWindowToken(), 0);
			}
		}, 100);
		super.onResume();
	}
	
	private void showStealthCheckin(boolean isVisible)
	{
		if(isVisible)
		{
			mStealthLayout.setVisibility(View.VISIBLE);
			Animation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setFillAfter(true);
			animation.setFillEnabled(true);
			animation.setDuration(800);
			mStealthLayout.startAnimation(animation);
			
		}
		else
		{
			mStealthLayout.setVisibility(View.INVISIBLE);
			Animation animation = new AlphaAnimation(1.0f, 0.0f);
			animation.setFillAfter(true);
			animation.setFillEnabled(true);
			animation.setDuration(800);
			mStealthLayout.startAnimation(animation);
		}
	}
	
	private void loadCheckin()
	{
		try {
			
			mVenueid = mJsonvenue.getString("id");
			JSONObject location = mJsonvenue.getJSONObject("location");
			mTxtName.setText(mJsonvenue.getString("name"));
			mTxtAddress.setText(location.getString("address") + (location.has("crossStreet") ? " (" + location.getString("crossStreet") + ")" : ""));
			mTxtCityState.setText(location.getString("city") + ", " + location.getString("state"));
			mBtnCheckin.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					doCheckin();
				}
			});
			
		}catch(Exception ex) {}
	}
	
	private void doCheckin()
	{
		final ProgressDialog dialog = new ProgressDialog(mContext);
			
			dialog.setMessage("Checking In");
			dialog.show();
			
			new Thread( new Runnable() {
				
				public void run() {
					Bundle params = new Bundle();
					params.putString("venueId", mVenueid);
					if(mShoutText.getText().length() > 0)
						params.putString("shout", mShoutText.getText().toString());
					String broadcast = "";
					if(!isPrivate)
						broadcast = "private";
					else
						broadcast = "public";
					
					if(mChkFacebook.isChecked())
						broadcast += ",facebook";
					if(mChkTwitter.isChecked())
						broadcast += ",twitter";
					
					
					params.putString("broadcast", broadcast);
					try {
						
						String results = AHttpUtil.simplePost(Foursquare.getFullUrl("checkins/add"), params);
						Util.log(results);
						if(mStealthVenue != null && mToggleStealth.isChecked())
						{
							mHandler.post(new Runnable() {
								
								public void run() {
									dialog.setMessage("Checkin Insid3r");
								}
							});
							
							String id = new JSONObject(results).getJSONObject("response").getJSONObject("checkin").getString("id");
							stealthCheckin(id);
						}
						mHandler.post(new Runnable() {
							
							public void run() {
								setResult(RESULT_OK);
								dialog.dismiss();
								finish();
							}
						});
						
						
					}catch(Exception ex) { 
						Util.log(ex.toString());
						mHandler.post( new Runnable() {
							
							public void run() {
								Toast.makeText(mContext, "Error occured, try again", 3000);
							}
						});
						 
					}
					
					dialog.dismiss();
					
				}
			}).start();
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SEARCH_REQUEST_CODE && resultCode == RESULT_OK)
		{
			try {
				mStealthVenue= new JSONObject(data.getExtras().getString("com.psm.android.venue"));
				mSTxtChoose.setVisibility(View.INVISIBLE);
				fillStealth();
			}catch(Exception ex) {}
			
		}
		else if(requestCode == GROUP_REQUEST_CODE && resultCode == RESULT_OK)
		{
			//TODO: implement group
			String gname = data.getExtras().getString("com.psm.android.groupname");
			String groupId = data.getExtras().getString("com.psm.android.groupid");
			int groupCount = data.getExtras().getInt("com.psm.android.groupcount");
			mTxtStealthGroup.setText(gname);
			mTxtStealthGroup.setTag(groupId);
			stealthGroupId = groupId;
			/* 
				data.putExtra("com.psm.android.groupid", "41241");
				data.putExtra("com.psm.android.groupname", "name");
				data.putExtra("com.psm.android.groupcount", 3);
			 */
		}
	}

	private void fillStealth()
	{
		try {
			Util.log("fill stealth");
			mSTxtName.setText(mStealthVenue.getString("name"));
			JSONObject location = mStealthVenue.getJSONObject("location");
			mSTxtAddress.setText(location.getString("address"));
			mSTxtCityState.setText(location.getString("city") + ", " + location.getString("state"));
		}catch(Exception ex) {}
	}
	
	private void stealthCheckin(String pubId)
	{
		String url = Insider.getUrlPath("msg/foursquare/checkin");
		Bundle params = new Bundle();
		params.putString("iToken",Util.mInsider.getAccessToken());
		params.putString("id", pubId);
		try {
			params.putString("publicVenueId", mJsonvenue.getString("id"));
			params.putString("privateVenueId",mStealthVenue.getString("id"));
			params.putString("message", "");
			if(stealthGroupId != null)
				params.putString("groupId", stealthGroupId);
			
			String response = AHttpUtil.simplePost(url, params);
			Util.log(response);
		}catch(Exception ex) {}
		
		//params.putString("publicVenueId", );
		//params.put
		
	}
	
	
}
