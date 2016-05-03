package com.psm.android;

import com.psm.android.R;
import com.psm.android.fb.FacebookMan;
import com.psm.android.sms.SmsInbox;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

public class MainA extends ActivityGroup {

	private ViewAnimator mAFrame;
	private LocalActivityManager mMan;
	private Handler mHandler;
	private ProgressBar mProgressBar;
	private static MainA mMain;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		mMain = this;
		mAFrame = (ViewAnimator)findViewById(R.id.main_viewanimator);
		mMan = getLocalActivityManager();
		mHandler = new Handler();
		mProgressBar = (ProgressBar)findViewById(R.id.main_progress);
		ShowProgress(true);
		//FacebookLogout();
		//SmsStart();
		
		Intent intent = new Intent(MainA.this, LoginActivity.class);
		startActivity(intent);
		finish();
		//FacebookStart();
		/*Intent intent = new Intent(MainA.this, LoginActivity.class);
		startActivity(intent);
		finish();*/
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		/*Intent intent = new Intent(MainA.this, LoginActivity.class);
		startActivity(intent);
		finish();*/
		
	}
	
	public static void FacebookStart()
	{
		if(mMain == null) return;
		Intent intent = null;
		
		if(mMain.mMan.getActivity("fb") == null)
			intent = new Intent(mMain, FacebookMan.class);
		else
			intent = mMain.mMan.getActivity("fb").getIntent();
		
		Window window = mMain.mMan.startActivity("fb", intent);
		mMain.mAFrame.addView(window.getDecorView());
	}
	
	public static void SmsStart()
	{
		if(mMain == null) return;
		Intent intent = null;
		
		if(mMain.mMan.getActivity("acctlist") == null)
			intent = new Intent(mMain, SmsInbox.class);
		else
			intent = mMain.mMan.getActivity("acctlist").getIntent();
		
		Window window = mMain.mMan.startActivity("acctlist", intent);
		mMain.mAFrame.addView(window.getDecorView());
	}
	
	public static void FacebookLogout()
	{
		
		if(mMain == null) return;
		Intent intent = null;
		
		if(mMain.mMan.getActivity("acctlist") == null)
			intent = new Intent(mMain, AcctList.class);
		else
			intent = mMain.mMan.getActivity("acctlist").getIntent();
		
		Window window = mMain.mMan.startActivity("acctlist", intent);
		mMain.mAFrame.addView(window.getDecorView());
		//BAAEAh4bHixcBADZARB4WIyyjrN978oUfK6YYIC1pK48w0FlfEfoiCTxm5MEzfY9QzoActHI12AUy8ZBn9ubHNZAu3ZBi1oZBkoOhunaVlNZCqZCqeh1UHkd2Sfl0cGkyOZAEawDg5CJDrDvxRD8wX9ZAj
		

	}
	
	public static void ShowProgress(Boolean Visible)
	{
		
		if(mMain == null) return;
		if(Visible)
			mMain.mProgressBar.setVisibility(View.VISIBLE);
		else
			mMain.mProgressBar.setVisibility(View.GONE);
	}
}
