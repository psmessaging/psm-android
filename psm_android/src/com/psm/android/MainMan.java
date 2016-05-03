package com.psm.android;

import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlManager;
import com.psm.android.R;
import com.psm.android.fb.FacebookMan;
import com.psm.android.fs.FoursquareMan;
import com.psm.android.sms.SmsInbox;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LocalActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;

public class MainMan extends ActivityGroup implements OnItemClickListener {

	private String[] menuTitles = {"Facebook", "Foursquare", "SMS", "Scheduled", "Settings"};
	private int[] menuIcons 	= {R.drawable.facebook, R.drawable.fsicon, android.R.drawable.ic_dialog_email,
										R.drawable.stopwatch, R.drawable.gears };
	
	private LocalActivityManager mLM;
	private Context mContext;
	
	private Button mSButton, mPButton;
	private ImageButton mImageButton;
	private ViewAnimator mViewSideMenu, mViewMainView;
	private BroadcastReceiver mReceiver;
	
	private menuAdapter mMenuAdapter;
	private AlertDialog mAlertDialog;
	private LinearLayout mMainLin;
	private boolean isShowingMenu =false;
	private RelativeLayout mrelLayout;
	
	private LayoutInflater mInflater;
	private Window window1, window2;
	private FrameLayout mFrame;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main_man);
		mLM = getLocalActivityManager();
		mContext = this;
		mInflater = getLayoutInflater();
		
		mSButton = (Button)findViewById(R.id.main_man_btnsidemenu);
		mPButton = (Button)findViewById(R.id.main_man_btnpopup);
		mImageButton = (ImageButton)findViewById(R.id.main_man_ibtn);
		mViewSideMenu = (ViewAnimator)findViewById(R.id.main_man_menu);
		mViewMainView = (ViewAnimator)findViewById(R.id.main_man_animator);
		mMainLin = (LinearLayout)findViewById(R.id.main_main_lin);
		mrelLayout = (RelativeLayout)findViewById(R.id.main_man_main);
		mFrame = (FrameLayout)findViewById(R.id.main_main_frame);
		
		mSButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(MainMan.this, SideMenu.class);
				startActivity(intent);
				overridePendingTransition(R.anim.side_in_animation, R.anim.main_slide_out);
				/*if(!isShowingMenu)
				{
					Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 0.8f, 
							Animation.RELATIVE_TO_SELF, 0.f, 
							Animation.RELATIVE_TO_SELF, 0.f);
					animation.setFillAfter(true);
					animation.setFillBefore(true);
					animation.setFillEnabled(true);
					animation.setDuration(300);
					mMainLin.setAnimationCacheEnabled(true);
					mrelLayout.setAnimationCacheEnabled(true);
					mFrame.startAnimation(animation);
					isShowingMenu = true;
				}
				else
				{
					Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.8f, 
							Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 0.f, 
							Animation.RELATIVE_TO_SELF, 0.f);
					animation.setFillAfter(true);
					animation.setFillBefore(true);
					animation.setFillEnabled(true);
					animation.setDuration(300);
					mMainLin.setAnimationCacheEnabled(true);
					mrelLayout.setAnimationCacheEnabled(true);
					mFrame.startAnimation(animation);
					isShowingMenu = false;
				}*/
				//mMainLin.setAnimation(animation);
				//mrelLayout.setAnimation(animation);
			}
		});
		
		mImageButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showNavigation();
				//Intent intent = new Intent("com.psm.android.CHOOSE_SECTION");
				//sendBroadcast(intent);
			}
		});
		if(getIntent().getExtras() != null)
		{
			Bundle extras = getIntent().getExtras();
			if(extras != null)
			{
				if(extras.containsKey("com.psm.android.thread_id") && !extras.containsKey("com.psm.android.thread_done"))
				{
					getIntent().putExtra("com.psm.android.thread_done", true);
					startWindow(2);//start sms
					/*
					Integer sthread_id = extras.getInt("com.psm.android.thread_id"); 
					SmsDatabase db = new SmsDatabase(this);
			        db.open();
			        db.markAllRead(sthread_id);
			        db.close();
					Intent intent = new Intent(MainMan.this, SmsConversation.class);
					intent.putExtra("com.psm.android.thread_id", sthread_id);
					startActivity(intent);
					*/
					
					//window1 = getLocalActivityManager().startActivity("sms", intent);
					//mViewMainView.removeAllViews();
					//mViewMainView.addView(window1.getDecorView());
					return;
				}
					
			}
			
			boolean startsmsSchedule = getIntent().getExtras().getBoolean("com.psm.android.smslist");
			if(startsmsSchedule == true)
			{
				startWindow(3);
			}
			else
				startPrefferedScreen();
		}
		else
			startPrefferedScreen();
	}
	
	private void startPrefferedScreen()
	{
		SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
		int startup = prefs.getInt("startup", 0);		
		switch (startup) {
		case 0: 
			startWindow(0);//facebook
			break;
		case 1:
			if(Util.mFoursquare.getToken() == null)
				startWindow(0);//facebook
			else
				startWindow(1);//foursquare
			break;
		case 2:
			startWindow(2);//SMS
			break;
		case 3:
			startWindow(3);//Chat
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter("com.psm.android.CHOOSE_SECTION");
		registerReceiver(mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Intent intent1 = new Intent(MainMan.this, FacebookMan.class);
				Window window1 = getLocalActivityManager().startActivity("main", intent1);
				mViewMainView.addView(window1.getDecorView());
			}
		}, filter);
	}
	
	
	
	@Override
	protected void onPause() {
		unregisterReceiver(mReceiver);
		super.onPause();
	}
	
	
	
	private void showNavigation()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		GridView view = new GridView(this);
		view.setSelector(R.drawable.tran_selector);
		view.setOnItemClickListener(this);
		view.setNumColumns(3);
		mMenuAdapter = new menuAdapter();
		view.setAdapter(mMenuAdapter);
		builder.setView(view);
		mMenuAdapter.notifyDataSetChanged();
		mAlertDialog = builder.create();
		mAlertDialog.show();
	}
	
	private class menuAdapter extends BaseAdapter
	{

		public int getCount() {
			if(Util.mFoursquare.getToken() == null)
				return 4;
			else
				return 5;
			
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup arg2) {
			View view = convertView;
			if(view == null){
				view = mInflater.inflate(R.layout.menu_cell, null);
				menuHolder holder = new menuHolder();
				holder.imgView = (ImageView)view.findViewById(R.id.menu_cell_icon);
				holder.txtView = (TextView)view.findViewById(R.id.menu_cell_name);
				view.setTag(holder);
			}
			
			menuHolder holder = (menuHolder)view.getTag();
			if(position > 0 && Util.mFoursquare.getToken() == null) position++;
			holder.imgView.setImageResource(menuIcons[position]);
			holder.txtView.setText(menuTitles[position]);
			holder.id = position;
			return view;
		}
		
	}

	@Override
	public void onBackPressed() {
		Util.log("MainMan Back Pressed");
		//super.onBackPressed();
	}
	
	private static class menuHolder
	{
		int id;
		ImageView imgView;
		TextView txtView;
	}
	
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
		//fb,fs,sms,settings
		mAlertDialog.dismiss();
		menuHolder holder = (menuHolder)view.getTag();
		startWindow(holder.id);
	}
	
	private void startWindow(int id)
	{
		Intent intent1;
		Window window1;
		
		switch (id) {
		case 0:
			intent1 = new Intent(MainMan.this, FacebookMan.class);
			window1 = getLocalActivityManager().startActivity("fb", intent1);
			mViewMainView.removeAllViews();
			mViewMainView.addView(window1.getDecorView());
			break;
		case 1:
			intent1 = new Intent(MainMan.this, FoursquareMan.class);
			window1 = getLocalActivityManager().startActivity("fs", intent1);
			mViewMainView.removeAllViews();
			mViewMainView.addView(window1.getDecorView());
			break;
		case 2:
			intent1 = new Intent(MainMan.this, SmsInbox.class);
			window1 = getLocalActivityManager().startActivity("sms", intent1);
			mViewMainView.removeAllViews();
			mViewMainView.addView(window1.getDecorView());
			break;
		case 3:
			/*intent1 = new Intent(MainMan.this, ChatMan.class);
			window1 = getLocalActivityManager().startActivity("chat", intent1);
			mViewMainView.removeAllViews();
			mViewMainView.addView(window1.getDecorView());
			break;*/
		//case 4:
			intent1 = new Intent(MainMan.this, ScheduleList.class);
			window1 = getLocalActivityManager().startActivity("sched", intent1);
			mViewMainView.removeAllViews();
			mViewMainView.addView(window1.getDecorView());
			break;
		case 4:
			intent1 = new Intent(MainMan.this, SettingsActivity.class);
			window1 = getLocalActivityManager().startActivity("setting", intent1);
			mViewMainView.removeAllViews();
			mViewMainView.addView(window1.getDecorView());
			break;
		default:
			break;
		}
	}
	
}
