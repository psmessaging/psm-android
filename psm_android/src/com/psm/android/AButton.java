package com.psm.android;

import com.psm.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;

public class AButton extends Button {

	public AButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public AButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AButton(Context context) {
		super(context);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			this.setBackgroundResource(R.drawable.bg_gray_rnd_sel);
		}
		else
			this.setBackgroundResource(R.drawable.bg_gray_rnd);
		
		return super.onTouchEvent(event);
	}
	
	
}
