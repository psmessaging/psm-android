package com.psm.android;

import java.util.ArrayList;

import com.psm.android.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SideMenu extends Activity {

	private Context mContext;
	private LayoutInflater mInflater;
	
	private sidemenuAdapter mAdapter;
	private ListView mList;
	private Button mBtnMenu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.side_menu);
		mInflater = getLayoutInflater();
				
		mContext = this;
		mList = (ListView)findViewById(R.id.side_menu_list);
		mAdapter = new sidemenuAdapter();
		mList.setAdapter(mAdapter);
		mBtnMenu = (Button)findViewById(R.id.side_menu_button);
		mBtnMenu.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				finish();
				overridePendingTransition(R.anim.main_slide_in, R.anim.side_out_animation);
			}
		});
	}
	
	private class sidemenuAdapter extends BaseAdapter
	{

		public int getCount() {
			// TODO Auto-generated method stub
			return 10;
		}

		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		public View getView(int arg0, View convertView, ViewGroup arg2) {
			View view = convertView;
			if(view == null)
			{
				view = mInflater.inflate(R.layout.side_menu_cell_item, null);
				sideHolder holder = new sideHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.smc_icon);
				holder.txtCaption = (TextView)view.findViewById(R.id.smc_text);
				view.setTag(holder);
				
			}
			
			sideHolder holder = (sideHolder)view.getTag();
			holder.txtCaption.setText("Test Menu Item");
			return view;
		}
		
	}
	
	private class sideHolder
	{
		ImageView imgIcon;
		TextView txtCaption;
	}
	@Override
	public void onBackPressed() {
		Util.log("Side Menu Back Pressed");
		super.onBackPressed();
		overridePendingTransition(R.anim.main_slide_in, R.anim.side_out_animation);
	}
	
	private static class SideMenuSection
	{
		public static String[] Facebook = { "Status", "Check In", "Friends" };
		public static String[] Foursquare = { "Me", "" };
	}
	
}
