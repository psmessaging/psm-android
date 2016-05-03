package com.psm.util;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.Util;

public class IUtil {

	private static final int MAX_ITERATIONS = 3;
	
	public static ArrayList<String> getBasicIds(JSONArray data)
	{
		ArrayList<String> idList = new ArrayList<String>();
		for (int i = 0; i < data.length(); i++) {
			try {
				
				JSONObject obj = data.getJSONObject(i);
				String id = normalizeFacebookId(obj.getString("id"));				
				if(!idList.contains(id));
					idList.add(id);	
					
			}catch(Exception ex) {Util.log("error: " + ex.getMessage());}
		}
		return idList;
	}
	
	
	public static String normalizeFacebookId(String id)
	{
		
		String[] splitId = id.split("[_]", 2);
		if(splitId.length == 2)
		{
			if(splitId[1].length() < 10)
				return id;
			else
				return splitId[1];
		}
		else
			return splitId[0];
	}
}
