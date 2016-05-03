package com.psm.android;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.apache.commons.codec.Encoder;
import org.apache.http.util.EncodingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;

import android.app.Application;
import android.location.Location;
import android.os.Bundle;

public class AMapUtil extends Application {

	private static String URL = "https://maps.googleapis.com/maps/api/geocode/json";//?parameters
	
	public static Location getLocationFromCity(String city) throws MalformedURLException, IOException
	{
		
		Bundle params = new Bundle();
		params.putString("sensor", "false");
		params.putString("address", URLEncoder.encode(city));
		try {
			
			JSONObject result = new JSONObject(AHttpUtil.openUrl(URL, params));
			JSONObject geometry = result.getJSONArray("results").getJSONObject(0)
											.getJSONObject("geometry").getJSONObject("location");
			Location location = new Location("sensor");
			location.setLatitude(geometry.getDouble("lat"));
			location.setLongitude(geometry.getDouble("lng"));
			return location;
			
		} catch (JSONException e) {
		}
		
		return null;
	}
	
	
}
