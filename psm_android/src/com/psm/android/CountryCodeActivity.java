package com.psm.android;

import java.util.ArrayList;

import com.psm.android.R;
import com.psm.android.CountryCodeDatabase.CCItem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class CountryCodeActivity extends Activity implements OnItemClickListener {

	private ListView mList;
	private CCListAdapter mAdapter;
	private Context mContext;
	
	
	private EditText mTxtFilter;
	private LayoutInflater mInflater;
	
	private CountryCodeDatabase mDb;
	
	private ArrayList<CCItem> mCountryCodes;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.country_code_search);
		
		mContext = this;
		
		mAdapter = new CCListAdapter();
		mList = (ListView)findViewById(R.id.cc_list);
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
		mTxtFilter = (EditText)findViewById(R.id.cc_filter);
		mTxtFilter.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
			
			public void afterTextChanged(Editable arg0) {
				filterResults(arg0.toString());			
			}
		});
		mDb = new CountryCodeDatabase(this);
	}
	
	@Override
	protected void onResume() {
		mDb.open();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		mDb.close();
		super.onPause();
	}
	
	private void filterResults(String filterStr)
	{
		if(filterStr.length() > 0)
		{
			mCountryCodes = mDb.getCountry(filterStr);
			mAdapter.notifyDataSetChanged();
		}
		else
		{
			mCountryCodes = null;
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private class CCListAdapter extends BaseAdapter
	{

		public int getCount() {
			if(mCountryCodes == null) return 0;
			return mCountryCodes.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return 0;
		}

		public View getView(int pos, View convertView, ViewGroup viewGroup) {

			View view = convertView;
			if(view == null)
			{
				view = new TextView(mContext);
				view.setPadding(10,10,10,10);
			}
			
			TextView txt = (TextView)view;
			
			CCItem item = mCountryCodes.get(pos);
			txt.setText("+" + item.code + " - " + item.country + " - " + item.prefix);
			
			return txt;
		}
		
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
		CCItem item = mCountryCodes.get(pos);
		Intent data = new Intent();
		data.putExtra("com.psm.android.prefix", item.code);
		setResult(RESULT_OK, data);
		finish();
		
	}
}
