package com.psm.android;


import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;

public class AFacebook {

	public static final String BASE_PATH = "https://graph.facebook.com/";
	
	public static String getFullUrl(String path, Bundle params)
	{
		if(params == null) params = new Bundle();
		//if(Util.mFacebook != null)
		//{
			params.putString("access_token", getAccessToken());//Util.mFacebook.getAccessToken());
		//}
		
		String fpath = BASE_PATH + path;
		String query = AHttpUtil.encodeUrl(params);
		return fpath + query;
	}
	
	public static String getAccessToken()
	{
		return Util.mFacebook.getAccessToken();//"BAAEAh4bHixcBAKifghyRFtadZCh6B0H7qXIurZBJqt7i821C6Kbm7MlcwungKpDvFc83b2ty8ve7B7UiyPkUu8vZCXTizTFKPODsku7ZBElZC68Edyf5tz04F2jNEHbwZD";
	}
	
	public static String openUrl(String path, Bundle params, String method)
	{
		return null;
	}
	
	public static String formatTime(String time)
	{
		SimpleDateFormat sformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		try {
			Date date = sformat.parse(time);
			return date.toLocaleString();
		}catch(Exception ex){ }
		
		return "";
		
		
		
		
	}
}
