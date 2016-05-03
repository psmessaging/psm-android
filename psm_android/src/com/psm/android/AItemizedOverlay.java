package com.psm.android;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class AItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	private Context mContext;
	private ArrayList<OverlayItem> mList = new ArrayList<OverlayItem>();
	
	public AItemizedOverlay(Drawable defaultMarker, Context context) {
		super(boundCenter(defaultMarker));
		mContext = context;
	}

	public void addOverlay(OverlayItem overlay)
	{
		mList.add(overlay);
		populate();
	}

	
	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return mList.get(i);
	}

	@Override
	public int size() {
		return mList.size();
	}

}
