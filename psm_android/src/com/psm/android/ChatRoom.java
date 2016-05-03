package com.psm.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class ChatRoom extends Activity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
	}
}
