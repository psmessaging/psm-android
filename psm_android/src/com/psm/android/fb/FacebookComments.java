package com.psm.android.fb;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.Holders;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.Holders.FBNavHolder;
import com.psm.android.R.color;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.IUtil;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class FacebookComments extends Activity {

	
	private LayoutInflater mInflater;
	private Context mContext;
	private Handler mHandler;
	
	private View mHeader;
	private FBNavHolder mHolder;
	
	private ListView mList;
	private FBComAdapter mAdapter;
	
	private Button mBtnLike, mBtnSend;
	private EditText mEditComment, mStealthComment;
	private LinearLayout mProgress;
	private ToggleButton mToggle;
	
	private String mFbId;
	private JSONObject mJsonItem;
	private JSONArray mJsonAComments;
	private JSONArray mJsonALikes;
	
	private boolean mShowHeader = true;
	private boolean isLiked = false;
	
	private HashMap<String, String> mInsiderMap = new HashMap<String, String>();
	private JSONArray mJsonIds = new JSONArray();
	
	private HashMap<Integer, Boolean> mShowingWhat = new HashMap<Integer, Boolean>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_comment);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null) { finish(); return; }
		
		mContext = this;
		mInflater = getLayoutInflater();
		
		mHandler = new Handler();
		mBtnLike = (Button)findViewById(R.id.fb_comment_btnlike);
		mBtnSend = (Button)findViewById(R.id.fb_comment_btnsend);
		mEditComment = (EditText)findViewById(R.id.fb_comment_txt);
		mProgress = (LinearLayout)findViewById(R.id.fb_comment_progress);
		mStealthComment = (EditText)findViewById(R.id.fb_comment_txtstealth);
		mToggle = (ToggleButton)findViewById(R.id.fb_comment_tglstealth);
		if(!Util.mInsider.isLoggedIn())
			mToggle.setVisibility(View.GONE);
		
		mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
					mStealthComment.setVisibility(View.VISIBLE);
				else
					mStealthComment.setVisibility(View.GONE);
			}
		});
		mBtnSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				postComment();
			}
		});
		
		mBtnLike.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				likeItem();
			}
		});
		mList = (ListView)findViewById(R.id.fb_comment_list);
		
		setupHeader();
		mAdapter = new FBComAdapter();
		mList.setAdapter(mAdapter);
		
		mFbId = extras.getString("com.psm.android.id");
		if(extras.containsKey("com.psm.android.hideheader"))
		{
			mShowHeader = false;			
		}
		
		loadComments();
		
	}
	
	@Override
	protected void onResume() {
		new Handler().postDelayed(new Runnable() {
			
			public void run() {
				((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(mEditComment.getWindowToken(), 0);
			}
		}, 100);
		super.onResume();
	}
	
	private void loadComments()
	{
		mProgress.setVisibility(View.VISIBLE);
		mList.setVisibility(View.INVISIBLE);
		
		new Thread(new Runnable() {
			
			public void run() {
				Bundle params = new Bundle();
				params.putString("access_token", AFacebook.getAccessToken());
				String path = AFacebook.getFullUrl(mFbId, params);
				final String results = ACacheUtil.getUrl(path, true);
				mHandler.post(new Runnable() {
					public void run() {
						mList.setVisibility(View.VISIBLE);
						mProgress.setVisibility(View.INVISIBLE);
						try {
							mJsonItem = new JSONObject(results);
							fillHeader();
							if(mJsonItem.has("comments"))
								mJsonAComments = mJsonItem.getJSONObject("comments").getJSONArray("data");
							if(mJsonItem.has("likes"))
								mJsonALikes = mJsonItem.getJSONObject("likes").getJSONArray("data");
							collectIds();
							mAdapter.notifyDataSetChanged();
							Util.log(mJsonIds.toString());
							translateMessages(mJsonIds);
						}catch(Exception ex) { Util.log(ex.getMessage()); }
					}
				});
			}
		}).start();
		
		
		
		
		
	}
	
	private void setupHeader()
	{
		//Header of original post
		View view = getLayoutInflater().inflate(R.layout.facebook_cell_nav, null);
		Holders.FBNavHolder holder = new Holders.FBNavHolder();
		holder.imgUserIcon = (ImageView)view.findViewById(R.id.fb_cell_nav_uicon);
		holder.txtUserName = (TextView)view.findViewById(R.id.fb_cell_nav_uname);
		holder.txtUserName.setMovementMethod(LinkMovementMethod.getInstance());
		holder.txtText = (TextView)view.findViewById(R.id.fb_cell_nav_text);
		holder.layoutExtended = (LinearLayout)view.findViewById(R.id.fb_cell_nav_extendedlayout);
		holder.txtExtLink = (TextView)view.findViewById(R.id.fb_cell_nav_linkcaption);
		holder.txtExtText = (TextView)view.findViewById(R.id.fb_cell_nav_caption);
		holder.imgExtended = (ImageView)view.findViewById(R.id.fb_cell_nav_extendpic);
		holder.txtDate = (TextView)view.findViewById(R.id.fb_cell_nav_date);
		holder.imgVia = (ImageView)view.findViewById(R.id.fb_cell_nav_icon);
		
		holder.clLayout = (LinearLayout)view.findViewById(R.id.fb_cell_nav_likecomment);
		holder.btnComment = (Button)view.findViewById(R.id.fb_cell_nav_btncomment);
		holder.btnLike = (Button)view.findViewById(R.id.fb_cell_nav_btnlike);
		holder.txtCommentCount = (TextView)view.findViewById(R.id.fb_cell_nav_comments);
		holder.txtLikeCount = (TextView)view.findViewById(R.id.fb_cell_nav_likes);
		
		mHeader = view;
		mHeader.setTag(holder);
	}
	
	private void fillHeader()
	{
		if(mJsonItem != null)
		{
			try {
				Holders.FBNavHolder holder = (Holders.FBNavHolder)mHeader.getTag();
				JSONObject object = mJsonItem;//mJsonData.getJSONObject(position-(mShowHeader ? 1:0));
				final JSONObject user = object.getJSONObject("from");
				JSONObject application = object.has("application") ? object.getJSONObject("application") : null ;
				holder.layoutExtended.setVisibility(View.GONE);
				String normalizedId = IUtil.normalizeFacebookId(object.getString("id"));
				
				if(mInsiderMap.containsKey(normalizedId))
				{
					holder.isInsider = true;
					object.put("message", mInsiderMap.get(normalizedId));
					holder.txtText.setTextColor(getResources().getColor(R.color.stealth_color));
				}
				else
				{
					
					holder.isInsider = false;
					holder.txtText.setTextColor(Color.WHITE);
				}
				
				//TODO: change to use facebook object instead of insider object
				if(user.getString("id").compareTo(Util.mInsider.getFacebookId()) == 0)
					holder.isOwner = true;
				else
					holder.isOwner = false;
				
				holder.txtUserName.setText(user.getString("name"));
				holder.txtUserName.setTag(user.getString("id"));
				
				//Place holder images
				holder.imgUserIcon.setImageResource(R.drawable.icon_ph);
				holder.imgExtended.setImageResource(R.drawable.icon_ph);
				
				holder.btnLike.setTag(object.getString("id"));
				holder.btnComment.setTag(object.getString("id"));
				int commentcount = object.has("comments") ? object.getJSONObject("comments").getInt("count") : 0;
				int likecount = object.has("likes") ? object.getJSONObject("likes").getInt("count") : 0;
				if(commentcount > 0 || likecount > 0)
				{
					//holder.clLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
					
					if(commentcount > 0)
					{
						holder.txtCommentCount.setText("c: " + String.valueOf(commentcount));
						holder.txtCommentCount.setVisibility(View.VISIBLE);
					}
					else
					{
						holder.txtCommentCount.setVisibility(View.GONE);
						holder.txtCommentCount.setText("");
					}
					
					if(likecount > 0)
					{
						holder.txtLikeCount.setVisibility(View.VISIBLE);
						holder.txtLikeCount.setText("l: " + String.valueOf(likecount));
					}
					else
					{
						holder.txtLikeCount.setVisibility(View.GONE);
						holder.txtLikeCount.setText("");
					}
				}
				else
				{
					//holder.clLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					holder.txtCommentCount.setVisibility(View.GONE);
					holder.txtCommentCount.setText("");
					holder.txtLikeCount.setVisibility(View.GONE);
					holder.txtLikeCount.setText("");
				}
				/*holder.txtUserName.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						try {
							String newpath = user.getString("id") + "/feed";
							if(mCurrentPage != null)
								if(newpath.compareToIgnoreCase(mCurrentPage) == 0) return;
							navigateToPath(newpath);
							addUserHeader();
							fillHeader(user.getString("id"));
							return;
						}catch(Exception ex) {}
					}
				});*/
				
				if(object.has("message") && object.has("story"))
				{
					holder.txtUserName.setVisibility(View.VISIBLE);
					holder.txtText.setText(object.getString("message") + " -- " + object.getString("story"));
					holder.clLayout.setVisibility(View.VISIBLE);
				}
				else
				{
					if(object.has("message"))
					{
						holder.txtUserName.setVisibility(View.VISIBLE);
						holder.txtText.setText(object.getString("message"));
						holder.clLayout.setVisibility(View.VISIBLE);
					}
					else if(object.has("story"))
					{
						holder.txtUserName.setVisibility(View.GONE);
						holder.txtText.setText(object.getString("story"));
						if(commentcount > 0)
							holder.clLayout.setVisibility(View.VISIBLE);
						else
							holder.clLayout.setVisibility(View.GONE);
					}
				}
				
				if(object.has("story_tags") && object.has("story"))
				{
					JSONObject tags = object.getJSONObject("story_tags");
					holder.txtText.setMovementMethod(LinkMovementMethod.getInstance());
					for (Iterator iterator = tags.keys(); iterator
							.hasNext();) {
						String string = (String) iterator.next();
						JSONArray array = tags.getJSONArray(string);
						JSONObject obj = array.getJSONObject(0);
						final String zid = obj.getString("id");
						boolean hasboth = object.has("message");
						int offset = hasboth ? holder.txtText.getText().length()-object.getString("story").length() : obj.getInt("offset");
						/*Util.clickify(holder.txtText, offset, obj.getInt("length"),
									new Util.ClickSpan.OnClickListener() {
										public void onClick() {
											try {
												Util.log(zid);
												mHandler.post(new Runnable() {
													public void run() {
														navigateToPath(zid + "/feed");
														addUserHeader();
														fillHeader(zid);														
													}
												});
												
											}catch(Exception  ex) {}
										}
									});
						*/
					}
					
					
				}
				try {
					Util.removeUnderlines((Spannable) holder.txtText.getText());
				}catch(Exception ex){}
				if(application != null)
				{
					String cdate = AFacebook.formatTime(object.getString("created_time")) + " via " + 
											application.getString("name");
					holder.txtDate.setText(cdate);
					String imgPath = "https://graph.facebook.com/" + application.getString("id") + "/picture";
					//loadPicture(imgPath, holder.imgVia);
				}
				else
				{
					holder.imgVia.setImageBitmap(null);
					holder.txtDate.setText(AFacebook.formatTime(object.getString("created_time")));
				}
				Util.log("type:" + object.getString("type"));
				//Type
				if(object.getString("type").compareToIgnoreCase("link") == 0
						|| object.getString("type").compareToIgnoreCase("video") == 0)
				{
					holder.layoutExtended.setVisibility(View.VISIBLE);
					
					if(object.has("name"))
					{
						//holder.txtExtLink.setText(object.getString("name"));
						holder.txtExtLink.setText(Html.fromHtml("<a href=\"" + 
								object.getString("link") + "\">" + 
								object.getString("name") + "</a>"));
						
						holder.txtExtLink.setMovementMethod(LinkMovementMethod.getInstance());
						Util.removeUnderlines((Spannable) holder.txtExtLink.getText());
					}
					else
						holder.txtExtLink.setText("");
					
					if(object.has("caption"))
						holder.txtExtText.setText(object.getString("caption"));
					else
						holder.txtExtText.setText("");
					
					if(object.has("picture"))
					{
						loadPicture(object.getString("picture"), holder.imgExtended);
					}
				}
				else if(object.getString("type").compareToIgnoreCase("photo") == 0)
				{
					holder.layoutExtended.setVisibility(View.VISIBLE);
					loadPicture(object.getString("picture"), holder.imgExtended);
					holder.txtExtText.setText("");
					holder.txtExtLink.setText("");
				}
				else if(object.getString("type").compareToIgnoreCase("status") == 0)
				{
					holder.layoutExtended.setVisibility(View.GONE);
				}
				
				if(object.has("place") && object.getString("type").compareToIgnoreCase("checkin") == 0)
				{
					holder.layoutExtended.setVisibility(View.VISIBLE);
					JSONObject place = object.getJSONObject("place");
					String txt = holder.txtText.getText().toString();
					holder.txtText.setText(txt + " -- at " + place.getString("name"));
					loadPicture(object.getString("picture"), holder.imgExtended);
					
					holder.txtExtLink.setText(object.getString("caption"));
					holder.txtExtText.setText("");
					
				}
				else if(object.has("place") && object.getString("type").compareToIgnoreCase("status") == 0)
				{
					holder.layoutExtended.setVisibility(View.GONE);
					JSONObject place = object.getJSONObject("place");
					String txt = holder.txtText.getText().toString();
					holder.txtText.setText(txt + " -- at " + place.getString("name"));
				}
				holder.clLayout.setVisibility(View.GONE);
				loadPicture("https://graph.facebook.com/" + user.getString("id") + "/picture", holder.imgUserIcon);
				//getPicture(user.getString("id"), holder.imgUserIcon);
				
			}catch(Exception ex) { Util.log("error8: " + ex.getMessage()); }
		}
	}
	
	private void loadPicture(final String url, final ImageView imageview)
	{
		
		new Thread(new Runnable() {
			public void run() {
				final Bitmap bitmap = ACacheUtil.getBitmap(url);
				mHandler.post(new Runnable() {
					public void run() {
						imageview.setImageBitmap(bitmap);
					}
				});
			}
		}).start();
		
		
	}
	
	private class FBComAdapter extends BaseAdapter
	{

		public int getCount() {
			if(mJsonAComments == null) return 0+(mShowHeader ? 1:0);
			return mJsonAComments.length()+(mShowHeader ? 1:0);
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			if(position == 0 && mShowHeader)
				return mHeader;
			
			if(view != null && mShowHeader)
			{
				if(view == mHeader)
					view = null;
			}
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.facebook_cell_comment, null);
				FCHolder holder = new FCHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.fb_cell_comment_icon);
				holder.txtName = (TextView)view.findViewById(R.id.fb_cell_comment_name);
				holder.txtDate = (TextView)view.findViewById(R.id.fb_cell_comment_date);
				holder.txtComment = (TextView)view.findViewById(R.id.fb_cell_comment_txt);
				view.setTag(holder);
			}
			boolean showRegular = false;
			
			if(mShowingWhat.containsKey(position))
				showRegular = mShowingWhat.get(position);
			
			FCHolder holder = (FCHolder)view.getTag();
			try {
				
				JSONObject comment = mJsonAComments.getJSONObject(position-(mShowHeader ? 1:0));
				JSONObject from = comment.getJSONObject("from");
				loadImage(from.getString("id"), holder.imgIcon);
				holder.txtName.setText(from.getString("name"));
				holder.txtComment.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						boolean showRegular = false;
						
						if(mShowingWhat.containsKey(position))
							showRegular = mShowingWhat.get(position);
						mShowingWhat.clear();
						if(showRegular == false)
						{
							mShowingWhat.put(position, true);
						}
						else
						{
							mShowingWhat.put(position, false);
						}
						notifyDataSetChanged();
					}
				});
				Util.log("load comment: " + comment.getString("id"));
				String id = IUtil.normalizeFacebookId(comment.getString("id"));
				if(mInsiderMap.containsKey(id) && !showRegular)
				{
					holder.txtComment.setTextColor(getResources().getColor(R.color.stealth_color));
					JSONObject object = new JSONObject(mInsiderMap.get(id));
					holder.txtComment.setText(object.getString("message"));
					//holder.txtComment.setText(mInsiderMap.get(id));
				}
				else
				{
					holder.txtComment.setTextColor(Color.WHITE);
					holder.txtComment.setText(comment.getString("message"));
				}
				holder.txtDate.setText(AFacebook.formatTime(comment.getString("created_time")));
				
			}catch(Exception ex) {}
			return view;
		}
		
	}
	
	private void postComment()
	{
		((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
		.hideSoftInputFromWindow(mEditComment.getWindowToken(), 0);
		((InputMethodManager)mContext.getSystemService(INPUT_METHOD_SERVICE))
		.hideSoftInputFromWindow(mStealthComment.getWindowToken(), 0);
		
		
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Posting Comment...");
		dialog.show();
		
		final String url = AFacebook.getFullUrl(mFbId + "/comments", null);
		new Thread(new Runnable() {
			public void run() {
				String comment = mEditComment.getText().toString();
				Bundle params = new Bundle();
				params.putString("message", comment);
				try {
					mHandler.post(new Runnable() {
						public void run() {
							dialog.setMessage("Posting to insid3r...");
						}
					});
					
					String results = AHttpUtil.openUrl(url, "POST", params);
					String id = new JSONObject(results).getString("id");
					postStealthComment(id);
					dialog.dismiss();
					mHandler.post(new Runnable() {
						public void run() {
							loadComments();
							mEditComment.setText("");
							mStealthComment.setText("");
							mToggle.setChecked(false);
						}
					});
				}catch(Exception ex) {Util.log(ex.getMessage());}
				
			}
		}).start();
		
	}
	
	
	
	private void likeItem()
	{
		try {
			Bundle params = new Bundle();
			String url = AFacebook.getFullUrl(mFbId + "/likes", null);
			if(!isLiked)
			{
				String results = AHttpUtil.openUrl(url, "POST", params);
				Util.log("like");
				mBtnLike.setBackgroundColor(Color.LTGRAY);
				isLiked = true;
			}
			else
			{
				Util.log("unlike");
				String results = AHttpUtil.deleteUrl(url, params);
				Util.log(results);
				mBtnLike.setBackgroundColor(Color.DKGRAY);
				isLiked = false;
				
			}
			
		}catch(Exception ex) {Util.log(ex.getMessage());}
	}
	
	private void loadImage(final String id, final ImageView imageView)
	{
		new Thread(new Runnable() {
			
			public void run() {
				String path = id + "/picture"; 
				Bundle bundle = new Bundle();
				bundle.putString("access_token", AFacebook.getAccessToken());
				final Bitmap bitmap = ACacheUtil.getBitmap(AFacebook.getFullUrl(path, bundle));
				mHandler.post(new Runnable() {
					public void run() {
						imageView.setImageBitmap(bitmap);
					}
				});
			}
		}).start();
		
		
	}
	
	
	private class FCHolder
	{
		ImageView imgIcon;
		TextView txtName;
		TextView txtDate;
		TextView txtComment;
		
	}
	
	/*
	 * 
	 * Stealth Methods
	 * 
	 * 
	 */
	private void postStealthComment(String id)
	{
		Util.log("Post Stealth Comment");
		if(mStealthComment.getText().length() > 0 && mToggle.isChecked())
		{
			//Has stealth message post it to private server
			String nid = IUtil.normalizeFacebookId(id);
			Util.log("normed comment: " + nid);
			Bundle params = new Bundle();
			params.putString("iToken", Util.mInsider.getAccessToken());
			params.putString("id", nid);
			params.putString("message", URLEncoder.encode(mStealthComment.getText().toString()));
			try {
				String results = AHttpUtil.simplePost(Insider.I_BASE_PATH + "msg/facebook", params);
				Util.log("Results: " + results);
			}catch(Exception ex){Util.log("Error:22 : " + ex.getLocalizedMessage());}

		}
	}
	
	private void collectIds()
	{
		ArrayList<String> strings = IUtil.getBasicIds(mJsonAComments);
		Util.log(strings.size());
		mJsonIds = new JSONArray();
		for(String string : strings)
		{
			String key = IUtil.normalizeFacebookId(string);
			if(!mInsiderMap.containsKey(key))
				mJsonIds.put(key);
		}
	}
	
	private void translateMessages(JSONArray array)
	{
		Util.log("translate messages: " + array.toString());
		if(array.length() > 0)
		{
			Bundle params = new Bundle();
			params.putString("ids", array.toString());
			params.putString("iToken", Util.mInsider.getAccessToken());
			try {
				String response = AHttpUtil.simplePost(Insider.I_BASE_PATH + "msg/facebook/batch", params);
				Util.log(response);
				JSONObject obj = new JSONObject(response).getJSONObject("data");
				Util.log(obj.toString());
				for (Iterator iterator = obj.keys(); iterator
						.hasNext();) {
					String string = (String) iterator.next();
					Util.log(string);
					mInsiderMap.put(string, obj.getString(string));
				}
			}catch(Exception ex) {Util.log(ex.getMessage());}
			Util.log("Map Size: " + mInsiderMap.size());
			mHandler.post(new Runnable() {
				public void run() {

					mAdapter.notifyDataSetChanged();
				}
			});
			
		}
	}
}
