package com.psm.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;


public class AHttpUtil {

	
	
	public static String encodeUrl(Bundle parameters)
	{
		return encodeUrl(parameters, true);
	}
	
	public static String encodeUrl(Bundle parameters, boolean sep)
	{
		String query = "";
		int x =0;
		for(String key : parameters.keySet())
		{
			
			String q = "?";
			if(sep == false)
				q = "&";
			query += (x==0 ? q : "&") + key + "=" + parameters.getString(key);
			x++;
			
		}
		
		return query;
		
	}
	
	public static String encodePostBody(Bundle parameters, String boundary) {
        if (parameters == null) return "";
        StringBuilder sb = new StringBuilder();

        for (String key : parameters.keySet()) {
            /*if (parameters.getByteArray(key) != null) {
                continue;
            }*/

            sb.append("Content-Disposition: form-data; name=\"" + key +
                    "\"\r\n\r\n" + parameters.getString(key));
            sb.append("\r\n" + "--" + boundary + "\r\n");
        }
        
        return sb.toString();
    }
	
	public static String encodePostParams(Bundle params)
	{
		
		StringBuilder sb = new StringBuilder();
		boolean and = true;
		for(String key: params.keySet())
		{
			if(!and)
			{
				sb.append("&");
			}
			
			sb.append(key + "=" + params.getString(key));
			and = false;
		}
		
		return sb.toString();
	}
	
	public static String openUrl(String url) throws MalformedURLException, IOException
	{
		return openUrl(url, "GET", new Bundle());
	}
	
	public static String openUrl(String url, Bundle params) throws MalformedURLException, IOException
	{
		return openUrl(url, "GET", params);
	}
	
	public static String simplePost(String url, Bundle params) throws MalformedURLException, IOException
	{
		return simplePost(url, params, "POST");
	}
	
	public static String simplePost(String url, Bundle params, String method) throws MalformedURLException, IOException 
	{
		OutputStream os;

		System.setProperty("http.keepAlive", "false");
		HttpURLConnection conn =
	            (HttpURLConnection) new URL(url).openConnection();
	        conn.setRequestProperty("User-Agent", System.getProperties().
	                getProperty("http.agent") + " agent");
	        
	        conn.setRequestMethod(method);
            conn.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            //conn.setRequestProperty("Connection", "Keep-Alive");
            
            conn.connect();
            
            os = new BufferedOutputStream(conn.getOutputStream());
            os.write(encodePostParams(params).getBytes());
            os.flush();
            
            String response = "";
	        try {
	            response = read(conn.getInputStream());
	        } catch (FileNotFoundException e) {
	            // Error Stream contains JSON that we can parse to a FB error
	            response = read(conn.getErrorStream());
	        }
	        return response;
	}
	
	public static String deleteUrl(String url, Bundle params) throws MalformedURLException, IOException
	{
		System.setProperty("http.keepAlive", "false");
		HttpURLConnection conn = (HttpURLConnection) new URL(url)
				.openConnection();
		conn.setRequestProperty("User-Agent", System.getProperties()
				.getProperty("http.agent") + " agent");
		conn.setRequestMethod("DELETE");
        conn.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded");
        conn.setDoOutput(false);
        conn.setDoInput(true);
        //conn.setRequestProperty("Connection", "Keep-Alive");
        conn.connect();
        
        String response = "";
        try {
            response = read(conn.getInputStream());
        } catch (FileNotFoundException e) {
            // Error Stream contains JSON that we can parse to a FB error
            response = read(conn.getErrorStream());
        }
        return response;
	}
	
	public static String openUrl(String url, String method, Bundle params)
	          throws MalformedURLException, IOException {
	        // random string as boundary for multi-part http post
	        String strBoundary = "kajrn4k1n23k1j23kj1ndun123ondnd2u1e1d1";
	        String endLine = "\r\n";

	        OutputStream os;

	        if (method.equals("GET")) {
	            url = url + encodeUrl(params);
	        }
	        Util.log("URL: " + url);
	        System.setProperty("http.keepAlive", "false");
	        HttpURLConnection conn =
	            (HttpURLConnection) new URL(url).openConnection();
	        conn.setRequestProperty("User-Agent", System.getProperties().
	                getProperty("http.agent") + " agent");
	        if (!method.equals("GET")) {
	            Bundle dataparams = new Bundle();
	            for (String key : params.keySet()) {
	                if (params.getByteArray(key) != null) {
	                        dataparams.putByteArray(key, params.getByteArray(key));
	                }
	            }

	            // use method override
	            /*if (!params.containsKey("method")) {
	                params.putString("method", method);
	            }*/

	            if (params.containsKey("access_token")) {
	                String decoded_token =
	                    URLDecoder.decode(params.getString("access_token"));
	                params.putString("access_token", decoded_token);
	            }

	            conn.setRequestMethod(method);
	            conn.setRequestProperty(
	                    "Content-Type",
	                    "multipart/form-data;boundary="+strBoundary);
	            conn.setDoOutput(true);
	            conn.setDoInput(true);
	            //conn.setRequestProperty("Connection", "Keep-Alive");
	            conn.connect();
	            os = new BufferedOutputStream(conn.getOutputStream());

	            os.write(("--" + strBoundary +endLine).getBytes());
	            os.write((encodePostBody(params, strBoundary)).getBytes());
	            os.write((endLine + "--" + strBoundary + endLine).getBytes());

	            if (!dataparams.isEmpty()) {

	                for (String key: dataparams.keySet()){
	                    os.write(("Content-Disposition: form-data; filename=\"" + key + "\"" + endLine).getBytes());
	                    os.write(("Content-Type: content/unknown" + endLine + endLine).getBytes());
	                    os.write(dataparams.getByteArray(key));
	                    os.write((endLine + "--" + strBoundary + endLine).getBytes());
	                    
	                }
	                
	            }
	            os.write((endLine + "--" + strBoundary + endLine).getBytes());
	            os.flush();
	        }
	        
	        String response = "";
	        try {
	            response = read(conn.getInputStream());
	        } catch (FileNotFoundException e) {
	            // Error Stream contains JSON that we can parse to a FB error
	            response = read(conn.getErrorStream());
	        }
	        return response;
	    }

	    public static String read(InputStream in) throws IOException {
	        StringBuilder sb = new StringBuilder();
	        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
	        for (String line = r.readLine(); line != null; line = r.readLine()) {
	            sb.append(line);
	        }
	        in.close();
	        return sb.toString();
	    }
	    
	    public static Bitmap getBitmap(String url) {
	        Bitmap bm = null;
	        try {
	            URL aURL = new URL(url);
	            URLConnection conn = aURL.openConnection();
	            conn.connect();
	            InputStream is = conn.getInputStream();
	            BufferedInputStream bis = new BufferedInputStream(is);
	            bm = BitmapFactory.decodeStream(new FlushedInputStream(is));
	            bis.close();
	            is.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            //if (httpclient != null) {
	            //    httpclient. ();
	            //}
	        }
	        return bm;
	    }
	    
	    static class FlushedInputStream extends FilterInputStream {
	        public FlushedInputStream(InputStream inputStream) {
	            super(inputStream);
	        }

	        @Override
	        public long skip(long n) throws IOException {
	            long totalBytesSkipped = 0L;
	            while (totalBytesSkipped < n) {
	                long bytesSkipped = in.skip(n - totalBytesSkipped);
	                if (bytesSkipped == 0L) {
	                    int b = read();
	                    if (b < 0) {
	                        break; // we reached EOF
	                    } else {
	                        bytesSkipped = 1; // we read one byte
	                    }
	                }
	                totalBytesSkipped += bytesSkipped;
	            }
	            return totalBytesSkipped;
	        }
	    }
	    
	    public static Location getLatitudeFromCity(String city) throws MalformedURLException, IOException
	    {
	    	//http://where.yahooapis.com/geocode?q=LAX&appid=wFlbAi4a&flags=JC
	    	Bundle params = new Bundle();
	    	params.putString("q", URLEncoder.encode(city));
	    	params.putString("appid", Util.YAHOO_APPID);
	    	params.putString("flags", "JC");
	    	String results = AHttpUtil.openUrl("http://where.yahooapis.com/geocode", params);
	    	Util.log(results);
	    	try {
		    	JSONObject jsonObj = new JSONObject(results).getJSONObject("ResultSet");
		    	if(jsonObj.getInt("Found") == 0)
		    		return null;
		    	JSONArray jaresults = jsonObj.getJSONArray("Results");
		    	JSONObject llobj = jaresults.getJSONObject(0);
		    	Location location = new Location("yahoo");
		    	location.setLatitude(llobj.getDouble("latitude"));
		    	location.setLongitude(llobj.getDouble("longitude"));
		    	return location;
	    	}catch(Exception ex) {}
	    	
	    	return null;
	    }
	    
	    public static String[] parseCatAndCity(String fullText)
	    {
	    	String[] string = fullText.split("[,]", 2);
	    	return string;
	    	
	    }
}
