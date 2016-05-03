package com.psm.android;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.psm.android.R;
import com.psm.android.fb.FacebookFriends;
import com.psm.android.fb.FacebookNav.MyGestureDetector;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class GroupCreate extends Activity implements OnScrollListener {

	private ListView mList;
	private LayoutInflater mInflater;
	private Handler mHandler;
	private GroupAdapter mAdapter;
	
	private Button mBtnAdd, mBtnCreate, BtnFBFriend;
	private EditText mEditNameorId, mEditGroupName;
	
	private ArrayList<groupItem> mItemArray = new ArrayList<groupItem>();
	
	
	private cellHolder mLastHolder = null;
	private boolean isScrolling = false;
	private int mGesturePosition = 0;
    private int scrollOffset = 0;
    private GestureDetector gestureDetector;
    private View.OnTouchListener gestureListener;
	
    private HashMap<String,String> mNameFbIdList = new HashMap<String,String>();
    private Context mContext;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.group_create);
		mContext = this;
		mInflater = getLayoutInflater();
		mHandler = new Handler();
		mBtnAdd = (Button)findViewById(R.id.group_create_btnadd);
		mBtnCreate = (Button)findViewById(R.id.group_create_btncreate);
		mEditNameorId = (EditText)findViewById(R.id.group_create_editid);	
		mEditGroupName = (EditText)findViewById(R.id.group_create_editname);
		BtnFBFriend = (Button)findViewById(R.id.group_create_addfb);
		mAdapter = new GroupAdapter();
		mList = (ListView)findViewById(R.id.group_create_list);
		mList.setAdapter(mAdapter);
		
		mAdapter.notifyDataSetChanged();
		
		mBtnAdd.setOnClickListener( new View.OnClickListener() {
			
			public void onClick(View v) {
				insiderAdd();
			}
		});
		
		BtnFBFriend.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				Intent intent = new Intent(GroupCreate.this, FacebookFriends.class);
				intent.putExtra("com.psm.android.id", "me");
				startActivityForResult(intent, 4);
			}
		});
		mBtnCreate.setOnClickListener( new View.OnClickListener() {
			
			public void onClick(View v) {
				String[] items = {"Private", "Public"};
				new AlertDialog.Builder(mContext).setTitle("Group Type:")
					.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if(which == 0)
								CreateGroup(true);
							else
								CreateGroup(false);
						}
					}).create().show();
				
			}
		});
		
		gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        
        mList.setOnTouchListener(gestureListener);
        mList.setOnScrollListener(this);
        
	}
	
	private void CreateGroup(final boolean isPrivate)
	{
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage("Creating Group...");
		dialog.show();
		if(mEditGroupName.getText().toString().length() < 3)
		{
			dialog.dismiss();
			
			new AlertDialog.Builder(this).setTitle("Group Name Too Short").setMessage("Minimum length is 3 characters for Group Name.")
				.setPositiveButton("Ok", null).create().show();
			
			return;
		}
		new Thread(new Runnable() {
			
			public void run() {
				try{ 
					
					String path = Insider.getUrlPath("group/create");
					Bundle params = new Bundle();
					params.putString("iToken", Util.mInsider.getAccessToken());
					params.putString("groupName", mEditGroupName.getText().toString());
					params.putString("isSecret", String.valueOf(isPrivate));
					JSONArray jsonMembers = new JSONArray();
					JSONArray jsonfbfriend= new JSONArray();
					for(groupItem item : mItemArray)
					{
						JSONObject obj = new JSONObject();
						if(item.item_type == 1) //is FB id
						{
							jsonfbfriend.put(item.id);
						}
						else
						{
							jsonMembers.put(item.id);
						}
					}
					params.putString("members", jsonMembers.toString());
					params.putString("facebookIds", jsonfbfriend.toString());
					
					final String results = AHttpUtil.simplePost(path, params);
					Util.log(results);
					
					mHandler.post(new Runnable() {
						public void run() {
							try {
								JSONArray retJson = new JSONObject(results).getJSONArray("invite");
								if(retJson.length() > 0)
								{
									dialog.dismiss();
									/*new AlertDialog.Builder(mContext)
										.setMessage(retJson.toString() + " are not insid3rs. Invite them!")
										.setPositiveButton("Ok", null).create().show();
									*/
									setResult(RESULT_OK);
									finish();
								}
								else
								{
									dialog.dismiss();
									setResult(RESULT_OK);
									finish();
								}
							}catch(Exception ex) {Util.log(ex.getMessage());}
							
						}
					});
				}catch(Exception ex) {Util.log(ex.getMessage());}
			}
		}).start();
		
	}
	
	private void insiderAdd()
	{
		groupItem item = new groupItem();
		item.item_type = 0;
		item.username = mEditNameorId.getText().toString();
		if(!mItemArray.contains(item))
		{
			mItemArray.add(item);
			mEditNameorId.setText("");
			mAdapter.notifyDataSetChanged();
			mList.setSelection(mList.getCount()-1);
		}
	}
	
	private class GroupAdapter extends BaseAdapter
	{

		public int getCount() {
			return mItemArray.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int arg0, View arg1, ViewGroup arg2) {
			View view = arg1;
			
			if(view == null)
			{
				view = mInflater.inflate(R.layout.group_cell_item, null);
				cellHolder holder = new cellHolder();
				holder.imgIcon = (ImageView)view.findViewById(R.id.group_cell_image);
				holder.txtName = (TextView)view.findViewById(R.id.group_cell_name);
				holder.txtType = (TextView)view.findViewById(R.id.group_cell_type);
				holder.btnRemove = (Button)view.findViewById(R.id.group_cell_btnremove);
				view.setTag(holder);
			}
			
			cellHolder holder = (cellHolder)view.getTag();
			holder.btnRemove.setVisibility(View.INVISIBLE);
			groupItem item = mItemArray.get(arg0);
			final int position = arg0;
			
			holder.btnRemove.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if(mLastHolder != null)
						mLastHolder.btnRemove.setVisibility(View.INVISIBLE);
					
					mItemArray.remove(position);
					mAdapter.notifyDataSetChanged();
				}
			});
			holder.txtName.setText(item.username);
			if(item.item_type == 0)
			{
				//is insider item
				holder.imgIcon.setImageResource(R.drawable.icon);
			}
			else if(item.item_type == 1)
			{
				//is facebook user
				holder.imgIcon.setImageResource(R.drawable.facebookuser);
			}
			return view;
		}
		
	}
	
	private class cellHolder
	{
		ImageView imgIcon;
		TextView txtName;
		TextView txtType;
		Button btnRemove;
	}
	
	private class groupItem
	{
		int item_type = 0;
		String username;
		String id;
	}
	
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
	            		cellHolder holder = (cellHolder)view.getTag();
	            		holder.btnRemove.setVisibility(View.VISIBLE);
	            		mLastHolder = holder;
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == 4 && resultCode == RESULT_OK)
		{
			groupItem item = new groupItem();
			item.item_type = 1;
			item.username = data.getExtras().getString("com.psm.android.name");
			item.id = data.getExtras().getString("com.psm.android.fbid");
			mNameFbIdList.put(item.id, item.username);
			if(!mItemArray.contains(item))
			{
				mItemArray.add(item);
				mAdapter.notifyDataSetChanged();
				mList.setSelection(mList.getCount()-1);
			}
			
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
}
