package com.psm.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class AImageView extends ImageView {

	private Handler mHandle = new Handler();
	private ProgressBar mProgres;
	
	public AImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public AImageView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public AImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
}
