package com.psm.android;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.psm.android.R;
import com.psm.util.Insider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PatternMatcher;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class AcctCreate extends Activity {

	
	private EditText txtFirstName, txtLastName, txtPhoneNumber, txtEmail, txtUsername;
	private ImageView imgCheck;
	private Button btnSubmit, btnCode;
	
	
	private Thread thread_username;
	private Handler mHandler;
	private Context mContext;
	private boolean isAvailable = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.acct_create);
		mHandler = new Handler();
		mContext = this;
		
		txtPhoneNumber = (EditText)findViewById(R.id.ac_phone);
		txtFirstName = (EditText)findViewById(R.id.ac_firstname);
		txtLastName = (EditText)findViewById(R.id.ac_lastname);
		txtEmail = (EditText)findViewById(R.id.ac_email);
		txtUsername = (EditText)findViewById(R.id.ac_username);
		imgCheck = (ImageView)findViewById(R.id.ac_checkmark);
		btnCode = (Button)findViewById(R.id.ac_btncode);
		btnSubmit = (Button)findViewById(R.id.ac_btnsubmit);
		
		
		btnSubmit.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				//TODO: Validate fields before sending
				ArrayList<String> array = validateFields();
				if(array.size() > 0)
				{
					new AlertDialog.Builder(mContext).setTitle("Error").setMessage(array.get(0)).show();
				}
				else
					createAccount();
			}
		});
		
		btnCode.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				//Util.log(validatePhone(txtPhoneNumber.getText().toString()));
				searchCountryCode();
			}
		});
		
		txtPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
		TelephonyManager tman = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		txtPhoneNumber.setText(tman.getLine1Number());
		
		txtUsername.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
			
			public void afterTextChanged(Editable arg0) {
				if(arg0.length() < 3)
				{
					imgCheck.setImageResource(android.R.drawable.checkbox_off_background);
					return;
				}
				
				if(!validateUsername(arg0.toString()))
					imgCheck.setImageResource(android.R.drawable.checkbox_off_background);
				else
					isUsernameAvailable(arg0.toString());
			}
		});
	}
	
	private void searchCountryCode()
	{
		Intent intent = new Intent(AcctCreate.this, CountryCodeActivity.class);
		startActivityForResult(intent, 4);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 4 && resultCode == RESULT_OK)
		{
			
			btnCode.setText("+" + data.getExtras().getString("com.psm.android.prefix"));
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	private void createAccount()
	{
		final ProgressDialog dialog = new ProgressDialog(mContext);
		dialog.setMessage("Creating Insid3r Account...");
		dialog.show();
		
		new Thread( new Runnable() {
			
			public void run() {
				Bundle bundle = new Bundle();
				bundle.putString("firstName", txtFirstName.getText().toString());
				bundle.putString("lastName", txtLastName.getText().toString());
				bundle.putString("countryCode", URLEncoder.encode(btnCode.getText().toString()));
				bundle.putString("phoneNumber",  URLEncoder.encode(txtPhoneNumber.getText().toString()));
				bundle.putString("eMail",  URLEncoder.encode(txtEmail.getText().toString()));
				bundle.putString("userName", txtUsername.getText().toString());
				bundle.putString("iToken", Util.mInsider.getAccessToken());
				Util.log("Post Token: " + bundle.getString("iToken"));
				try {
					String results = AHttpUtil.simplePost(Insider.I_BASE_PATH + "user/setinfo", bundle);
					Util.log(results);
					if(results.compareToIgnoreCase("true") == 0)
					{
						mHandler.post(new Runnable() {
							public void run() {
								Intent intent = new Intent(AcctCreate.this, MainMan.class);
								startActivity(intent);
								finish();
							}
						});
					}
					else
					{
						mHandler.post(new Runnable() {
							public void run() {
								new AlertDialog.Builder(mContext).setTitle("Error 1002")
									.setMessage("Error Creating Account.").show();
							}
						});
					}
						
				}catch(Exception ex) {}
				dialog.dismiss();
				
			}
		}).start();
		
		
		
		
	}
	
	private ArrayList<String> validateFields()
	{
		ArrayList<String> array = new ArrayList<String>();
		
		Pattern pattern = Pattern.compile("^[a-zA-Z]+$");
		Pattern pattern1 = Pattern.compile("^[A-Za-z][A-Za-z0-9]+$");
		
		Matcher matcher = pattern.matcher(txtFirstName.getText().toString());
		if((!matcher.matches() && txtFirstName.getText().length() > 0) || txtFirstName.getText().length() == 0)
			array.add("First Name Invalid");
		
		Matcher matcher1 = pattern.matcher(txtLastName.getText().toString());
		if((!matcher1.matches() && txtLastName.getText().length() > 0 ) || txtLastName.getText().length() == 0)
			array.add("Last Name Invalid");
		
		if(!validatePhone(txtPhoneNumber.getText().toString()) && txtPhoneNumber.length() > 0)
			array.add("Phone Number Invalid");
		
		if(!validateEmail(txtEmail.getText().toString()) && txtEmail.length() > 0)
			array.add("Email Address Invalid");
		
		Matcher matcher2 = pattern1.matcher(txtUsername.getText().toString());
		if(!matcher2.matches() && txtUsername.getText().length() > 0)
			array.add("Username Invalid");
		else if(matcher2.matches() && txtUsername.getText().length() > 0)
		{
			if(!isAvailable)
				array.add("Username not available");
		}
		
		
		return array;
	}
	
	private boolean validateUsername(String username)
	{
		Pattern pattern = Pattern.compile("^[A-Za-z][A-Za-z0-9]+$");
		Matcher matcher = pattern.matcher(username);
		return matcher.matches();
		
	}
	
	private boolean validatePhone(String phone)
	{
		String string = phone.replaceAll("[^0-9]+", "");
		Pattern pattern = Pattern.compile("^[0-9]+$");
		Matcher matcher = pattern.matcher(string);
		return matcher.matches();
	}
	
	private boolean validateEmail(String email)
	{
		Pattern pattern = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@" +
					"[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
		
	}
	
	private void isUsernameAvailable(String username)
	{
		if(thread_username != null)
			if(thread_username.isAlive())
				return;
		
		final String fusername = username;
		thread_username = new Thread( new Runnable() {
			
			public void run() {
				mHandler.post(new Runnable() {
					public void run() {
						
						Bundle params = new Bundle();
						params.putString("iToken", Util.mInsider.getAccessToken());
						params.putString("userName", fusername);
						
						try {
							String response = AHttpUtil.openUrl(Insider.I_BASE_PATH + "user/available", params);
							Util.log(response);
							if(response.compareToIgnoreCase("true") == 0)
							{
								imgCheck.setImageResource(android.R.drawable.checkbox_on_background);
								isAvailable = true;
							}
							else
							{
								imgCheck.setImageResource(android.R.drawable.checkbox_off_background);
								isAvailable = false;
							}
						}catch(Exception ex){}
						
					}
				});
			}
		});
		
		thread_username.start();

	}
}
