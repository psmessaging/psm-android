package com.psm.android.fs;

import java.net.URLEncoder;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;
import com.psm.android.ACacheUtil;
import com.psm.android.AHttpUtil;
import com.psm.android.Util;

import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

public class Foursquare {

	private String mToken =null;
	private long mLogin =0;
	private String mFoursquareName =null;
	
	private JSONObject mSelf =null;
	
	public final static String BASE_PATH = "https://api.foursquare.com/v2/"; 
	public final static String VERSION = "20120101";
	
	public static GeoPoint getGeoPoint(Location location)
	{		
		return null;
	}
	
	public static GeoPoint getGeoPoint(double latitude, double longitude)
	{
		GeoPoint geo = new GeoPoint( (int)((float)latitude*1E6) , (int)((float)longitude*1E6) );
		return geo;
		
	}
	
	public static JSONArray getItemList(JSONObject jsonResult)
	{
		JSONArray venues = new JSONArray();
		try {
			JSONObject response = jsonResult.getJSONObject("response");
			if(response.has("groups"))
			{
				JSONArray groups = response.getJSONArray("groups");
				for (int i = 0; i < groups.length(); i++) {
					JSONObject obj = groups.getJSONObject(i);
					JSONArray items = obj.getJSONArray("items");
					for (int j = 0; j < items.length(); j++) {
						venues.put(items.getJSONObject(j));
					}
				}
			}
		}catch(Exception ex){}
		return venues;
	}
	public static JSONObject getVenue(JSONObject jsonResult)
	{
		try {
			
			JSONObject response = jsonResult.getJSONObject("response");
			return response.getJSONObject("venue");
			
		}catch(Exception ex) {}
		
		return null;
		
	}
	
	public static JSONArray getVenueList(JSONObject jsonResult)
	{
		
		JSONArray venues = new JSONArray();
		try {
			
			JSONObject response = jsonResult.getJSONObject("response");
			if(response.has("venues"))
			{
				return response.getJSONArray("venues");
			}
			
		}catch(Exception ex){}
		return null;
		
	}
	
	public static String getIconUrl(JSONArray jsonCategory)
	{
		return getIconUrl(jsonCategory, "64");
	}
	
	public static String getIconUrl(JSONArray jsonCategory, String size)
	{
		if(jsonCategory == null) return null;
		
		if(jsonCategory.length() > 0)
		{
			try {
			JSONObject icon = jsonCategory.getJSONObject(0).getJSONObject("icon");
			String url = icon.getString("prefix") + size + icon.getString("name");
			return url;
			}catch(Exception ex){}
		}
		
		return null;
			
	}
	
	public static String getLL(Location location)
	{
		if(location == null) return null;
		String loc = String.format("%.1f,%.1f", location.getLatitude(), location.getLongitude());
		return loc;
	}
	
	public static String getFullUrl(String path)
	{
		return getFullUrl(path, null);
	}
	
	public static String getFullUrl(String path, Bundle params)
	{
		if(params == null) params = new Bundle();
		params.putString("v", VERSION);
		
		if(Util.mFoursquare != null)
			if(Util.mFoursquare.getToken() != null)
				params.putString("oauth_token", Util.mFoursquare.getToken());
		
		String fpath = BASE_PATH + path;
		String query = AHttpUtil.encodeUrl(params);
		return fpath + query;
		
	}
	
	public String getToken()
	{
		return mToken;
	}
	
	public void setToken(String token)
	{
		mToken = token;
		mLogin = new Date().getTime();
	}
	
	public void setLoginTime(long ldate)
	{
		mLogin = ldate;
	}
	
	public long getLoginTime()
	{
		return mLogin;
	}
	
	public String getName()
	{
		if(populateSelf())
		{
			try{
				
				JSONObject user = mSelf.getJSONObject("user");
				return user.getString("firstName");
				
			}catch (Exception e) {
				Util.log(e.getMessage());
				return "N/A";
			}
		}
		else
			return "N/A";	
		
	}
	
	public String getIconUrl()
	{
		if(populateSelf())
		{
			try{
				
				JSONObject user = mSelf.getJSONObject("user");
				return user.getString("photo");
				
			}catch (Exception e) {
				return null;
			}
		}
		else
			return null;
	}
	
	private boolean populateSelf()
	{
		return populateSelf(false);
		
	}
	private boolean populateSelf(boolean Update)
	{
		if(!Update && mSelf != null) return true;
		
		try {
			JSONObject fresponse = new JSONObject(
					ACacheUtil.getUrl(
							getFullUrl("users/self"), true));
			JSONObject resp = fresponse.getJSONObject("response");
			Util.log(resp);
			mSelf = resp;
			return true;
			

		} catch (Exception e) {
			Util.log("Error: " + e.getMessage());
			if(mSelf != null)return true;
			else return false;
		}
		
	}
	
	
	
}
