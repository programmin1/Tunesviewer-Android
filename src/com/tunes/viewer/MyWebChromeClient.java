package com.tunes.viewer;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Context;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

public class MyWebChromeClient extends WebChromeClient {
	
	private Activity activity;
	public MyWebChromeClient(Activity a) {
		//Called with the main activity as param 
		activity = a;
	}
	
	@Override
	public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)  
	{
	  Log.d("alert", message);
	  Toast.makeText(activity.getApplicationContext(), message, 3000).show();
	  result.confirm();
	  return true;
	};
	   
	/**
	 * Catches progress events while the html is loading.
	 */
	public void onProgressChanged(WebView view, int progress)
	{
		activity.setTitle(" Loading...");
		activity.setProgress(progress * 100);
		if(progress == 100) {
			activity.setTitle(R.string.app_name);
		}
	}
	
	// Webkit messages
	public void addMessageToConsole(String message, int lineNumber, String sourceID)  
	{
		Log.d("WebKit", sourceID + ": Line " + Integer.toString(lineNumber) + " : " + message);  
	}

}