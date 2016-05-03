package com.psm.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.json.JSONObject;

import com.psm.android.R;
import com.psm.android.fs.FoursquareLogin;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;

public class SettingsActivity extends Activity implements OnItemSelectedListener {

	private ToggleButton fbToggle, fsToggle, smsToggle;
	private Context mContext;
	private boolean fsResult = false;
	private boolean smsResult = false;
	
	private LayoutInflater mInflater;
	private Button btnGroup, btnPin, btnUser, btnAccount;
	private Handler mHandler;
	private Spinner mSpinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.settings);
		mContext = this;
		mInflater = getLayoutInflater();
		mHandler = new Handler();
		
		fbToggle = (ToggleButton)findViewById(R.id.set_fb_toggle);
		fsToggle = (ToggleButton)findViewById(R.id.set_fs_toggle);
		smsToggle = (ToggleButton)findViewById(R.id.set_sms_toggle);
		
		
		btnPin = (Button)findViewById(R.id.set_btnpin);
		btnUser = (Button)findViewById(R.id.set_btnuser);
		btnGroup = (Button)findViewById(R.id.set_btnigroups);
		btnAccount = (Button)findViewById(R.id.set_btnaccount);
		
		if(!Util.mInsider.isLoggedIn())
		{
			btnGroup.setVisibility(View.GONE);
			btnAccount.setVisibility(View.GONE);
		}
		
		btnAccount.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(SettingsActivity.this, AcctCreate.class);
				intent.putExtra("com.psm.android.modify", true);
				startActivity(intent);
			}
		});
		
		
		btnUser.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				final ProgressDialog dialog = new ProgressDialog(mContext);
				dialog.setMessage("Loading Info...");
				dialog.show();
				new Thread(new Runnable() {
					public void run() {
						try {
							
							Bundle bundle = new Bundle();
							bundle.putString("iToken", Util.mInsider.getAccessToken());
							final String results = AHttpUtil.openUrl(Insider.I_BASE_PATH + "user/self", bundle);							
							mHandler.post(new Runnable() {
								
								public void run() {
									try {
										JSONObject obj = new JSONObject(results);
										JSONObject user =	obj.getJSONObject("user");
										String userInfo = "Name: " + user.getString("firstName") + " " + user.getString("lastName");
										userInfo += "\nUsername: " + user.getString("userNameFormat");
										userInfo += "\ninsid3r id: " + user.getString("insiderId");
										new AlertDialog.Builder(mContext).setMessage(userInfo).setPositiveButton("Ok", null).show();
									}catch(Exception ex){ Util.log(ex.getMessage());}
								}
							});
						}catch(Exception ex) {Util.log(ex.getMessage());}
						dialog.dismiss();
					}
				}).start();
				
			}
		});
		
		btnGroup.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(SettingsActivity.this, GroupListActivity.class);
				startActivity(intent);
				
			}
		});
		
		SharedPreferences prefs = getSharedPreferences(SessionManager.SMS_PREFS, MODE_PRIVATE);
		smsToggle.setChecked(prefs.getBoolean("listen", true));
		
		if(Util.mFoursquare.getToken() != null)
		{
			fsToggle.setChecked(true);
		}
		else
			fsToggle.setChecked(false);
		
		btnPin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				usesPin();
			}
		});
		
		fbToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				if(!isChecked)
				{
					Util.log("checked");
					((ToggleButton)arg0).setChecked(true);
				}
			}
		});
		
		fsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if(isChecked && !fsResult)
				{
					Intent intent = new Intent(SettingsActivity.this, FoursquareLogin.class);
					startActivityForResult(intent, 1);
				}
				else
				{
					if(!fsResult)
					{
						new AlertDialog.Builder(mContext).setTitle("Log out of Foursquare?")
								.setMessage("This will log you out of foursquare.  All Foursquare features" +
										" will be disabled.").setNegativeButton("No", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												fsResult = true;
												fsToggle.setChecked(true);
											}
										})
										.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
											
											public void onClick(DialogInterface arg0, int arg1) {
												Util.mFoursquare.setToken(null);
												Util.mSession.initFoursquare();
												SessionManager.saveFoursquare(Util.mFoursquare, mContext);
												setupSpinner();
											}
										}).show();
					}
				}
				fsResult = false;
				
			}
		});
		
		smsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				final Editor editor = mContext.getSharedPreferences(SessionManager.SMS_PREFS, MODE_PRIVATE).edit();
				if(!smsResult && !isChecked)
				{
				new AlertDialog.Builder(mContext).setTitle("No More SMS Notifications")
					.setMessage("Disabling SMS: You will no longer receive regular sms notifications, you will still receive notifications for insid3r sms messages, is " +
							"that what you want?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									smsResult = true;
									smsToggle.setChecked(false);
									editor.putBoolean("listen", false);
									editor.commit();
								}
							}).setNegativeButton("No", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									smsResult = true;
									smsToggle.setChecked(true);
									editor.putBoolean("listen", true);
									editor.commit();
								}
							}).create().show();
					
				}else{ smsResult = false; }
			}
		});
		
		mSpinner = (Spinner)findViewById(R.id.set_spinner);
        setupSpinner();
        mSpinner.setOnItemSelectedListener(this);
	}
	
	private void setupSpinner()
	{
		String[] mStrings = {"facebook", "foursquare", "SMS"};
		String[] mStrings1 = {"facebook", "SMS"};
		ArrayList<String> mListItems = new ArrayList<String>();
		if(!fsToggle.isChecked())
			mListItems.addAll(Arrays.asList(mStrings1));
		else
			mListItems.addAll(Arrays.asList(mStrings));
		
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.simple_item , mListItems);
        mSpinner.setAdapter(adapter);
        if(!fsToggle.isChecked())
		{
			if(startupIndex() > 0)
				mSpinner.setSelection(startupIndex()-1);
			else
				mSpinner.setSelection(startupIndex());
		}
        else
        	mSpinner.setSelection(startupIndex());
	}
	private int startupIndex()
	{
		SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
		int startup = prefs.getInt("startup", 0);
		return startup;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
	
		if(requestCode == 1 && resultCode == RESULT_OK)
		{
			Util.mFoursquare.setLoginTime(System.currentTimeMillis());
			SessionManager.saveFoursquare(Util.mFoursquare, this);
			if(Util.mFoursquare.getToken() != null)
				fsToggle.setChecked(true);
			setupSpinner();
			
		}
		else if(requestCode == 1 && resultCode == RESULT_CANCELED)
		{
			fsResult = true;
			fsToggle.setChecked(false);
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void usesPin()
	{
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Loading...");
		dialog.show();
		
		new Thread(new Runnable() {
			public void run() {
				String path = "user/pin/use";
				String fullpath = Insider.I_BASE_PATH + path;
				Bundle bundle = new Bundle();
				bundle.putString("iToken", Util.mInsider.getAccessToken());
				try {
					final String results = AHttpUtil.openUrl(fullpath, bundle);
					mHandler.post(new Runnable() {

						public void run() {
							if (results.compareTo("true") == 0)
								pinSetup(true);
							else
								pinSetup(false);
						}
					});

				} catch (Exception ex) {
					mHandler.post(new Runnable() {

						public void run() {
							pinSetup(false);
						}
					});

				}
				dialog.dismiss();
			}
		}).start();
		
	}
	
	private void pinSetup(boolean uses)
	{
		
		View view = mInflater.inflate(R.layout.pin_menu, null);
		final pinHolder holder = new pinHolder();
		holder.confirmLayout = (LinearLayout)view.findViewById(R.id.pin_menu_confirm);
		holder.txtConfirm = (EditText)view.findViewById(R.id.pin_menu_econfirm);
		holder.txtPin = (EditText)view.findViewById(R.id.pin_menu_pin);
		holder.chkRem = (CheckBox)view.findViewById(R.id.pin_menu_remember);
		holder.chkRem.setVisibility(View.GONE);
		try {
			
			
			if(uses)
			{
				Builder builder = new AlertDialog.Builder(mContext);
				builder.setView(view);
				
				holder.confirmLayout.setVisibility(View.GONE);
				holder.chkRem.setVisibility(View.VISIBLE);
				builder.setNegativeButton("Cancel", null);
				builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog dialog1 = new ProgressDialog(mContext);
						dialog1.setMessage("Logging you in...");
						dialog1.show();
						new Thread(new Runnable() {
							
							public void run() {
								
								String path1 = Insider.I_BASE_PATH + "user/pin/login";
								Bundle params = new Bundle();
								params.putString("iToken", Util.mInsider.getAccessToken());
								params.putString("pin", holder.txtPin.getText().toString());
								params.putString("rememberPin", String.valueOf(holder.chkRem.isChecked()));
								try {
									String results = AHttpUtil.simplePost(path1, params);
									Util.log("Login Results: " + results);
									if(results.compareTo("true") == 0)
									{
										Util.mInsider.setUsesPin(true);
										Util.mInsider.setPinned(true);
										btnGroup.setVisibility(View.VISIBLE);
										//btnAccount.setVisibility(View.VISIBLE);
									}
									else
									{
										mHandler.post(new Runnable() {
											public void run() {
												new AlertDialog.Builder(mContext).setTitle("Error").setMessage("Error logging you into the insid3r...")
												.setPositiveButton("Ok", null).create().show();
												Util.mInsider.setPinned(false);
												btnGroup.setVisibility(View.GONE);
												//btnAccount.setVisibility(View.GONE);
											}
										});
										
									}
									
								}catch(Exception ex) {}
								dialog1.dismiss();
							}
						}).start();
						
						
						
					}
				});
				builder.create().show();
			}
			else
			{
				
				Builder builder = new AlertDialog.Builder(mContext);
				builder.setView(view);
				builder.setNegativeButton("Cancel", null);
				builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						if(holder.txtConfirm.getText().toString().compareTo(holder.txtPin.getText().toString()) == 0)
						{							
							
							final ProgressDialog dialog = new ProgressDialog(mContext);
							dialog.setMessage("Setting Pin...");
							dialog.show();
							new Thread(new Runnable() {
								public void run() {
									String fullpath = Insider.getUrlPath("user/pin/set");
									Bundle bundle = new Bundle();
									bundle.putString("iToken", Util.mInsider.getAccessToken());
									bundle.putString("pin", holder.txtPin.getText().toString());
									try {
										String results = AHttpUtil.simplePost(fullpath, bundle);
										Util.log("setpin: " + results);
										Util.mInsider.setUsesPin(true);
									}catch(Exception ex) {}
									dialog.dismiss();
								}
							}).start();
						}
						else
						{
							new AlertDialog.Builder(mContext).setMessage("Error! Pin Numbers must match")
							.setPositiveButton("Ok", null).create().show();
						}
					}
				});
				builder.create().show();
			}
		}catch(Exception ex) {}
		
	}
	private class pinHolder
	{
		LinearLayout confirmLayout;
		EditText txtPin;
		EditText txtConfirm;
		CheckBox chkRem;
		
	}
	public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
			long arg3) {
		
		Editor edit = getSharedPreferences("app", MODE_PRIVATE).edit();
		if(!fsToggle.isChecked())
		{
			if(pos > 0)
				edit.putInt("startup", pos+1);
			else
				edit.putInt("startup", pos);
		}
		else
			edit.putInt("startup", pos);
		
		edit.commit();
		
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
