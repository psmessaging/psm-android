package com.psm.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Date;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.util.EncodingUtils;

import com.psm.android.AHttpUtil.FlushedInputStream;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;

public class ACacheUtil extends Application {
	
	private static String CACHE_DIR = "/data/data/com.psm.android/cache";
	private static long CACHE_TIME = 7200000L;
	

	public static String getUrl(String url, boolean force)
	{
		return getUrl(url, new Bundle(), force);
	}
	
	public static String getUrl(String url)
	{
		return getUrl(url, new Bundle(), false);
	}
	
	
	public static String getUrl(String url, Bundle params, boolean force)
	{
		try {
			if(params == null)
				params = new Bundle();
			
			final String cacheDir = CACHE_DIR;
			File file = new File(cacheDir, getMd5(url + AHttpUtil.encodeUrl(params))+".cx");
			
			if(isCached(url) && !force && file.length() > 0)
			{
				//Util.log("cached");
				FileInputStream fi = new FileInputStream(file);
				String fileStr = AHttpUtil.read(fi);
				fi.close();
				return fileStr;
			}
			else
			{
				//Util.log("notcached");
				FileOutputStream fo = new FileOutputStream(file);
				String string  = AHttpUtil.openUrl(url, "GET", params);
				fo.write(string.getBytes());
				fo.flush();
				fo.close();
				return string;
			}
			
		}catch(Exception ex){Util.log("error4: " + ex.getMessage());}
		
		return null;
	}	
	
	public static Bitmap getBitmap(String url)
	{
		return getBitmap(url, false);
	}
	
	public static Bitmap getBitmap(String url, boolean force)
	{
		try {
			final String cacheDir = CACHE_DIR;
			File file = new File(cacheDir, getMd5(url)+".cx");
			
			if(isCached(url) && !force && file.length() > 0)
			{
					//Util.log("cached");
					FileInputStream fi = new FileInputStream(file);
		            Bitmap bm = BitmapFactory.decodeStream(new FlushedInputStream(fi));
		            fi.close();
		            return bm;
				
			}
			else
			{
				//Util.log("notcached");
				FileOutputStream fo = new FileOutputStream(file);
				Bitmap bm  = AHttpUtil.getBitmap(url);
				bm.compress(CompressFormat.PNG, 100, fo);
				fo.flush();
				fo.close();
				return bm;
			}
		}catch(Exception ex){Util.log("error3: " + ex.getMessage());}
		
		
		return null;
	}
	
	public static boolean isCached(String url)
	{
		try {
			
			final String cacheDir = CACHE_DIR;
			File file = new File(cacheDir, getMd5(url) +".cx");
			if(file.exists())
			{
				if((new Date().getTime())-file.lastModified() > CACHE_TIME)
					return false;
				else
					return true;
			}
			else
				return false;
			
		}catch(Exception ex){
			return false;
		}
	}
	
	public static String getMd5(String string)
	{
		try {
			
			final MessageDigest messageDigest = MessageDigest.getInstance("MD5"); 
			messageDigest.reset(); 
			messageDigest.update(string.getBytes("UTF-8")); 
			final byte[] resultByte = messageDigest.digest();
			final char[] result = Hex.encodeHex(resultByte);
			return new String(result);
			
		}catch(Exception ex){}
		
		return null;
		
	}
	
}
