package com.psm.android.fb;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.XMLReader;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.Holders;
import com.psm.android.PullToRefreshListView;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.Holders.FBNavHolder;
import com.psm.android.PullToRefreshListView.OnRefreshListener;
import com.psm.android.R.color;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.android.Util.ClickSpan;
import com.psm.util.IUtil;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

public class FacebookNav extends Activity implements OnItemLongClickListener, OnScrollListener {

	private LayoutInflater mInflater;
	private Handler mHandler;
	private Context mContext;
	private View mHeader;
	private View mFooter;
	
	private HeaderHolder mHolder;
	
	
	private PullToRefreshListView mList;
	private FBNavAdapter mAdapter;
	
	private JSONArray mJsonData;
	
	private final static int VIEW_TYPE_EXTENDED = 5;
	private final static int VIEW_TYPE_BASIC	= 6;
	private final static int REQUEST_STATUS		= 7;
	private final static int REQUEST_FRIENDS	= 8;
	private final static int REQUEST_ALBUMS		= 10;
	
	private HashMap<String, Bitmap> mUserIcons = new HashMap<String, Bitmap>();
	private ArrayList<String> mHistory = new ArrayList<String>();
	private String mCurrentPage = null;
	private String mCurrentId = null;
	
	private LinearLayout mProgress;
	private Thread mNavThread;
	private boolean mShowHeader = true;
	private boolean mPreLoad = false;
	
	private HashMap<Integer, Boolean> mShowingWhat = new HashMap<Integer, Boolean>();
	
	private HashMap<String, String> mInsiderMap = new HashMap<String, String>();
	private ArrayList<String> mCheckedIds = new ArrayList<String>();
	private boolean isScrolling = false;
	
	private JSONObject mJsonPaging = null;
	
	//Gesture Items
	Holders.FBNavHolder mLastHolder = null;
	private int mGesturePosition = 0;
    private int scrollOffset = 0;
    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;
    
    private BroadcastReceiver mReceiver;
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_nav);
		mInflater = getLayoutInflater();
		mHandler = new Handler();
		mContext = this;
		mProgress = (LinearLayout)findViewById(R.id.fb_nav_progress);
		
		mHeader = mInflater.inflate(R.layout.facebook_header_user, null);
		mHolder = new HeaderHolder();
		mHolder.txtName = (TextView)mHeader.findViewById(R.id.fb_head_name);
		mHolder.txtOther = (TextView)mHeader.findViewById(R.id.fb_head_other);
		mHolder.imgIcon = (ImageView)mHeader.findViewById(R.id.fb_head_icon);
		mHolder.btnFriend = (Button)mHeader.findViewById(R.id.fb_head_btnfriends);
		mHolder.btnPost = (Button)mHeader.findViewById(R.id.fb_head_btnwrite);
		mHolder.btnPhoto = (Button)mHeader.findViewById(R.id.fb_head_btnphoto);
		mHeader.setTag(mHolder); 
		
		
		
		mList = (PullToRefreshListView)findViewById(R.id.fb_nav_list);
		
		gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        
        mList.setOnTouchListener(gestureListener);
        mList.setOnScrollListener(this);
		
		mList.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		mList.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
			public void onRefresh() {
				mShowingWhat.clear();
				navigateToPath(mCurrentPage, true, true, true);
				addUserHeader();
				if(mList.getHeaderViewsCount() > 0)
					fillHeader(mCurrentId, true);
			}
		});
		
		
		mAdapter = new FBNavAdapter();
		addFooter(mList);
		mList.setAdapter(mAdapter);
		getFeed();
	}
	
	private void setupReceiver()
	{
		if(mReceiver == null)
		{
			mReceiver = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent data) {
					Util.log("receive message");
					if(data.getAction().compareTo("com.psm.android.REFRESH") == 0)
					{
						Util.log("Refresh");
						navigateToPath(mCurrentPage, true, true, true);
					}
					else if(data.getAction().compareTo("com.psm.android.NAVFRIEND") == 0)
					{
						String friendId = data.getExtras().getString("com.psm.android.fbid");
						navigateToPath(friendId + "/feed");
					}
					else if(data.getAction().compareTo("com.psm.android.NAVIGATE") == 0)
					{
						String path = data.getExtras().getString("com.psm.android.navpath");
						navigateToPath(path);
					}
				}
			};
		}
		
		
	}
	
	@Override
	protected void onResume() {
		setupReceiver();
		if(mReceiver != null)
		{
			IntentFilter filter = new IntentFilter();
			filter.addAction("com.psm.android.REFRESH");
			filter.addAction("com.psm.android.NAVFRIEND");
			filter.addAction("com.psm.android.NAVIGATE");
			Util.log("Register Receiver");
			registerReceiver(mReceiver, filter);
		}
		super.onResume();
		
	}
	
	@Override
	protected void onDestroy() {
		if(isFinishing())
			unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		return super.onKeyDown(keyCode, event);
		
	}
	
	@Override
	public void onBackPressed() {
		if(mHistory.size() == 0)
		{
			super.onBackPressed();
		}
		else
		{
			navigateToPath(mHistory.get(0), true);
			mHistory.remove(0);
		}

	}
	
	private void addUserHeader()
	{
		if(mList.getHeaderViewsCount() > 0) return;
		mHandler.post(new Runnable() {
			public void run() {
				//mList.setAdapter(null);
				//mList.addHeaderView(mHeader);
				//mList.setAdapter(mAdapter);
				mShowHeader = true;
				mAdapter.notifyDataSetChanged();
				
			}
		});
		
		
	}
	
	private void removeUserHeader()
	{
		//mList.removeHeaderView(mHeader);
		mShowHeader = false;
		mAdapter.notifyDataSetChanged();
	}
	
	private void fillHeader(final String fbuserId, boolean forceReload)
	{
		
		String results = ACacheUtil.getUrl(AFacebook.getFullUrl(fbuserId, null), forceReload);
		Util.log(results);
		try {
			JSONObject obj = new JSONObject(results);
			mHolder.imgIcon.setImageResource(R.drawable.facebookuser);
			mCurrentId = fbuserId;
			loadPicture(AFacebook.BASE_PATH + obj.getString("id") + "/picture", mHolder.imgIcon);
			mHolder.txtName.setText(obj.getString("name"));
			mHolder.txtOther.setText("");
			StringBuilder sb = new StringBuilder();
			
			if(obj.has("work"))
			{
				try {
					String wname = obj.getJSONArray("work").getJSONObject(0).getJSONObject("employer").getString("name");
					sb.append( "Works at " + wname + "\n");
					
				}catch(Exception ex){}
			}
			
			if(obj.has("education"))
			{
				try {
					JSONArray array = obj.getJSONArray("education");
					JSONObject school = array.getJSONObject(array.length()-1);
					sb.append("Studied at " + school.getJSONObject("school").getString("name"));
				}catch(Exception ex){}
				
			}
			
			mHolder.txtOther.setText( sb.toString() );
			
			mHolder.btnPost.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(FacebookNav.this,FacebookStatus.class);
					intent.putExtra("com.psm.android.id", fbuserId);
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivityForResult(intent, REQUEST_STATUS);
				}
			});
			
			mHolder.btnFriend.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					Intent intent = new Intent(FacebookNav.this,FacebookFriends.class);
					intent.putExtra("com.psm.android.id", fbuserId);
					intent.putExtra("com.psm.android.donav", true);
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivityForResult(intent, REQUEST_FRIENDS);
				}
			});
			
			mHolder.btnPhoto.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					Intent intent = new Intent(FacebookNav.this,FacebookAlbums.class);
					intent.putExtra("com.psm.android.id", fbuserId);
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivityForResult(intent, REQUEST_ALBUMS);
				}
			});
		}catch(Exception ex){}
	}
	
	
	
	private void addFooter(ListView list)
	{
		if(mFooter == null)
		{
			View view = mInflater.inflate(R.layout.cell_load_more, null);
			final FooterHolder holder = new FooterHolder();
			holder.mLoadmore = (Button)view.findViewById(R.id.cell_loadmore_btn);
			holder.mProgress = (LinearLayout)view.findViewById(R.id.cell_loadmore_progress);
			view.setTag(holder);
			holder.mLoadmore.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View arg0) {
					holder.mProgress.setVisibility(View.VISIBLE);
					holder.mLoadmore.setVisibility(View.INVISIBLE);
					new Thread(new Runnable() {
						
						public void run() {		
							try{
								String nextUrl = mJsonPaging.getString("next");
								final String results = ACacheUtil.getUrl(nextUrl, true);
								mHandler.post(new Runnable() {
									
									public void run() {
										try {
											JSONObject jsonResult = new JSONObject(results);
											JSONArray msgs = jsonResult.getJSONArray("data");
											for(int x =0;x<msgs.length();x++)
											{
												mJsonData.put(msgs.getJSONObject(x));
											}
											if(msgs.length() > 0)
												collectIds();
											//mAdapter.notifyDataSetChanged();
											if(jsonResult.has("paging"))
											{
												mJsonPaging = new JSONObject(results).getJSONObject("paging");
												holder.mProgress.setVisibility(View.INVISIBLE);
												holder.mLoadmore.setVisibility(View.VISIBLE);
											}
											else
											{
												holder.mProgress.setVisibility(View.INVISIBLE);
												holder.mLoadmore.setVisibility(View.INVISIBLE);
											}
										}catch(Exception ex) {
											holder.mProgress.setVisibility(View.INVISIBLE);
											holder.mLoadmore.setVisibility(View.VISIBLE);
										}
									}
								});
							}catch(Exception ex ) {}}}).start();
					
				}
			});
			list.addFooterView(view);
			mFooter = view;
		}
	}
	
	
	private void getFeed()
	{
		navigateToPath("me/feed");
	}
	
	
	private void clickPhoto(View view)
	{
		String photoid = (String)view.getTag();
	}
	
	private void navigateToPath(final String fbpath)
	{
		navigateToPath(fbpath, false);
	}
	
	private void navigateToPath(final String fbpath, final boolean noHistory)
	{
		navigateToPath(fbpath, noHistory, false, false);
	}
	
	private void navigateToPath(final String fbpath, final boolean noHistory, final boolean ignoreSame, final boolean forceRefresh)
	{
		if(mNavThread != null)
			if(mNavThread.isAlive()) return;
		
		if(mCurrentPage != null && !ignoreSame)
			if(fbpath.compareToIgnoreCase(mCurrentPage) == 0) return;
		
		mProgress.setVisibility(View.VISIBLE);
		mList.setVisibility(View.GONE);
		mCheckedIds.clear();
		FooterHolder holder = (FooterHolder)mFooter.getTag();
		holder.mProgress.setVisibility(View.INVISIBLE);
		holder.mLoadmore.setVisibility(View.VISIBLE);
		
		mNavThread = new Thread(new Runnable() {
			public void run() {
				mPreLoad = true;
				if(mCurrentPage != null)
				{
					//if(mHistory.contains(fbpath))
					mHistory.remove(fbpath);
					if(!noHistory)
						mHistory.add(0,mCurrentPage);
				}
				mCurrentPage = fbpath;
				if(mCurrentPage.contains("feed") == true)
				{
					String[] str = mCurrentPage.split("[/]", 2);
					fillHeader(str[0], false);
				}
				else
					mList.removeHeaderView(mHeader);
					
				Bundle params = new Bundle();
				params.putString("access_token", AFacebook.getAccessToken());
				String path = AFacebook.getFullUrl(fbpath, params);
				final String results = ACacheUtil.getUrl(path, forceRefresh);
				mHandler.post(new Runnable() {
					public void run() {
						try {
							mJsonData = new JSONObject(results).getJSONArray("data");
							mJsonPaging = new JSONObject(results).getJSONObject("paging");
							collectIds();
						}catch(Exception ex) {mJsonData = null;}
						mPreLoad = false;
						mList.setVisibility(View.VISIBLE);
						mProgress.setVisibility(View.GONE);
						mAdapter.notifyDataSetChanged();
						if(mList.getCount() > 0)
							mList.setSelection(1);
						mList.setSelected(false);
						((PullToRefreshListView)mList).onRefreshComplete(new Date().toLocaleString());
					}});	
			}
		});
		mNavThread.start();
	}
	
	
	private class FBNavAdapter extends BaseAdapter implements OnClickListener
	{

		public int getCount() {
			if(mJsonData == null) return 0;
			return mJsonData.length() + (mShowHeader ? 1:0); 
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}
		
		

		public View getView(final int position, View convertView, ViewGroup viewGroup) {
			if(mShowHeader && position == 0)
			{
				return mHeader;
			}
			
			View view = convertView;
			if(view != null && mShowHeader)
			{
				if(view == mHeader)
					view = null;
			}
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.facebook_cell_nav, null);
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
				holder.btnComment.setOnClickListener(this);
				holder.btnLike.setOnClickListener(this);
				
				holder.txtCommentCount = (TextView)view.findViewById(R.id.fb_cell_nav_comments);
				holder.txtLikeCount = (TextView)view.findViewById(R.id.fb_cell_nav_likes);
				holder.iconComment = (ImageView)view.findViewById(R.id.fb_cell_nav_iconcm);
				holder.iconLike = (ImageView)view.findViewById(R.id.fb_cell_nav_iconlk);
				
				holder.btnRemove = (ImageButton)view.findViewById(R.id.fb_cell_nav_del);
				//holder.gesture = (GestureOverlayView)view.findViewById(R.id.fb_cell_nav_gesture);
				/*view.setOnClickListener(new View.OnClickListener() {
					public void onClick(View arg0) {
						Util.log("view click");
					}
				});*/
				
				view.setTag(holder);
			}
			viewGroup.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			
			Holders.FBNavHolder holder = (Holders.FBNavHolder)view.getTag();
			
			holder.txtText.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					boolean showRegular = false;
					
					if(mShowingWhat.containsKey(position))
						showRegular = mShowingWhat.get(position);
					//mShowingWhat.clear();
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
			try {
				//TODO: define different types of messages, video, link, etc...
				JSONObject object = mJsonData.getJSONObject(position-(mShowHeader ? 1:0));
				final JSONObject user = object.getJSONObject("from");
				holder.ownerId = user.getString("id");
				holder.txtText.setText("");
				JSONObject application = object.has("application") ? object.getJSONObject("application") : null ;
				
				holder.layoutExtended.setVisibility(View.GONE);
				String normalizedId = IUtil.normalizeFacebookId(object.getString("id"));
				JSONObject insiderObject = null;
				boolean showRegular = false;
				final String fbPostId = object.getString("id");
				if(mShowingWhat.containsKey(position))
					showRegular = mShowingWhat.get(position);
				
				if(mInsiderMap.containsKey(normalizedId) && !showRegular)
				{
					holder.isInsider = true;
					insiderObject = new JSONObject(mInsiderMap.get(normalizedId));
					object.put("imessage", insiderObject.getString("message"));
					holder.txtText.setTextColor(getResources().getColor(R.color.stealth_color));
					holder.txtText.setText(object.getString("imessage"));
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
				
				final boolean istheOwner = holder.isOwner;
				final String fbId = object.getString("id");//insiderObject.getString("id");
				final String imsgId = holder.isInsider ? insiderObject.getString("id"):null;
				
				holder.btnRemove.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						if(istheOwner)
						{	
							if(imsgId != null)
								deletePost(fbId, imsgId);
							else
								deletePost(fbId, null);
						}
					}
				});

				holder.txtUserName.setText(user.getString("name"));
				holder.txtUserName.setTag(user.getString("id"));
				
				//Place holder images
				if(!isScrolling)
				{
					holder.imgUserIcon.setImageResource(R.drawable.facebookuser);
					//holder.imgExtended.setImageResource(R.drawable.icon_ph);
				}
				
				holder.btnLike.setTag(object.getString("id"));
				holder.btnComment.setTag(object.getString("id"));
				int commentcount = object.has("comments") ? object.getJSONObject("comments").getInt("count") : 0;
				int likecount = object.has("likes") ? object.getJSONObject("likes").getInt("count") : 0;
				if(commentcount > 0 || likecount > 0)
				{
					//holder.clLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
					
					if(commentcount > 0)
					{
						holder.txtCommentCount.setText(String.valueOf(commentcount));
						holder.txtCommentCount.setVisibility(View.VISIBLE);
						holder.iconComment.setVisibility(View.VISIBLE);
					}
					else
					{
						holder.iconComment.setVisibility(View.GONE);
						holder.txtCommentCount.setVisibility(View.GONE);
						holder.txtCommentCount.setText("");
					}
					
					if(likecount > 0)
					{
						holder.txtLikeCount.setVisibility(View.VISIBLE);
						holder.iconLike.setVisibility(View.VISIBLE);
						holder.txtLikeCount.setText(String.valueOf(likecount));
					}
					else
					{
						holder.txtLikeCount.setVisibility(View.GONE);
						holder.iconLike.setVisibility(View.GONE);
						holder.txtLikeCount.setText("");
					}
				}
				else
				{
					//holder.clLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					holder.txtCommentCount.setVisibility(View.GONE);
					holder.iconLike.setVisibility(View.GONE);
					holder.iconComment.setVisibility(View.GONE);
					holder.txtLikeCount.setVisibility(View.GONE);
					holder.txtCommentCount.setText("");
					holder.txtLikeCount.setText("");
				}
				holder.txtUserName.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						try {
							String newpath = user.getString("id") + "/feed";
							if(mCurrentPage != null)
								if(newpath.compareToIgnoreCase(mCurrentPage) == 0) return;
							navigateToPath(newpath);
							addUserHeader();
							fillHeader(user.getString("id"), false);
							return;
						}catch(Exception ex) {}
					}
				});
				
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
						if(insiderObject != null && !showRegular)
						{
							holder.txtUserName.setVisibility(View.VISIBLE);
							holder.txtText.setText(object.getString("imessage"));
							holder.clLayout.setVisibility(View.VISIBLE);
						}
						else
						{
							holder.txtUserName.setVisibility(View.VISIBLE);
							holder.txtText.setText(object.getString("message"));
							holder.clLayout.setVisibility(View.VISIBLE);
						}
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
						Util.clickify(holder.txtText, offset, obj.getInt("length"),
									new Util.ClickSpan.OnClickListener() {
										public void onClick() {
											try {
												Util.log(zid);
												mHandler.post(new Runnable() {
													public void run() {
														navigateToPath(zid + "/feed");
														addUserHeader();
														fillHeader(zid, false);														
													}
												});
												
											}catch(Exception  ex) {}
										}
									});
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
					holder.imgExtended.setImageBitmap(null);
				}
				
				if(object.has("place") && object.getString("type").compareToIgnoreCase("checkin") == 0)
				{
					if(holder.isInsider && !showRegular)
					{
						holder.layoutExtended.setVisibility(View.GONE);
						if(insiderObject != null)
						{
							try {
								String results = ACacheUtil.getUrl(AFacebook.getFullUrl(insiderObject.getString("privateVenueId"), null));
								Util.log(results);
								holder.layoutExtended.setVisibility(View.VISIBLE);
								JSONObject place = new JSONObject(results);
								String txt = holder.txtText.getText().toString();
								holder.txtText.setText(txt + " -- at " + place.getString("name"));
								loadPicture(place.getString("picture"), holder.imgExtended);
								holder.txtExtLink.setTextColor(getResources().getColor(R.color.stealth_color));
								holder.txtExtLink.setText(user.getString("name").split("[ ]")[0] + " checked in at " + place.getString("name"));
								holder.txtExtText.setText("");
							}catch(Exception ex) {}
						}
					}
					else
					{
						holder.layoutExtended.setVisibility(View.VISIBLE);
						JSONObject place = object.getJSONObject("place");
						String txt = holder.txtText.getText().toString();
						holder.txtText.setText(txt + " -- at " + place.getString("name"));
						loadPicture(object.getString("picture"), holder.imgExtended);
						holder.txtExtLink.setTextColor(Color.WHITE);
						holder.txtExtLink.setText(object.getString("caption"));
						holder.txtExtText.setText("");
					}
					
				}
				else if(object.has("place") && object.getString("type").compareToIgnoreCase("status") == 0)
				{
					holder.layoutExtended.setVisibility(View.GONE);
					if(holder.isInsider && !showRegular)
					{
						if(insiderObject != null)
						{
							try {
								String results = ACacheUtil.getUrl(AFacebook.getFullUrl(insiderObject.getString("privateVenueId"), null));
								JSONObject iplace = new JSONObject(results);
								String txt = holder.txtText.getText().toString();
								holder.txtText.setText(txt + " -- at " + iplace.getString("name"));
							}catch(Exception ex) {}
							//{"id":"144864878879787","name":"Bristol Park Urgent Care","picture":"https:\/\/fbcdn-profile-a.akamaihd.net\/static-ak\/rsrc.php\/v2\/yc\/r\/YvAeQa2pawy.png","link":"http:\/\/www.facebook.com\/pages\/Bristol-Park-Urgent-Care\/144864878879787","likes":2,"category":"Local business","is_published":true,"is_community_page":true,"location":{"city":"Fountain Valley","state":"CA","country":"United States","latitude":33.715546774408,"longitude":-117.93017518757},"checkins":355,"talking_about_count":5}
	
							//Util.log(results);
						}
					}
					else
					{
						JSONObject place = object.getJSONObject("place");
						String txt = holder.txtText.getText().toString();
						holder.txtText.setText(txt + " -- at " + place.getString("name"));
					}
				}
				
				getPicture(user.getString("id"), holder.imgUserIcon);
				
			}catch(Exception ex) { Util.log("error8: " + ex.getMessage()); }
			
			return view;
		}

		public void onClick(View v) {
			if(v.getTag() == null) return;
			
			Intent intent = new Intent(FacebookNav.this, FacebookComments.class);
			intent.putExtra("com.psm.android.id", (String)v.getTag());
			startActivity(intent);
			
		}
		
	}
	
	
	
	private class HeaderHolder
	{
		ImageView imgIcon;
		TextView txtName;
		TextView txtOther;
		Button btnFriend;
		Button btnPost;
		Button btnPhoto;
	}
	
	private class FooterHolder
	{
		Button mLoadmore;
		LinearLayout mProgress;
	}
	private void getPicture(final String id, final ImageView imageview)
	{
		if(mUserIcons.containsKey(id))
		{
			imageview.setImageBitmap(mUserIcons.get(id));
			return;
		}
		
		if(mPreLoad)
		{
			Bitmap bitmap = ACacheUtil.getBitmap(AFacebook.BASE_PATH + id + "/picture");
			mUserIcons.put(id, bitmap);
			imageview.setImageBitmap(mUserIcons.get(id));
			return;
		}
		
		new Thread( new Runnable() {
			public void run() {
				Bitmap bitmap = ACacheUtil.getBitmap(AFacebook.BASE_PATH + id + "/picture");
				mUserIcons.put(id, bitmap);
				mHandler.post(new Runnable() {
					public void run() {
						imageview.setImageBitmap(mUserIcons.get(id));
					}
				});
				
			}
		}).start();
		
		
	}
	
	private void loadPicture(final String url, final ImageView imageview)
	{
		
		
		if(mPreLoad)
		{
			Bitmap bitmap = ACacheUtil.getBitmap(url);
			imageview.setImageBitmap(bitmap);
		}
		else
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
		
	}
	
	private void deletePost(final String fbId, final String insiderId)
	{
		final ProgressDialog dialog = new ProgressDialog(getParent());
		
		dialog.show();
		if(insiderId != null && fbId != null) //delete insider message
		{
			dialog.setMessage("Deleting insid3r Message...");
			new Thread(new Runnable() {
				public void run() {
					try {
						String url = Insider.getUrlPath("msg/" + insiderId) + "?iToken=" + Util.mInsider.getAccessToken();
						Util.log(url);
						String response = AHttpUtil.deleteUrl(url, null);
						Util.log(response);
						if(response.compareToIgnoreCase("true") == 0)
							mInsiderMap.remove(IUtil.normalizeFacebookId(fbId));
						
						mHandler.post(new Runnable() {
							public void run() {
								navigateToPath(mCurrentPage, true, true, true);
							}
						});
						dialog.dismiss();
					}catch(Exception ex) {}
				}
			}).start();
			
		}
		else if(insiderId == null && fbId != null)
		{
			dialog.setMessage("Deleting facebook Message...");
			new Thread(new Runnable() {
				public void run() {
					try {
						
						String url = AFacebook.getFullUrl(fbId, null);
						Util.log(url);
						String results = AHttpUtil.deleteUrl(url, null);
						Util.log(results);
						mHandler.post(new Runnable() {
							public void run() {
								navigateToPath(mCurrentPage, true, true, true);
							}
						});
						dialog.dismiss();
					}catch(Exception ex) {}
				}
			}).start();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == REQUEST_STATUS && resultCode == RESULT_OK)
			navigateToPath(mCurrentPage, true, true, true);
	}

	public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos,
			long arg3) {
		
		Util.log(String.valueOf(pos));
		Util.log(String.valueOf(view.getTag()));
		//Holders.FBNavHolder holder = (Holders.FBNavHolder)view.getTag();
		/*String[] items = {"Delete"};
		if(holder.isOwner && holder.isInsider)
			items = new String[] {"Delete"};
		else if(holder.isOwner && !holder.isInsider)
			items = new String[] {"Delete", "Convert to Insid3r"};
		else
			return false;
		*/
		/*TextView newview = new TextView(this);
		
		newview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		newview.setHint("Your Stealth Message...");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
								
			}
		});
		builder.create().show();
		*/
		
		return false;
	}
	/*
	 * 
	 * Load More Events
	 * 
	 * 
	 * 
	 */
	
	
	
	/*
	 * 
	 * Stealth Methods
	 * 
	 */
	private void collectIds()
	{
		if(!Util.mInsider.isLoggedIn())
			mInsiderMap.clear();
		
		ArrayList<String> strings = IUtil.getBasicIds(mJsonData);
		JSONArray array = new JSONArray();
		for(String string : strings)
		{
			if(!mInsiderMap.containsKey(string) && !mCheckedIds.contains(string))
			{
				array.put(IUtil.normalizeFacebookId(string));
				mCheckedIds.add(string);
			}
		}
		translateMessages(array);
	}
	
	private void translateMessages(JSONArray array)
	{
		if(!Util.mInsider.isLoggedIn())
		{
			mAdapter.notifyDataSetChanged();
			return;
		}
		
		if(array.length() > 0)
		{
			Bundle params = new Bundle();
			params.putString("ids", array.toString());
			params.putString("iToken", Util.mInsider.getAccessToken());
			//mInsiderMap.clear();
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
			mAdapter.notifyDataSetChanged();
		}
	}
	
	/*
	 * 
	 * Gesture Detection
	 */
	public class MyGestureDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 120;
	    private static final int SWIPE_MAX_OFF_PATH = 250;
	    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	    @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	        try {
	            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
	                return false;
	            // right to left swipe
	            /*if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	View view = mList.getChildAt(mGesturePosition);
	            	FibHolder holder = (FibHolder)view.getTag();
	            	holder.btnRemove.setVisibility(View.INVISIBLE);
	            	mGesturePosition = mList.pointToPosition((int)e2.getX(), (int)e2.getY());
	                Toast.makeText(FacebookInbox.this, "Left Swipe " + String.valueOf(mGesturePosition), Toast.LENGTH_SHORT).show(); 
	                return true;
	            }  else*/ if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	//Toast.makeText(FacebookNav.this, "Right Swipe", Toast.LENGTH_SHORT).show();
	            	
	            	if(mLastHolder != null)
	            		mLastHolder.btnRemove.setVisibility(View.INVISIBLE);
	            	
	            	mGesturePosition = mList.pointToPosition((int)e2.getX(), (int)e2.getY()) - scrollOffset;
	            	View view = mList.getChildAt(mGesturePosition);
	            	if(view != null)
	            	{
	            		Holders.FBNavHolder holder = (Holders.FBNavHolder)view.getTag();
	            		if(holder.isOwner)
	            		{
	            			holder.btnRemove.setVisibility(View.VISIBLE);
	            			mLastHolder = holder;
	            		}
	            		
	            	}
	            }
	        } catch (Exception e) {
	            // nothing
	        }
	        return false;
	    }
	    @Override
	    public boolean onDown(MotionEvent e) {
	    	return false;
	    }
	    @Override
	    public boolean onSingleTapConfirmed(MotionEvent e) {
	    	//int pos = mList.pointToPosition((int)e.getX(), (int)e.getY());
	    	if(mLastHolder != null)
	    		mLastHolder.btnRemove.setVisibility(View.INVISIBLE);
	    	return false;
	    }
	}

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		scrollOffset = firstVisibleItem;
		
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if(scrollState == SCROLL_STATE_TOUCH_SCROLL || scrollState == SCROLL_STATE_FLING)
		{
			isScrolling = true;
			if(mLastHolder != null)
				mLastHolder.btnRemove.setVisibility(View.INVISIBLE);
		}
		else
			isScrolling = false;
	}
	
	private void loadMore()
	{
		
	}
	
}
