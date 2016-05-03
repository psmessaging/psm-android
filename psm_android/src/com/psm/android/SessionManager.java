package com.psm.android;

import java.util.Date;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.psm.android.fs.Foursquare;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.Toast;

public class SessionManager extends Application  {

	public static String FB_PREFS = "facebookprefs";
	public static String FS_PREFS = "foursquareprefs";
	public static String IS_PREFS = "insiderprefs";
	public static String SMS_PREFS ="smsprefs";
	
	private static String[] mFbPermissions =  { "email", "publish_checkins", "offline_access", 
			"friends_online_presence", "user_online_presence", "manage_friendlists", "read_mailbox",
			"read_requests", "read_stream", "manage_notifications", "publish_checkins", 
			"friends_activities", "user_activities",
			"publish_stream", "user_likes", "friends_likes",
			"user_about_me", "friends_about_me", "user_activities", "friends_activities",
			"user_birthday", "friends_birthday", "friends_checkins", "user_checkins",
			"user_groups", "user_hometown", "friends_hometown", "user_photos", "user_status", 
			"friends_photos", "friends_status", "user_location", 
			"friends_location", "publish_actions" };
	
	public static String[] getFbPermissions()
	{
		return mFbPermissions;
	}
	
	public void initFacebook()
	{
		if(Util.mFacebook == null)
			Util.mFacebook = new Facebook(Util.FB_APPID);		
	}
	
	public void initInsider()
	{
		if(Util.mInsider == null)
			Util.mInsider = new Insider();
	}
	
	public void initFoursquare()
	{
		if(Util.mFoursquare == null)
			Util.mFoursquare = new Foursquare();
	}
	
	/**
	 * 
	 * Facebook Sessions
	 * 
	 */
	
	public static boolean saveFacebook(Facebook facebook, Context context)
	{
		return saveFacebook(facebook.getAccessToken(), facebook.getAccessExpires(), context);
	}
	
	public static boolean saveFacebook(String token, long expires, Context context)
	{
		Editor editor = context.getSharedPreferences(FB_PREFS, Context.MODE_PRIVATE).edit();
		editor.putString("token", token);
		editor.putLong("expire", expires);
		return editor.commit();
	}
	
	public static boolean restoreFacebook(Facebook facebook, Context context)
	{
		Util.log("Restore Facebook Session");
		
		if(facebook == null) return false;
		
		SharedPreferences prefs = context.getSharedPreferences(FB_PREFS, Context.MODE_PRIVATE);
		String atoken = prefs.getString("token", null);
		//Util.log(atoken);
		
		long aexpire = prefs.getLong("expire", -1);
		//Util.log(String.valueOf(aexpire));
		if(atoken == null || aexpire == -1)
			return false;
		else
		{
			Date now = new Date();
			facebook.setAccessExpires(aexpire);
			facebook.setAccessToken(atoken);
			return facebook.isSessionValid();
		}
		
	}
	
	
	/**
	 * 
	 * Foursquare Sessions
	 * 
	 */
	
	public static boolean saveFoursquare(Foursquare foursquare, Context context)
	{
		return saveFoursquare(foursquare.getToken(), foursquare.getLoginTime(), context);
	}
	
	public static boolean saveFoursquare(String token, long loginTime, Context context)
	{
		
		Editor prefs = context.getSharedPreferences(FS_PREFS, Context.MODE_PRIVATE).edit();
		prefs.putString("token", token);
		prefs.putLong("login", loginTime);
		return prefs.commit();
		
	}
	
	public static boolean restoreFoursquare(Foursquare foursquare, Context context)
	{
		if(foursquare == null) return false;
		
		SharedPreferences prefs = context.getSharedPreferences(FS_PREFS, Context.MODE_PRIVATE);
		String atoken = prefs.getString("token", null);
		long alogin = prefs.getLong("login", 0);
		
		if(atoken == null)
			return false;
		else
		{
			foursquare.setLoginTime(alogin);
			foursquare.setToken(atoken);
			return true;
		}
		
	}
	
	/**
	 * 
	 * Insider Sessions
	 * 
	 */
	
	public static boolean saveInsider(Insider insider, Context context)
	{
		Editor editor = context.getSharedPreferences(IS_PREFS, Context.MODE_PRIVATE).edit();
		editor.putString("token", insider.getAccessToken());
		editor.putLong("expires", insider.getExpires());
		editor.putString("fbid", insider.getFacebookId());
		return editor.commit();
	}
	
	public static boolean restoreInsider(Insider insider, Context context)
	{
		
		SharedPreferences prefs = context.getSharedPreferences(IS_PREFS, Context.MODE_PRIVATE);
		String token = prefs.getString("token", null);
		long expires = prefs.getLong("expires", 0);
		String id = prefs.getString("fbid", null);
		insider.setAccessExpires(expires);
		insider.setAccessToken(token);
		insider.setFacebookId(id);
		return insider.isSessionValid();
		
		
	}
}
