package com.psm.android.fb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.ACacheUtil;
import com.psm.android.AFacebook;
import com.psm.android.AHttpUtil;
import com.psm.android.R;
import com.psm.android.Util;
import com.psm.android.R.color;
import com.psm.android.R.drawable;
import com.psm.android.R.id;
import com.psm.android.R.layout;
import com.psm.util.IUtil;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

public class FacebookInbox extends Activity implements OnItemLongClickListener{
	
	private Handler mHandler;
	private Context mContext;
	private LayoutInflater mInflater;
	
	private ListView mList;
	private FBInboxAdapter mAdapter;
	private FBInboxCommentAdapter mCommentAdapter;
	
	private JSONArray mJsonInbox, mJsonComments;
	private LinearLayout mProgress;
	private boolean mShowingComments = false;
	
	private HashMap<String, Bitmap> mIcons = new HashMap<String, Bitmap>();
	
	private HashMap<String, String> mInsiderMap = new HashMap<String, String>();
	private ArrayList<String> mCheckedIds = new ArrayList<String>();
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.facebook_inbox);
		mHandler = new Handler();
		mContext = this;
		mInflater = getLayoutInflater();
		mProgress = (LinearLayout)findViewById(R.id.fb_inbox_progress);
		mList = (ListView)findViewById(R.id.fb_inbox_list);
		mAdapter = new FBInboxAdapter();
		mCommentAdapter = new FBInboxCommentAdapter();
		mList.setOnItemLongClickListener(this);

		loadInbox(true);
		
	}
	
	private void loadInbox(final boolean forceRefresh)
	{
		mList.setVisibility(View.INVISIBLE);
		mProgress.setVisibility(View.VISIBLE);
		new Thread(new Runnable() {
			
			public void run() {
				final String results = ACacheUtil.getUrl(AFacebook.getFullUrl("me/inbox", null), forceRefresh);
				Util.log(AFacebook.getFullUrl("me/inbox", null));
				mHandler.post( new Runnable() {
					
					public void run() {
						
						try {
							mJsonInbox = new JSONObject(results).getJSONArray("data");
							mList.setAdapter(mAdapter);
							mList.setOnItemClickListener(mAdapter);
							mAdapter.notifyDataSetChanged();
							mList.setVisibility(View.VISIBLE);
							mProgress.setVisibility(View.GONE);
						}catch(Exception ex) {}
						
					}
				});
				
			}
		}).start();
		
		
	}
	
	private void loadComments(JSONObject jsonMessage, final boolean forceRefresh)
	{
		mShowingComments = true;
		mList.setVisibility(View.INVISIBLE);
		mProgress.setVisibility(View.VISIBLE);
		
		try {
			final String inboxId = jsonMessage.getString("id");
		
			new Thread(new Runnable() {
				public void run() {
					final String results = ACacheUtil.getUrl(AFacebook.getFullUrl(inboxId + "/comments", null), forceRefresh);
					mHandler.post( new Runnable() {
						public void run() {
							
							try {
								mJsonComments = new JSONObject(results).getJSONArray("data");
								mList.setAdapter(mCommentAdapter);
								mList.setOnItemClickListener(mCommentAdapter);
								mCommentAdapter.notifyDataSetChanged();
								mList.setVisibility(View.VISIBLE);
								mProgress.setVisibility(View.GONE);
								collectIds(mJsonComments);
							}catch(Exception ex) {}
							
						}
					});
					
				}
			}).start();
		} catch(Exception ex) {}
	}
	
	private class FBInboxAdapter extends BaseAdapter implements OnItemClickListener
	{

		public int getCount() {
			if(mJsonInbox == null) return 0;
			return mJsonInbox.length();
		}

		public Object getItem(int arg0) {	return null;}

		public long getItemId(int arg0) {	return 0;}

		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.facebook_cell_comment, null);
				FibHolder holder = new FibHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.fb_cell_comment_icon);
				holder.txtName = (TextView)view.findViewById(R.id.fb_cell_comment_name);
				holder.txtDate = (TextView)view.findViewById(R.id.fb_cell_comment_date);
				holder.txtComment = (TextView)view.findViewById(R.id.fb_cell_comment_txt);
				
				view.setTag(holder);
			}
			
			FibHolder holder = (FibHolder)view.getTag();
			try {
				JSONObject obj = mJsonInbox.getJSONObject(position);
				
				String fromid = "";
				if(obj.has("from"))
					fromid = obj.getJSONObject("from").getString("id");
				JSONArray to = obj.getJSONObject("to").getJSONArray("data");
				String string = "";
				String iconId = null;
				boolean appendcomma = false;
				for(int i = 0; i < to.length(); i++)
				{
					JSONObject jsonTo = to.getJSONObject(i);
					if(jsonTo.getString("id").compareTo(fromid) == 0)
						continue;
					else
					{
						if(appendcomma == false)
							iconId = jsonTo.getString("id");
						
						string += (appendcomma ? ", " : "" ) + jsonTo.getString("name").split("[ ]",2)[0];
						appendcomma = true;
					}
				}
				if(to.length() == 1)
					string = to.getJSONObject(0).getString("name");
				
				holder.imgIcon.setImageResource(R.drawable.facebookuser);
				if(iconId == null)
					iconId = fromid;
				loadIcon(holder.imgIcon, iconId);
				holder.txtName.setText(string);
				JSONArray comments = obj.getJSONObject("comments").getJSONArray("data");
				JSONObject lastcomment = comments.getJSONObject(comments.length()-1);
				holder.txtComment.setText(lastcomment.getString("message"));
				if(obj.has("created_time"))
					holder.txtDate.setText(AFacebook.formatTime(obj.getString("created_time")));
				else
					holder.txtDate.setText(AFacebook.formatTime(obj.getString("updated_time")));
				
			} catch(Exception ex){Util.log(ex.getMessage());}
			return view;
		}

		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long arg3) {
			try {
				loadComments(mJsonInbox.getJSONObject(position), false);
			}catch(Exception ex) {}
			
		}
		
	}
	
	
	
	private class FBInboxCommentAdapter extends BaseAdapter implements OnItemClickListener
	{
		
		public int getCount() {
			if(mJsonComments == null) return 0;
			return mJsonComments.length();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = convertView;

			if(view == null)
			{
				view = mInflater.inflate(R.layout.facebook_cell_comment, null);
				FibHolder holder = new FibHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.fb_cell_comment_icon);
				holder.txtName = (TextView)view.findViewById(R.id.fb_cell_comment_name);
				holder.txtDate = (TextView)view.findViewById(R.id.fb_cell_comment_date);
				holder.txtComment = (TextView)view.findViewById(R.id.fb_cell_comment_txt);
				holder.btnRemove = (Button)view.findViewById(R.id.fb_cell_comment_btn);
				view.setTag(holder);
			}
			FibHolder holder = (FibHolder)view.getTag();
			holder.position = position;
			holder.btnRemove.setVisibility(View.INVISIBLE);
			try {
				
				
				JSONObject jcomment = mJsonComments.getJSONObject(position);
				String from = jcomment.getJSONObject("from").getString("name");
				String id = jcomment.getJSONObject("from").getString("id");
				
				//TODO: user facebook object instead of insider object
				if(id.compareTo(Util.mInsider.getFacebookId()) == 0)
					holder.isOwner = true;
				holder.fbId = jcomment.getString("id");
				holder.imgIcon.setImageResource(R.drawable.facebookuser);
				loadIcon(holder.imgIcon, id);
				holder.txtName.setText(from);
				String normalizedId = IUtil.normalizeFacebookId(holder.fbId);
				if(mInsiderMap.containsKey(normalizedId))
				{
					holder.isInsider = true;
					holder.showingInsider = true;
					JSONObject insiderObject = new JSONObject(mInsiderMap.get(normalizedId));
					holder.insiderMessage = insiderObject.getString("message");
					holder.txtComment.setTextColor(getResources().getColor(R.color.stealth_color));
					holder.txtComment.setText(insiderObject.getString("message"));
				}
				else
				{
					holder.isInsider = false;
					holder.txtComment.setText(jcomment.getString("message"));
					holder.txtComment.setTextColor(Color.WHITE);
				}
				
				holder.txtDate.setText(AFacebook.formatTime(jcomment.getString("created_time")));
			}catch(Exception ex) {}
			return view;
		}

		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long arg3) {
			FibHolder holder = (FibHolder)view.getTag();
			if(holder.isInsider)
			{
				if(holder.showingInsider)
				{
					try {
						JSONObject jcomment = mJsonComments.getJSONObject(position);
						holder.txtComment.setText(jcomment.getString("message"));
						holder.txtComment.setTextColor(Color.WHITE);
						holder.showingInsider = !holder.showingInsider;
					}catch(Exception ex){}
				}
				else
				{
					holder.txtComment.setText(holder.insiderMessage);
					holder.txtComment.setTextColor(getResources().getColor(R.color.stealth_color));
					holder.showingInsider = !holder.showingInsider;
				}
			}
		}
		
	}
	
	private void loadIcon(final ImageView imageView, final String fbId)
	{
		if(mIcons.containsKey(fbId))
			imageView.setImageBitmap(mIcons.get(fbId));
		else
		{
			new Thread( new Runnable() {
				public void run() {
					final Bitmap bitmap = ACacheUtil.getBitmap("https://graph.facebook.com/" + fbId + "/picture");
					mHandler.post( new Runnable() {
						public void run() {
							imageView.setImageBitmap(bitmap);
						}
					});
					
					
				}
			}).start();
		}
	}
	
	private class FibHolder
	{
		int position;
		String fbId;
		String iMsgId;
		boolean canDelete = false;
		boolean isOwner = false;
		boolean isInsider = false;
		boolean showingInsider = false;
		String insiderMessage = null;
		
		ImageView imgIcon;
		TextView txtName;
		TextView txtDate;
		TextView txtComment;
		Button btnRemove;
	}
	
	@Override
	public void onBackPressed() {
		if(mShowingComments)
		{
			loadInbox(false);
			mShowingComments = false;
		}
		else
			super.onBackPressed();
	}

	public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos,
			long arg3) {
		final FibHolder holder = (FibHolder)view.getTag();
		JSONObject jcomment = null;
		String fbId = null;
		try {
			jcomment = mJsonComments.getJSONObject(pos);
			fbId = jcomment.getString("id");
		}catch(Exception ex){}
		//final JSONObject fcomment = jcomment;
		final String ffbid = IUtil.normalizeFacebookId(fbId);
		if(holder.isOwner && mShowingComments)
		{
			
			View view1 = mInflater.inflate(R.layout.facebook_alert_view, null);
			final TextView txtView = (TextView)view1.findViewById(R.id.fb_alert_edittext);
			
			new AlertDialog.Builder(mContext)
				.setView(view1).setNegativeButton("Cancel", null).setPositiveButton("Submit", new
						DialogInterface.OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								final ProgressDialog dialog1 = new ProgressDialog(mContext);
								dialog1.setMessage("Creating insid3r message...");
								dialog1.show();
								new Thread(new Runnable() {
									
									public void run() {
										// TODO Auto-generated method stub
										Bundle params = new Bundle();
										params.putString("iToken", Util.mInsider.getAccessToken());
										params.putString("id", ffbid);
										params.putString("message", txtView.getText().toString());
										try {
											String results = AHttpUtil.simplePost(Insider.I_BASE_PATH + "msg/facebook", params);
											mCheckedIds.remove(ffbid);
											mHandler.post(new Runnable() {
												public void run() {
													collectIds(mJsonComments);
												}
											});
										}catch(Exception ex){Util.log("error" + ex.getMessage());}
										dialog1.dismiss();
									}
								}).start();
								
							}
						}).show();
					
			
			
		}
		return false;
	}
	
	private void collectIds(JSONArray objects)
	{
		if(!Util.mInsider.isLoggedIn())
			mInsiderMap.clear();
		
		ArrayList<String> strings = IUtil.getBasicIds(objects);
		JSONArray array = new JSONArray();
		for(String string : strings)
		{
			if(!mInsiderMap.containsKey(string) && !mCheckedIds.contains(string))
			{
				array.put(string);
				mCheckedIds.add(string);
			}
		}
		
		translateMessages(array);
	}

	private void translateMessages(JSONArray array)
	{
		if(!Util.mInsider.isLoggedIn())
			return;
		if(array.length() == 0)
			return;
		
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
				mInsiderMap.put(string, obj.getString(string));
			}
		}catch(Exception ex) {Util.log(ex.getMessage());}
		Util.log("Map Size: " + mInsiderMap.size());
		mAdapter.notifyDataSetChanged();
	}
	
}
