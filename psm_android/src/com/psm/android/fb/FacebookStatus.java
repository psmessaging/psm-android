package com.psm.android.fb;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.Util.JsonNameComparator;
import com.psm.util.IUtil;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IInterface;
import android.telephony.CellLocation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class FacebookStatus extends Activity implements OnItemClickListener {

	
	private Context mContext;
	private Handler mHandler;
	private LayoutInflater mInflater;
	
	private Button mBtnPost, mBtnGroup, mBtnPhoto;
	private LinearLayout mLayout;
	
	private EditText mEditStatus, mEditStealth;
	private ImageView mImgIcon;
	private TextView mTxtTitle;
	private ImageView mImgPost;
	
	private String mToFbid;
	private ListView mGroupList;
	private JSONArray mJsonGroups;
	private ArrayList<JSONObject> sortedGroups = new ArrayList<JSONObject>();
	
	private boolean isShowing = false;
	private LinearLayout mGroupProgress;
	private groupListAdapter mGroupAdapter;
	
	private boolean groupCache = true;
	
	private static final int CAMERA_REQUEST = 8;
	private GroupcellHolder lastSelected = null;
	private int lastSelectedPosition = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_status);
		mInflater = getLayoutInflater();
		lastSelectedPosition = -1;
		
		mContext = this;
		mHandler = new Handler();
		mBtnPost = (Button)findViewById(R.id.fb_status_btnpost);
		mBtnPhoto = (Button)findViewById(R.id.fb_status_btnphoto);
		mImgPost = (ImageView)findViewById(R.id.fb_status_imgpost);
		mBtnGroup = (Button)findViewById(R.id.fb_status_btnchoosegroup);
		mLayout = (LinearLayout)findViewById(R.id.fb_status_llscroll);
		mGroupProgress = (LinearLayout)findViewById(R.id.fb_status_progress);
		
		
		
		mGroupAdapter = new groupListAdapter();
		mGroupList = (ListView)findViewById(R.id.fb_status_grouplist);
		mGroupList.setAdapter(mGroupAdapter);
		mGroupList.setOnItemClickListener(mGroupAdapter);
		
		mEditStatus = (EditText)findViewById(R.id.fb_status_edittext);
		mEditStealth = (EditText)findViewById(R.id.fb_status_stealth);
		if(!Util.mInsider.isLoggedIn())
		{
			mEditStealth.setVisibility(View.GONE);
			mBtnGroup.setVisibility(View.GONE);
		}
		mImgIcon = (ImageView)findViewById(R.id.fb_status_imgicon);
		mTxtTitle = (TextView)findViewById(R.id.fb_status_txtupdate);
		mImgIcon.setImageResource(R.drawable.facebookuser);
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{	
			if(extras.containsKey("com.psm.android.id"))
			{
				mToFbid = extras.getString("com.psm.android.id");
			}
			else
				mToFbid = "me";
		}
		else
			mToFbid = "me";
		
		if(mToFbid.compareToIgnoreCase("me") == 0)
			mTxtTitle.setText("Update Status");
		else
			mTxtTitle.setText("Write Post");
		
		mBtnPost.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				postStatus();
			}
		});
		
		mBtnPhoto.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				new AlertDialog.Builder(mContext).setTitle("Take Picture From:")
					.setItems(new String[] {"Take Picture", "Choose From Gallery"}, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if(which == 0)
							{
								Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); 
				                
								startActivityForResult(cameraIntent, CAMERA_REQUEST );
							}
						}
					}).create().show();
			}
		});
		
		mBtnGroup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				
				if(isShowing == false)
				{
					((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(mEditStatus.getWindowToken(), 0);
					getGroups();
					mLayout.setVisibility(View.INVISIBLE);
					isShowing = true;
					Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 1.0f, 
							Animation.RELATIVE_TO_SELF, 0.0f);
					animation.setFillAfter(true);
					animation.setFillBefore(true);
					animation.setFillEnabled(true);
					animation.setDuration(800);
					animation.setInterpolator(new AccelerateInterpolator());
					mLayout.startAnimation(animation);
					
				}
				else
				{
					//((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
					//.hideSoftInputFromWindow(mEditStatus.getWindowToken(), 0);
					
					isShowing = false;
					Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 0.0f, 
							Animation.RELATIVE_TO_SELF, 1.0f);
					animation.setFillAfter(true);
					animation.setFillBefore(true);
					animation.setFillEnabled(true);
					animation.setDuration(800);
					animation.setInterpolator(new DecelerateInterpolator());
					mLayout.startAnimation(animation);
					mLayout.setVisibility(View.GONE);
				}
			}
		});
		
	}
	
	private void fillPhotoIcon()
	{
		
		
		new Thread(new Runnable() {
			final Bitmap bitmap = ACacheUtil.getBitmap("https://graph.facebook.com/me/picture?access_token=" +
					AFacebook.getAccessToken());
			public void run() {
				mHandler.post(new Runnable() {
					public void run() {
						mImgIcon.setImageBitmap(bitmap);
					}
				});
			}
		}).start();
		
		
		

	}
	
	@Override
	protected void onResume() {
		fillPhotoIcon();
		super.onResume();
	}
	
	private void postStatus()
	{
		final ProgressDialog dialog = new ProgressDialog(mContext);
		if(mToFbid.compareToIgnoreCase("me") == 0)
			dialog.setMessage("Updating Status...");
		else
			dialog.setMessage("Writing Post...");
		dialog.show();
		
		new Thread(new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				params.putString("message", mEditStatus.getText().toString());
				String url = AFacebook.getFullUrl(mToFbid + "/feed", null);
				Util.log("URL: " + url);
				try {
					String results = AHttpUtil.openUrl(url, "POST", params);
					//String results = "{id:\"100003327843280_155255657928695\"}";
					Util.log(results);
					
					JSONObject result = new JSONObject(results);
					String publicMessageId = result.getString("id");
					mHandler.post(new Runnable() {
						public void run() {
							dialog.setMessage("Posting to Insid3r");
						}
					});
					checkForPrivateMessage(publicMessageId);
					
				}catch(Exception ex) {}
				mHandler.post(new Runnable() {
					
					public void run() {
						dialog.dismiss();
						setResult(RESULT_OK);
						finish();
						Intent intent = new Intent("com.psm.android.REFRESH");
						sendBroadcast(intent);
						
					}
				});
			}
		}).start();
	}
	
	private void checkForPrivateMessage(String messageId)
	{
		EditText txtStealth = (EditText)findViewById(R.id.fb_status_stealth);
		if(txtStealth.getText().length() > 0)
		{
			//Has stealth message post it to private server
			Bundle params = new Bundle();
			params.putString("iToken", Util.mInsider.getAccessToken());
			params.putString("id", IUtil.normalizeFacebookId(messageId));
			params.putString("message", URLEncoder.encode(txtStealth.getText().toString()));
			if(lastSelectedPosition > 0)
				params.putString("groupId", lastSelected.groupId);
			
			try {
				String results = AHttpUtil.simplePost(Insider.I_BASE_PATH + "msg/facebook", params);
				Util.log("Results: " + results);
			}catch(Exception ex){}

		}
	}
	
	/*
	 * 
	 * Group Methods
	 * 
	 * 
	 *
	 */
	private class groupListAdapter extends BaseAdapter implements OnItemClickListener
	{

		public int getCount() {
			if(mJsonGroups == null) return 0;
			return mJsonGroups.length()+1;
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.group_cell_group, null);
				GroupcellHolder holder = new GroupcellHolder();
				holder.txtGroupName = (TextView)view.findViewById(R.id.group_cell_group_text);
				holder.imgView = (ImageView)view.findViewById(R.id.group_cell_group_img);
				holder.selectedItem = (ImageView)view.findViewById(R.id.group_cell_group_check);
				holder.txtGroupCount = (TextView)view.findViewById(R.id.group_cell_group_count);
				holder.lockIcon = (ImageView)view.findViewById(R.id.group_cell_group_lock);
				view.setTag(holder);
			}
			GroupcellHolder holder = (GroupcellHolder)view.getTag();
			try {
				if(position == 0)
				{
					holder.groupId = null;
					holder.txtGroupName.setText("Everyone");
					holder.txtGroupCount.setVisibility(View.INVISIBLE);
					if(lastSelected == null)
					{
						lastSelected = holder;
						lastSelectedPosition = 0;
					}
					holder.lockIcon.setVisibility(View.GONE);
						
				}
				else
				{
					JSONObject obj = sortedGroups.get(position-1);
					holder.groupId = obj.getString("id");
					holder.txtGroupName.setText(obj.getString("name"));
					holder.txtGroupCount.setVisibility(View.VISIBLE);
					holder.txtGroupCount.setText(obj.getString("count"));
					if(obj.has("private"))
					{
						if(obj.getBoolean("private"))
							holder.lockIcon.setVisibility(View.VISIBLE);
						else
							holder.lockIcon.setVisibility(View.GONE);
					}
					else
						holder.lockIcon.setVisibility(View.GONE);
				}
				if(lastSelectedPosition == position)
					holder.selectedItem.setImageResource(android.R.drawable.checkbox_on_background);
				else
					holder.selectedItem.setImageResource(android.R.drawable.checkbox_off_background);
				
			}catch(Exception ex) {Util.log("gpl error: " + ex.getMessage());}
			
			
			return view;
		}

		

		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long arg3) {
			
			if(lastSelected != null)
				lastSelected.selectedItem.setImageResource(android.R.drawable.checkbox_off_background);
			lastSelected = (GroupcellHolder)view.getTag();
			lastSelected.selectedItem.setImageResource(android.R.drawable.checkbox_on_background);
			lastSelectedPosition = position;
		}
	}
	
	private class GroupcellHolder
	{
		String groupId;
		TextView txtGroupName;
		TextView txtGroupCount;
		ImageView imgView;
		ImageView selectedItem;
		ImageView lockIcon;
		
	}
	
	private void getGroups()
	{
		mGroupProgress.setVisibility(View.VISIBLE);
		new Thread( new Runnable() {
			
			public void run() {
				
				String path = Insider.getUrlPath("group/self") + "?iToken=" + Util.mInsider.getAccessToken();
				
				final String results = ACacheUtil.getUrl(path, null, groupCache);
				Util.log(results);
					mHandler.post(new Runnable() {
						public void run() {
							try {
								mGroupProgress.setVisibility(View.GONE);
								mJsonGroups = new JSONArray(results);
								sortedGroups.clear();
								for(int i=0; i<mJsonGroups.length();i++)
								{
									sortedGroups.add(mJsonGroups.getJSONObject(i));
								}
								Collections.sort(sortedGroups,new Util.JsonNameComparator());
								groupCache = false;
								mGroupAdapter.notifyDataSetChanged();
							}catch(Exception ex) {}
						}
					});
					
				
			}
		}).start();
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
		Util.log("item clicked");
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK)
		{
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			/*Bundle params = new Bundle();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    photo.compress(Bitmap.CompressFormat.JPEG, 70, baos);
			params.putByteArray("file", baos.toByteArray());
			try {
				String response = AHttpUtil.openUrl(Insider.getUrlPath("msg/facebook/photo"), "POST", params);
				Util.log("response22: " + response);
			}catch(Exception ex) { Util.log("error22: " + ex.getMessage()); }*/
            mImgPost.setImageBitmap(photo);
		}
		//super.onActivityResult(requestCode, resultCode, data);
	}
}
