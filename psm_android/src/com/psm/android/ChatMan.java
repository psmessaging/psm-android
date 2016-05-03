package com.psm.android;

import com.psm.android.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class ChatMan extends Activity {

	private static final int REQUEST_CREATE = 4;
	
	private Button btnCreate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chat_man);
		
		btnCreate = (Button)findViewById(R.id.chat_man_create);
		btnCreate.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(ChatMan.this, ChatCreate.class);
				startActivityForResult(intent, REQUEST_CREATE);
			}
		});
	}
}
