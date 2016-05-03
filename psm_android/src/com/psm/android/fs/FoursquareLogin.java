package com.psm.android.fs;


import java.net.URLDecoder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.psm.android.R;
import com.psm.android.SessionManager;
import com.psm.android.Util;
import com.psm.android.R.id;
import com.psm.android.R.layout;
public class FoursquareLogin extends Activity {

	
	private Context mContext;
	/**
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.weblayout);
		mContext = this;
		
		WebView web = (WebView)findViewById(R.id.wl_webview);
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setSaveFormData(false);
		web.getSettings().setSavePassword(false);
		
		CookieSyncManager.createInstance(this); 
	    CookieManager cookieManager = CookieManager.getInstance();
	    cookieManager.removeAllCookie();
	    
		web.setWebChromeClient(new WebChromeClient() {
			   public void onProgressChanged(WebView view, int progress) {
			     FoursquareLogin.this.setProgress(progress * 1000);
			   }
			 });
		
		web.setWebViewClient(new FWClient());
		web.loadUrl("https://foursquare.com/oauth2/authenticate" +
				"?client_id=PSTZSIEK4VH3BRLFDF3Z32URHTLSWWSZM22VRFMCBXCGKDHV&response_type=token" +
				"&redirect_uri=http://192.168.1.199");
		//web.loadUrl("http://192.168.1.199/#access_token=IQFTYZOA01HPC03YTTGUCFC5YMIACGU251JMBD33RBDLZY3A");
				
	}
	
	private class FWClient extends WebViewClient
	{
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if(url.contains("#"))
			{
				view.stopLoading();
				//Get Access Token and save
				//Bundle bundle = Util.getQueryParams(url);
				//http://192.168.1.199/#access_token=IQFTYZOA01HPC03YTTGUCFC5YMIACGU251JMBD33RBDLZY3A

				String splt[] = url.split("[#]", 2);

				String accessToken = splt[1].replace("access_token=", "");
				if(Util.mFoursquare == null)
					Util.mFoursquare = new Foursquare();
				Util.mFoursquare.setToken(accessToken);
				
				SessionManager.saveFoursquare(Util.mFoursquare, mContext);
				
				Util.log(accessToken);
				setResult(RESULT_OK);
				finish();
				
				return;
			}
			super.onPageStarted(view, url, favicon);
			
		}
		
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			Toast.makeText(getParent(), "Error Displaying foursquare Login", 3000).show();
			super.onReceivedError(view, errorCode, description, failingUrl);
		}
		
		
	}
}
