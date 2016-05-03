package com.psm.android;

import java.util.Arrays;
import java.util.LinkedList;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

public class TestPullDown extends Activity implements OnScrollListener, OnTouchListener{

	private ListView mList;
	private SimpleAdapter mAdapter;
	private LinkedList<String> mListItems;
	
	private boolean isScrolling = false;
	private boolean isScrollRefresh = false;
	private RelativeLayout mPullLayout;
	float MAX_PULL = 1.0f;
	float mFirstPoint;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.test_pulldown);
		
		mPullLayout = (RelativeLayout)findViewById(R.id.pull_down_layout);
		
		String[] string = {"one","two","three","four","five","six","seven"
				,"eight","nine","ten","eleven","twelve"};
		mList = (ListView)findViewById(R.id.test_pulldown_list);
		
		mListItems = new LinkedList<String>();
        mListItems.addAll(Arrays.asList(string));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mListItems);
        mList.setAdapter(adapter);
        mList.setOnScrollListener(this);
        mList.setOnTouchListener(this);
        setupRefreshFrame();
	}
	
	private void setupRefreshFrame()
	{
		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, -1.0f);
		animation.setDuration(1000);
		//animation.setFillBefore(true);
		animation.setFillAfter(true);
		animation.setFillEnabled(true);
		mPullLayout.setAnimation(animation);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int y = (int)event.getY();
		Util.log(y);
		//return false;
		
		return super.onTouchEvent(event);
	}
	
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if(firstVisibleItem == 0)
			isScrollRefresh = true;
		else
			isScrollRefresh = false;
		
		Util.log(String.valueOf(firstVisibleItem) + " / " + String.valueOf(visibleItemCount) + " / " + String.valueOf(totalItemCount));
	}
	
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState == SCROLL_STATE_TOUCH_SCROLL)
		{
			Util.log("Start Scroll");
			isScrolling = true;
		}
		else
		{
			Util.log("End Scroll");
			isScrolling = false;
		}
	}
	
	/*
	public interface OnRefreshListener
	{
		public void onRefresh();
	}*/

	public boolean onTouch(View v, MotionEvent event) {
		float y = event.getY();
		int tcount = event.getHistorySize()-1;
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
				float mDif = (y - mFirstPoint)/((float)getWindow().getDecorView().getHeight());
				if(mDif > 1.0f)
					mDif = 1.0f;
				if(!isScrollRefresh)
					return false;
				
				Util.log(mDif);
				Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.f, 
						Animation.RELATIVE_TO_SELF, 0.f, 
						Animation.RELATIVE_TO_SELF, 0.f, 
						Animation.RELATIVE_TO_SELF, mDif);
				animation.setFillBefore(true);
				animation.setFillAfter(true);
				animation.setFillEnabled(true);
				mPullLayout.setAnimation(animation);
				mList.setAnimation(animation);
			Util.log("Action Move y: " + String.valueOf(y));
			break;
		case MotionEvent.ACTION_DOWN:
			mFirstPoint = event.getY();
			break;
		case MotionEvent.ACTION_UP:
			//Animation animation1 = new TranslateAnimation(0.f, 0.f, 0.f, -1.0f);
			//mPullLayout.setAnimation(animation1);
			break;
		default:
			break;
		}
		return false;
	}
	
	
}
