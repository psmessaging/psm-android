package com.psm.android.fb;

import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;

import android.app.Activity;
import android.app.ActivityGroup;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ViewAnimator;

public class FacebookMan extends ActivityGroup {

	
	private ViewAnimator mAnimator;
	
	private Button mBtnCheckin, mBtnPhoto, mBtnStatus, mBtnInbox, mBtnNews;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_man);
		mAnimator = (ViewAnimator)findViewById(R.id.fb_main_animator);
		
		mBtnCheckin = (Button)findViewById(R.id.fb_man_checkin);
		mBtnPhoto = (Button)findViewById(R.id.fb_man_photo);
		mBtnStatus = (Button)findViewById(R.id.fb_man_status);
		mBtnInbox = (Button)findViewById(R.id.fb_man_inbox);
		mBtnNews = (Button)findViewById(R.id.fb_man_news);
		
		Util.log("Facebook Man: " + Util.mInsider.getAccessToken());
		
		mBtnInbox.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				Intent intent = new Intent(FacebookMan.this, FacebookInbox.class);
				startActivity(intent);
			}
		});
		
		mBtnStatus.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(FacebookMan.this, FacebookStatus.class);
				startActivity(intent);
			}
		});
		
		mBtnCheckin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(FacebookMan.this, FacebookCheckin.class);
				startActivity(intent);
			}
		});
		
		mBtnNews.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent("com.psm.android.NAVIGATE");
				intent.putExtra("com.psm.android.navpath", "me/home");
				sendBroadcast(intent);
			}
		});
		
		showClass(FacebookNav.class, "nav");
	}
	
	
	private void showClass(Class<?> cls, String tag)
	{
		Activity activity = getLocalActivityManager().getActivity(tag);
		Intent intent = null;
		if(activity == null)
		{
			intent = new Intent(FacebookMan.this, cls);
			Window window = getLocalActivityManager().startActivity(tag, intent);
			mAnimator.addView(window.getDecorView());
		}
		else
		{
			intent = activity.getIntent();
			Window window = activity.getWindow();
			mAnimator.removeAllViews();
			mAnimator.addView(window.getDecorView());
		}
	}
	
	@Override
	public void onBackPressed() {
		Util.log("FacebookMan Back Pressed");
		//super.onBackPressed();
	}
}
