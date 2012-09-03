package com.tunes.viewer.WebView;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

public class MyWebChromeClient extends WebChromeClient {
	
	private Activity activity;
	
	public MyWebChromeClient(Activity a) {
		//Called with the main activity as param 
		activity = a;
	}
	
	@Override
	public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result) {
		Log.d("alert", message);
		Toast.makeText(activity.getApplicationContext(), message, 3000).show();
		result.confirm();
		return true;
	}
	
	/**
	 * Catches progress events while the html is loading.
	 */
	public void onProgressChanged(WebView view, int progress) {
		//activity.setTitle(" Loading...");
		activity.setProgress(progress * 100);
		/*if(progress == 100) {
			activity.setTitle(R.string.app_name);
		}*/
	}
	
	// Webkit messages already logged with tag "Web Console".
	
	public void addMessageToConsole(String message, int lineNumber, String sourceID)  
	{
		Log.d("WebKit", sourceID + ": Line " + Integer.toString(lineNumber) + " : " + message);  
	}

}