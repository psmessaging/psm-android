package com.psm.android;

//Use this to login to facebook first

import org.json.JSONObject;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.psm.android.R;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class LoginActivity extends Activity {

	private Button mLoginButton;
	private Context mContext;
	private Handler mHandler;
	
	Class<?> startupClass = MainMan.class;//FoursquareMan.class;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.login);
		mContext = this;
		mHandler = new Handler();
		
		getCacheDir();
		mLoginButton = (Button)findViewById(R.id.login_facebook);
		mLoginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				AuthorizeFacebook();
			}
		});
		
		restoreSessions();
		Intent service = new Intent(this, ScheduleMsgService.class);
		startService(service);
		
		doRestoreInsider();
	}
	
	
	
	private void restoreSessions()
	{
		Util.mSession.initFacebook();
		Util.mSession.initInsider();
		Util.mSession.initFoursquare();
		//boolean isFacebookValid = false;
		//boolean isInsiderValid = false;
		
		SessionManager.restoreFacebook(Util.mFacebook, this);
		SessionManager.restoreInsider(Util.mInsider, this);
		SessionManager.restoreFoursquare(Util.mFoursquare, this);
	}
	
	
	
	private void isInsiderSetup(boolean isSetup, boolean usesPin)
	{
		if(Util.mInsider.isSessionValid())
		{
			if(!isSetup)
			{
				Util.mInsider.setUsesPin(usesPin);
				Intent intent = new Intent(LoginActivity.this, AcctCreate.class);
				startActivity(intent);
				finish();
			}
			else
			{
				Util.mInsider.setUsesPin(usesPin);
				Intent intent = new Intent(LoginActivity.this, startupClass);
				startActivity(intent);
				finish();
			}
		}
		else
		{
			mLoginButton.setVisibility(View.VISIBLE);
		}
				
		
	}
	
	private void doRestoreInsider()
	{
		if(!Util.mFacebook.isSessionValid())
			return;
		
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Insid3r Loading...");
		dialog.show();
		mLoginButton.setVisibility(View.GONE);
		if(Util.mFacebook.isSessionValid())
		{
			new Thread(new Runnable() {
				
				public void run() {
					// TODO Auto-generated method stub
					
				
					Insider.authorizeWithFacebook(Util.mFacebook, new Insider.InsiderLoginListener() {
						public void onError(String error) {
							dialog.dismiss();
							Util.log("error2: " + error);
							mHandler.post(new Runnable() {
								public void run() {
									Toast.makeText(mContext, "Unable to login to Insid3r", 2000).show();
									Intent intent = new Intent(LoginActivity.this, startupClass);
									startActivity(intent);
									finish();
								}
							});
							
						}
						
						public void onComplete(final Bundle params) {
							dialog.dismiss();
							mHandler.post(new Runnable() {
								public void run() {
									
									Util.log("onComplete Insider");
									final String access_token = params.getString("iToken");
									Util.mInsider.setAccessToken(access_token);
									Util.log("iToken: " + Util.mInsider.getAccessToken());
									Util.mInsider.setAccessExpires(params.getLong("expires"));
									Util.mInsider.setInsiderId(params.getString("insiderId"));
									Util.mInsider.setFacebookId(params.getString("facebookId"));
									isInsiderSetup(params.getBoolean("isSetup"), params.getBoolean("usesPin"));
									//mLoginButton.setVisibility(View.VISIBLE);
								}
							});
							
						}
					});
				}
			}).start();
		}
		else
			dialog.dismiss();
	}
	
	private void AuthorizeFacebook()
	{
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Logging On...");
		dialog.show();
		Util.mSession.initFacebook();
		if(!SessionManager.restoreFacebook(Util.mFacebook, this))
		{
			Util.mFacebook.authorize(this, 
					SessionManager.getFbPermissions(),
					new Facebook.DialogListener() {
				public void onFacebookError(FacebookError e) {	
					dialog.dismiss();
					if(e != null && mContext != null)
						Toast.makeText(mContext, "Error: " + e.getMessage(), 5000).show();
				}
				public void onError(DialogError e) {
					dialog.dismiss();
					if(e != null && mContext != null)
						Toast.makeText(mContext, "Error: " + e.getMessage(), 5000).show();
				}
				public void onComplete(Bundle values) {
					dialog.dismiss();
					Util.log("onComplete");
					for(String key: values.keySet())
					{
						Util.log(key);
					}
					SessionManager.saveFacebook(Util.mFacebook, mContext);
					doRestoreInsider();
				}
				public void onCancel() {
					if(mContext != null)
						Toast.makeText(mContext, "Canceled", 2000).show();
				}
			});
		}
		else
		{
			dialog.dismiss();
			//Util.mSession.initInsider();
			SessionManager.restoreInsider(Util.mInsider, this);
			doRestoreInsider();
		}
	}
	
	private void populateISelf()
	{
		String path = "user/self";
		Bundle params = new Bundle();
		params.putString("itoken", Util.mInsider.getAccessToken());
		try {
			String response = AHttpUtil.simplePost(Insider.I_BASE_PATH, params);
		}catch(Exception ex) {}
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		Util.mFacebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	
}
