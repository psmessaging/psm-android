package com.psm.android.fb;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.GroupChooser;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.IUtil;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class FacebookCheckin extends Activity {

	private final static int PLACE_SEARCH 	= 4;
	private final static int STEALTH_SEARCH	= 5;
	private final static int GROUP_CHOICE	= 6;
	private JSONObject mJsonCheckin, mJsonPrivate;
	
	private ImageView 	mCheckinIcon, mStealthIcon;
	private TextView 	mCheckinName, mStealthName;
	private TextView	mCheckinOther, mStealthOther, mStealthGroup;
	
	private Button mCheckinChoose, mStealthChoose, mPost, mGroupButton;
	private LinearLayout mStealthLayout, mStealthAsk;
	
	private ToggleButton mToggle;
	private Handler mHandle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_checkin);
		mHandle = new Handler();
		mCheckinIcon = (ImageView)findViewById(R.id.fb_checkin_icon);
		mCheckinName = (TextView)findViewById(R.id.fb_checkin_name);
		mCheckinOther = (TextView)findViewById(R.id.fb_checkin_other);
		mCheckinChoose = (Button)findViewById(R.id.fb_checkin_btnchoose);
		mStealthGroup = (TextView)findViewById(R.id.fb_checkin_txtgroupname);
		mGroupButton = (Button)findViewById(R.id.fb_checkin_btngroup);
		mStealthLayout = (LinearLayout)findViewById(R.id.fb_checkin_stealthlayout);
		mStealthAsk = (LinearLayout)findViewById(R.id.fb_checkin_stealthask);
		
		mStealthIcon = (ImageView)findViewById(R.id.fb_checkin_sicon);
		mStealthName = (TextView)findViewById(R.id.fb_checkin_sname);
		mStealthOther = (TextView)findViewById(R.id.fb_checkin_sother);
		mStealthChoose = (Button)findViewById(R.id.fb_checkin_btnschoose);
		
		mPost = (Button)findViewById(R.id.fb_checkin_btnpost);
		mToggle = (ToggleButton)findViewById(R.id.fb_checkin_toggle);
		
		if(!Util.mInsider.isLoggedIn())
			mStealthAsk.setVisibility(View.GONE);
		
		mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
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
		});
		
		mPost.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				postCheckin();
			}
		});
		
		mCheckinChoose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showSearch(false);
			}
		});
		
		mStealthChoose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showSearch(true);
			}
		});
		
		mGroupButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				Intent activity = new Intent(FacebookCheckin.this, GroupChooser.class);
				startActivityForResult(activity, GROUP_CHOICE);
			}
		});
		
		showSearch(false);
	}
	
	private void showSearch(boolean stealth)
	{
		Intent intent = new Intent(FacebookCheckin.this, FacebookSearch.class);
		if(!stealth)
			startActivityForResult(intent, PLACE_SEARCH);
		else
			startActivityForResult(intent, STEALTH_SEARCH);
		
	}
	
	private void postCheckin()
	{
		final boolean doStealth = mToggle.isChecked();
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage("Posting Checkin...");
		dialog.show();
		new Thread( new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				try {
					
					params.putString("place", mJsonCheckin.getString("id"));
					//TODO: possible enable a message
					//params.putString("message", "test message");
					params.putString("access_token", AFacebook.getAccessToken());
					
					JSONObject coordinates = new JSONObject();
					coordinates.put("latitude", mJsonCheckin.getJSONObject("location").getString("latitude"));
					coordinates.put("longitude", mJsonCheckin.getJSONObject("location").getString("longitude"));
					params.putString("coordinates", coordinates.toString());
					
					String results = AHttpUtil.openUrl("https://graph.facebook.com/me/checkins", "POST", params);
					Util.log("Results: " + results);
					String pid = new JSONObject(results).getString("id");
					if(doStealth)
					{
						mHandle.post( new Runnable() {
							public void run() {
								dialog.setMessage("Posting to insid3r...");
							}
						});
						postStealthCheckin(pid);
					}
					mHandle.post(new Runnable() {
						
						public void run() {
							dialog.dismiss();
							finish();
							Intent intent = new Intent("com.psm.android.REFRESH");
							sendBroadcast(intent);
						}
					});
					
				}catch(Exception ex) {Util.log("Error: " + ex.getMessage());}
			}
		}).start();
		
		
	}
	
	private void postStealthCheckin(String id)
	{
		try {
			
			Bundle bundle = new Bundle();
			bundle.putString("iToken", Util.mInsider.getAccessToken());
			bundle.putString("id", IUtil.normalizeFacebookId(id));
			bundle.putString("publicVenueId", mJsonCheckin.getString("id"));
			bundle.putString("privateVenueId", mJsonPrivate.getString("id"));
			if(mStealthGroup.getTag() != null)
				bundle.putString("groupId", (String)mStealthGroup.getTag());

			//TODO: maybe message for stealth checkin
			//bundle.putString("message", "test message");
			
			
			String fullUrl = Insider.getUrlPath("msg/facebook/checkin");
			Util.log(fullUrl);
			String results = AHttpUtil.simplePost(fullUrl, bundle);
			Util.log(results);
			

		}catch(Exception ex) {}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == PLACE_SEARCH)
				if(resultCode == RESULT_OK)
				{
					if(data != null)
					{
						try {
							mJsonCheckin = new JSONObject( data.getExtras().getString("com.psm.android.venue") );
							refreshPlaces();
						}catch(Exception ex) {}
					}
					//Util.log("RETURNED: " + data.getExtras().getString("com.psm.android.venue"));
				}
				else
				{
					if(mJsonCheckin == null) finish();
				}
	
		if(requestCode == STEALTH_SEARCH && resultCode == RESULT_OK)
		{
			if(data != null)
			{
				try {
					mJsonPrivate = new JSONObject( data.getExtras().getString("com.psm.android.venue") );
					refreshPlaces();
				}catch(Exception ex) {}
			}
			//Util.log("RETURNED: " + data.getExtras().getString("com.psm.android.venue"));
		}
		
		if(requestCode == GROUP_CHOICE && resultCode == RESULT_OK)
		{
			String gname = data.getExtras().getString("com.psm.android.groupname");
			String groupId = data.getExtras().getString("com.psm.android.groupid");
			int groupCount = data.getExtras().getInt("com.psm.android.groupcount");
			mStealthGroup.setText(gname);
			mStealthGroup.setTag(groupId);
			//mTxtStealthGroup.setText(gname);
			//mTxtStealthGroup.setTag(groupId);
			//stealthGroupId = groupId;
		}
	}
	
	private void refreshPlaces()
	{
		if(mJsonCheckin != null)
		{
			try {
				
				String bmpUrl = "https://graph.facebook.com/" + mJsonCheckin.getString("id") + "/picture";
				Bitmap bitmap = ACacheUtil.getBitmap(bmpUrl);
				mCheckinIcon.setImageBitmap(bitmap);
				mCheckinName.setText(mJsonCheckin.getString("name"));
				JSONObject location = mJsonCheckin.getJSONObject("location");
				mCheckinOther.setText(location.getString("city") + ", " + location.getString("state"));
			}catch(Exception ex) {}
		}
		else
		{
			mCheckinIcon.setImageResource(R.drawable.icon);
			mCheckinName.setText("");
			mCheckinOther.setText("");
		}
		
		if(mJsonPrivate != null)
		{
			try {
				
				String bmpUrl = "https://graph.facebook.com/" + mJsonPrivate.getString("id") + "/picture";
				Bitmap bitmap = ACacheUtil.getBitmap(bmpUrl);
				mStealthIcon.setImageBitmap(bitmap);
				mStealthName.setText(mJsonPrivate.getString("name"));
				JSONObject location = mJsonPrivate.getJSONObject("location");
				mStealthOther.setText(location.getString("city") + ", " + location.getString("state"));
			}catch(Exception ex) {}
		}
		else
		{
			mStealthIcon.setImageResource(R.drawable.icon);
			mStealthName.setText("");
			mStealthOther.setText("");
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(mToggle.isChecked())
			menu.add("switch checkin locations");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switchPlaces();
		refreshPlaces();
		return super.onOptionsItemSelected(item);
	}
	
	private void switchPlaces()
	{
		if(mJsonPrivate == null || mJsonCheckin == null)
		{
			new AlertDialog.Builder(this).setMessage("Cannot swap locations, you have chosen only one")
				.setPositiveButton("Ok", null).create().show();
			return;
		}
			
		
		JSONObject obj = mJsonPrivate;
		mJsonPrivate = mJsonCheckin;
		mJsonCheckin = obj;
	}
	
}
