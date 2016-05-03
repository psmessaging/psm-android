package com.psm.android.fs;

import com.psm.android.R;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ViewAnimator;

public class FoursquareMan extends ActivityGroup {

	
	private static int[] Man_Buttons = { R.id.fs_man_friends, R.id.fs_man_explore, R.id.fs_man_me, R.id.fs_man_checkin };
	private int selectedItem = 1;
	
	private ViewAnimator mAnimator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_man);
		mAnimator = (ViewAnimator)findViewById(R.id.fs_man_anim);
		setupNavButtons();
		showClass(FoursquareExplore.class, "explore");
		setSelected();
	}
	
	private void showClass(Class<?> cls, String tag)
	{
		Activity activity = getLocalActivityManager().getActivity(tag);
		Intent intent = null;
		if(activity == null)
		{
			intent = new Intent(FoursquareMan.this, cls);
			Window window = getLocalActivityManager().startActivity(tag, intent);
			mAnimator.removeAllViews();
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
	
	private void setupNavButtons()
	{
		int x = 0;
		for(int id : Man_Buttons)
		{
			Button btn = (Button)findViewById(id);
			btn.setId(x);
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					clickNavButton((Button)v);
				}
			});
			x++;
		}
	}
	
	private void setSelected()
	{
		for(int i=0 ; i < 3;i++)
		{
			
			Button btn = (Button)findViewById(i);
			
			if(selectedItem == i)
			{
				btn.setBackgroundResource(R.drawable.btnbg);
				btn.setTextColor(Color.WHITE);
			}
			else
			{
				btn.setBackgroundResource(R.drawable.bg_gray_rnd);
				btn.setTextColor(Color.BLACK);
			}
		}
	}
	
	private void clickNavButton(Button btn)
	{
		switch (btn.getId()) {
		case 0: //friends
			showClass(FoursquareFriends.class, "friends");
			selectedItem = 0;
			break;
		case 1: //explore
			showClass(FoursquareExplore.class, "explore");
			selectedItem = 1;
			break;
		case 2: //me
			showClass(FoursquareMe.class, "me");
			selectedItem = 2;
			break;
		case 3: //checkin
			Intent intent = new Intent(FoursquareMan.this, FoursquareSearch.class);
			intent.putExtra("com.psm.android.checkin", true);
			startActivity(intent);
			break;
		default:
			break;
		}
		setSelected();
	}
	
}
