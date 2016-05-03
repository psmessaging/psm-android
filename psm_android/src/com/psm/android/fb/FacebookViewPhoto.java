package com.psm.android.fb;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FacebookViewPhoto extends Activity {

	private ImageView mImage;
	private Context mContext;
	private Handler mHandler;
	
	private String mImageId;
	private JSONObject mJsonPhoto;
	
	private TextView mTxtTitle;
	private TextView mTxtLikes, mTxtComments;
	private LinearLayout mLayout;
	private Button mbtnComment, mbtnLike;
	
	private boolean isShowingExtras = true;
	
	private LinearLayout mProgress;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_photo_view);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null)
		{
			finish();
			return;
		}
		mHandler = new Handler();
		
		mProgress = (LinearLayout)findViewById(R.id.fb_photo_view_progress);
		mImageId = extras.getString("com.psm.android.id");
		mTxtTitle = (TextView)findViewById(R.id.fb_photo_view_title);
		mTxtComments = (TextView)findViewById(R.id.fb_photo_view_comments);
		mTxtLikes = (TextView)findViewById(R.id.fb_photo_view_likes);
		mLayout = (LinearLayout)findViewById(R.id.fb_photo_layoutbottom);
		mbtnComment = (Button)findViewById(R.id.fb_photo_view_btncomment);
		mbtnLike = (Button)findViewById(R.id.fb_photo_view_btnlike);
		mbtnComment.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickLikeComment();
			}
		});
		mbtnLike.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickLikeComment();
			}
		});
		
		try {
			
			if(extras.containsKey("com.psm.android.json"));
				mJsonPhoto = new JSONObject(extras.getString("com.psm.android.json"));
				
		}catch(Exception ex) {}
		
		mImage = (ImageView)findViewById(R.id.fb_photo_view_photo);
		mImage.setImageBitmap(null);
		mImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(isShowingExtras)
					hideHeaderFooter();
				else
					showHeaderFooter();
			}
		});
		
		loadImage();
		getLCCounts();
		
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				mHandler.post( new Runnable() {
					
					public void run() {
						if(isShowingExtras)
							hideHeaderFooter();
					}
				});
				
			}
		};
		
		Timer mtimer = new Timer();
		mtimer.schedule(task, 1000);
		
		
		
		
	}
	
	private void getLCCounts()
	{
		new Thread(new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				params.putString("fields", "likes,comments");
				params.putString("access_token", Util.mFacebook.getAccessToken());
				try {
					String response = AHttpUtil.openUrl("https://graph.facebook.com/" + mImageId, params);
					JSONObject obj = new JSONObject(response);
					final int likes = obj.getJSONObject("likes").getJSONArray("data").length();
					final int comments = obj.getJSONObject("comments").getJSONArray("data").length();
					mHandler.post(new Runnable() {
						public void run() {
							mTxtComments.setText(String.valueOf(comments));
							mTxtLikes.setText(String.valueOf(likes));
						}
					});
				}catch(Exception ex) {}
				
			}
		}).start();
	}
	
	private void clickLikeComment()
	{
		Intent intent = new Intent(FacebookViewPhoto.this, FacebookComments.class);
		intent.putExtra("com.psm.android.id", mImageId);
		intent.putExtra("com.psm.android.hideheader", true);
		startActivity(intent);
		
	}
	
	private void loadImage()
	{
		
			
			mProgress.setVisibility(View.VISIBLE);
			new Thread(new Runnable() {
				
				public void run() {
					// TODO Auto-generated method stub
					try {
						String url = mJsonPhoto.getString("source");
						final Bitmap bitmap = ACacheUtil.getBitmap(url);
						mHandler.post(new Runnable() {
							public void run() {
								// TODO Auto-generated method stub
								mImage.setImageBitmap(bitmap);
								mProgress.setVisibility(View.GONE);
							}
						});
					}catch(Exception ex) {}
					
				}
			}).start();
			
			
			try {
				if(mJsonPhoto.has("name"))
					mTxtTitle.setText(mJsonPhoto.getString("name"));
				else
					mTxtTitle.setVisibility(View.GONE);
				
			}catch(Exception ex) {}
			
			
	}
	
	private void showHeaderFooter()
	{
		isShowingExtras = true;
		
		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setFillAfter(true);
		animation.setFillBefore(true);
		animation.setFillEnabled(true);
		animation.setDuration(300);
		mTxtTitle.startAnimation(animation);
		
		Animation animation2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation2.setFillAfter(true);
		animation2.setFillBefore(true);
		animation2.setFillEnabled(true);
		animation2.setDuration(300);
		mLayout.startAnimation(animation2);
	}
	
	private void hideHeaderFooter()
	{
		isShowingExtras = false;
		
		Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, -1.0f);
		animation.setFillAfter(true);
		animation.setFillBefore(true);
		animation.setFillEnabled(true);
		animation.setDuration(300);
		mTxtTitle.startAnimation(animation);
		
		Animation animation2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, 
				Animation.RELATIVE_TO_SELF, 1.0f);
		animation2.setFillAfter(true);
		animation2.setFillBefore(true);
		animation2.setFillEnabled(true);
		animation2.setDuration(300);
		mLayout.startAnimation(animation2);
	}
}
