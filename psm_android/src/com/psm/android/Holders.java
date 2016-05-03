package com.psm.android;

import android.gesture.GestureOverlayView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Holders {

	
	public static class LocationCellHolder
	{
		public String id;
		public ProgressBar prgBar;
		public TextView txtMessage;
		public ImageView categoryIcon;
		public TextView txtTitle;
		public TextView txtAddress;
		public TextView txtDistance;
		public TextView txtTip;
		public TextView txtTipDate;
		public LinearLayout tipLayout;
		
	}
	
	public static class FBNavHolder
	{
		public int viewType;
		public boolean isInsider = false;
		public boolean isOwner = false;
		public String ownerId;
		public String id;
		public ImageButton btnRemove;
		public ImageView imgUserIcon;
		public TextView txtUserName;
		public TextView txtText;
		public LinearLayout layoutExtended;
		public ImageView imgExtended;
		public TextView txtExtLink;
		public TextView txtExtText;
		public TextView txtDate;
		public ImageView imgVia;
		public LinearLayout clLayout;
		public Button btnLike;
		public Button btnComment;
		public TextView txtCommentCount;
		public TextView txtLikeCount;
		public ImageView iconComment;
		public ImageView iconLike;
		public GestureOverlayView gesture;
	}
}
