package com.psm.android;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.psm.android.R;
import com.psm.android.fs.FoursquareLogin;
import com.psm.android.fs.FoursquareMan;

public class AcctList extends Activity implements OnItemClickListener {
	private static int SERVICE_COUNT = 2;
	private static String[] SERVICE_NAMES = {"facebook", "foursquare"};
	private static int[] SERVICE_ICONS = {R.drawable.facebook, R.drawable.fsicon};
	private boolean[] SERVICE_STATUS = {false,false};
	
	private Handler mHandler;
	private ListView mListView;
	private LayoutInflater mInflater;
	
	private AccountAdapter mAdapter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.acctlist);
		
		
		mInflater = getLayoutInflater();
		mHandler = new Handler();
		mListView = (ListView)findViewById(R.id.acct_listview);
		mListView.setAdapter(mAdapter = new AccountAdapter());
		mListView.setOnItemClickListener(this);
		
		
	}
	
	
	private class AccountAdapter extends BaseAdapter
	{
		
		public int getCount() {
			return SERVICE_COUNT;
		}

		public Object getItem(int arg0) {
			
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View cview, ViewGroup viewgroup) {
			View view = cview;
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.simple_list_item, null);	
				Holder holder = new Holder();
				holder.Icon = (ImageView)view.findViewById(R.id.simplelist_icon);
				holder.Textview1 = (TextView)view.findViewById(R.id.simplelist_text1);
				holder.Textview2 = (TextView)view.findViewById(R.id.simplelist_text2);
				holder.SubText1 = (TextView)view.findViewById(R.id.simplelist_subtext);
				holder.SubIcon = (ImageView)view.findViewById(R.id.simplelist_subimg);
				holder.SubProgress = (ProgressBar)view.findViewById(R.id.simplelist_progress);
				view.setTag(holder);
			}
			
			Holder holder = (Holder)view.getTag();
			holder.Icon.setImageResource(SERVICE_ICONS[position]);
			holder.Textview1.setText(SERVICE_NAMES[position]);
			
			if(SERVICE_STATUS[position])
			{
				holder.Textview2.setText("On");
				if(position == 1)
				{
					holder.SubText1.setText("Logged in as: " + Util.mFoursquare.getName());
					AsyncgetIcon(Util.mFoursquare.getIconUrl(), holder.SubIcon, holder.SubProgress);
				}
			}
			else
			{
				holder.SubIcon.setImageDrawable(null);
				holder.SubText1.setText("");
				holder.Textview2.setText("Off");
			}
			if(SERVICE_STATUS[0] == false && position != 0)
				view.setEnabled(true);
			else
				view.setEnabled(true);
			
			return view;
		}
		
		private class Holder
		{
			ImageView Icon;
			ImageView SubIcon;
			ProgressBar SubProgress;
			TextView SubText1;
			TextView Textview1;
			TextView Textview2;
			
		}
		
	}

	
	public void AsyncgetIcon(final String url, final ImageView view, final ProgressBar progress)
	{
		if(progress != null)
			progress.setVisibility(View.VISIBLE);
		
		new Thread(new Runnable() {
			public void run() {
				final Bitmap bm = ACacheUtil.getBitmap(url);
				mHandler.post(new Runnable() {
					public void run() {
						view.setImageBitmap(bm);
						if(progress != null)
							progress.setVisibility(View.INVISIBLE);
					}
				});
			}
		}).start();
	}

	public void onItemClick(AdapterView<?> adapter, View view, int pos, long other) {
				
		if(pos == 0) //faceboook
		{
			if(SERVICE_STATUS[0] == false)
			{
				Util.mSession.initFacebook();
				Util.mFacebook.authorize(this, 
						SessionManager.getFbPermissions(),
						new Facebook.DialogListener() {
					public void onFacebookError(FacebookError e) {	
						Toast.makeText(getParent(), "Error: " + e.getMessage(), 5000).show();
						Util.log(e.getMessage());
					}
					public void onError(DialogError e) {
						Toast.makeText(getParent(), "Error: " + e.getMessage(), 5000).show();
						Util.log(e.getMessage());
					}
					public void onComplete(Bundle values) {
						//TODO: save session
						for(String key : values.keySet())
						{
							Util.log(key + ": " + values.getString(key));
							
						}
					}
					public void onCancel() {
						Toast.makeText(getParent(), "Canceled", 2000).show();
					}
				});
			}
			else
			{
				new AlertDialog.Builder(this).setTitle("Logout of Facebook?")
					.setNegativeButton("No", null)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							SERVICE_STATUS[0] = false;
							mAdapter.notifyDataSetChanged();
						}
					}).show();
			}
		}
		else if(pos == 1) //foursquare
		{
			
			if(SERVICE_STATUS[1] == false)
			{
				//Util.mSession.LoginFacebook(this);
				Intent intent = new Intent(AcctList.this, FoursquareLogin.class);
				startActivityForResult(intent, 1);
			}
			else
			{
				new AlertDialog.Builder(this).setTitle("Logout of Foursquare?")
					.setNegativeButton("No", null)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							SERVICE_STATUS[1] = false;
							mAdapter.notifyDataSetChanged();
						}
					}).show();
			}
			
			
			
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == 1 && resultCode == RESULT_OK)
		{
			
			if(Util.mFoursquare.getToken() != null)
				SERVICE_STATUS[1] = true;
			Util.mFoursquare.setLoginTime(new Date().getTime());
			SessionManager.saveFoursquare(Util.mFoursquare, this);
			mAdapter.notifyDataSetChanged();
			Intent intent = new Intent(this, FoursquareMan.class);
			startActivity(intent);
			
		}
		else
			Util.mFacebook.authorizeCallback(requestCode, resultCode, data);
		
	}
	
}
