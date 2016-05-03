package com.psm.android.fs;

import java.sql.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FoursquareMe extends Activity {

	private JSONObject mJsonMe, mJsonPoints, mJsonStats;
	private ProgressBar mProgress;
	private Handler mHandler;
	private Context mContext;
	private TextView mTextName, mTextLast, mTextDate, mTextBadge, mTextCheckins, mTextMayors, mTxtGoalMax;
	private TextView mTextPoints, mTextGoal;
	private ImageView mImageIcon;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.foursquare_me);
		mHandler = new Handler();
		mContext = this;
		
		mProgress = (ProgressBar)findViewById(R.id.fsm_progress);
		mTextName = (TextView)findViewById(R.id.fsm_name);
		mTextLast = (TextView)findViewById(R.id.fsm_lastseen);
		mTextDate = (TextView)findViewById(R.id.fsm_lastdate);
		mTextBadge = (TextView)findViewById(R.id.fsm_txt_badges);
		mTextCheckins = (TextView)findViewById(R.id.fsm_txt_checkins);
		mTextMayors = (TextView)findViewById(R.id.fsm_txt_mayorships);
		mTextPoints = (TextView)findViewById(R.id.fsm_txt_points);
		mTextGoal = (TextView)findViewById(R.id.fsm_txt_goal);
		mImageIcon = (ImageView)findViewById(R.id.fsm_icon);
		mTxtGoalMax = (TextView)findViewById(R.id.fs_me_textgoal);
		
		mProgress.setMax(50 + 3);
		mProgress.setProgress(0 + 3);
		
		getMe();
	}
	
	private void getMe()
	{
		new Thread(new Runnable() {
			
			public void run() {
				
				Bundle params = new Bundle();
				Util.log(Foursquare.getFullUrl("users/self"));
				String results = ACacheUtil.getUrl(Foursquare.getFullUrl("users/self"), true);
				try {
					mJsonMe = new JSONObject(results).getJSONObject("response").getJSONObject("user");
					mHandler.post(new Runnable() {
						public void run() {
							populate();
						}
					});
				}catch(Exception ex) {}
				
			}
		}).start();
	}
	
	private void populate()
	{
		if(mJsonMe == null) return;
		
		//mTextName, mTextLast, mTextDate, mTextBadge, mTextCheckins, mTextMayors
		try {
			//Self
			mTextName.setText(mJsonMe.getString("firstName") + " " + mJsonMe.getString("lastName"));
			
			int cnt = mJsonMe.getJSONObject("checkins").getInt("count");
			if(cnt > 0)
			{
				JSONObject lstVenue = mJsonMe.getJSONObject("checkins")
							.getJSONArray("items").getJSONObject(0).getJSONObject("venue");
				mTextLast.setText("last seen: " + lstVenue.getString("name"));
				long theDate = mJsonMe.getJSONObject("checkins").getJSONArray("items").getJSONObject(0).getLong("createdAt");
				Date mdate = new Date(theDate*1000);
				mTextDate.setText(mdate.toLocaleString());
				
			}
			
			//Scores
			JSONObject scores = mJsonMe.getJSONObject("scores");
			int recent = scores.getInt("recent");
			int goal = 0;
			if(scores.has("goal"))
			{
				goal = scores.getInt("goal");
				mTxtGoalMax.setText("GOAL");
			}
			else
			{
				goal = scores.getInt("max");
				mTxtGoalMax.setText("MAX");
			}
			mProgress.setProgress(recent + 3);
			mProgress.setMax(goal + 3);
			mTextPoints.setText(String.valueOf(recent));
			mTextGoal.setText(String.valueOf(goal));
			
			//Stats
			mTextBadge.setText(mJsonMe.getJSONObject("badges").getString("count"));
			mTextCheckins.setText(mJsonMe.getJSONObject("checkins").getString("count"));
			mTextMayors.setText(mJsonMe.getJSONObject("mayorships").getString("count"));
			populateBitmap(mJsonMe.getString("photo"));
			
		} catch (JSONException e) {
		}
		
	}
	
	private void populateBitmap(String url)
	{
		Bitmap bitmap = ACacheUtil.getBitmap(url);
		mImageIcon.setImageBitmap(bitmap);
	}
	
}
