package com.psm.util;

import java.util.Date;

import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.android.Facebook;
import com.psm.android.AHttpUtil;
import com.psm.android.LoginActivity;
import com.psm.android.MainA;
import com.psm.android.SessionManager;
import com.psm.android.Util;

public class Insider {

	//public final static String I_BASE_PATH = "http://192.168.1.122:8080/proj-war/i/";
	public final static String I_BASE_PATH = "https://zeta.insid3r.com/proj-war/i/";
	public final static String SMS_IDENTIFIER = "/?";
	
	private String 	mAccess_Token;
	private String 	mFacebookId;
	private String mInsiderId;
	private boolean isPinned;
	private boolean usesPin;
	
	private long		mExpires;
	
	public String getAccessToken()					{ return mAccess_Token;}
	public void setAccessToken(String accessToken)	{ this.mAccess_Token = accessToken;}
	public long getExpires()						{ return mExpires;}
	public void setAccessExpires(long expires)		{ this.mExpires = expires;}
	public String getFacebookId() 					{ return mFacebookId == null ? "" : mFacebookId;}
	public void setFacebookId(String mFacebookId) 	{ this.mFacebookId = mFacebookId;}
	public String getInsiderId() 					{	return mInsiderId;}
	public void setInsiderId(String mInsiderId) 	{ this.mInsiderId = mInsiderId;}
	
	
	
	public boolean isPinned() {
		return isPinned;
	}
	public void setPinned(boolean isPinned) {
		this.isPinned = isPinned;
	}
	public boolean isUsingPin() {
		return usesPin;
	}
	public void setUsesPin(boolean usesPin) {
		this.usesPin = usesPin;
	}
	
	public boolean isSessionValid()
	{
		Date date = new Date();
		//Util.log(mAccess_Token);
		Util.log("Expires: " + mExpires);
		Util.log("Current Time: " + date.getTime());
		Util.log("acesstoken: " + getAccessToken());
		if(mExpires == -1 && mAccess_Token != null) return true;
		
		if(mExpires == 0 || mAccess_Token == null) return false;
		if(date.getTime() < mExpires)
			return true;
		else
			return false;
	}
	
	public boolean isLoggedIn()
	{
		Util.log(this.usesPin);
		Util.log(this.isSessionValid());
		if(this.usesPin && isPinned)
			return true;
		
		if(!this.usesPin && isSessionValid())
			return true;
		
		return false;
	}
	
	public static void authorizeWithFacebook(Facebook facebook, InsiderLoginListener listener)
	{
		//Send Access Token
		Util.log("AuthorizeWithFacebook");
		String path = "user/authorize";
		String method = "POST";
		Bundle params = new Bundle();
		
		params.putString("access_token", facebook.getAccessToken());
		try {
			
			String results = AHttpUtil.simplePost(I_BASE_PATH + path, params);
			Util.log("ResultsA: " + results);
			JSONObject obj = new JSONObject(results);
			//Util.mSession.initInsider();
			//Util.mInsider.setAccessToken(obj.getString("iToken"));
			//Util.mInsider.setAccessExpires(obj.getLong("insiderExpires"));
			//Util.mInsider.setFacebookId(obj.getString("id"));
			
			Bundle bundle = new Bundle();
			bundle.putString("iToken", obj.getString("iToken"));
			bundle.putLong("expires", obj.getLong("insiderExpires"));
			bundle.putString("facebookId", obj.getString("id"));
			bundle.putString("insiderId", obj.getString("insiderId"));
			bundle.putBoolean("isSetup", obj.getBoolean("isSetup"));
			bundle.putBoolean("usesPin", obj.getBoolean("usesPin"));
			listener.onComplete(bundle);
			
		}catch(Exception ex) { listener.onError(ex.getMessage()); }
	}
	
	public static interface InsiderLoginListener
	{
		public void onComplete(Bundle params);
		public void onError(String error);
	}
	
	public static String getUrlPath(String path)
	{
		return I_BASE_PATH + path;
	}
}
