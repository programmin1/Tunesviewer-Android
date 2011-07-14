package com.tunes.viewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

public class TunesViewerActivity extends Activity {

	private final String TAG = "Main";
	private static Context _AppContext;
	private WebView _web;
	//private DownloadViewer myDownloader;
	private MyWebViewClient _myWVC;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		_AppContext = getApplicationContext();
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//set(R.id.mainWebView) = new WebView(this);
		_web = (WebView) findViewById(R.id.mainWebView);
		WebSettings s = _web.getSettings();
		s.setJavaScriptEnabled(true);
		s.setPluginsEnabled(true);
		s.setUserAgentString("iTunes/10.2");
		s.setSupportZoom(true);
		s.setBuiltInZoomControls(true);
		s.setUseWideViewPort(false); //disables horizontal scroll

		_myWVC =  new MyWebViewClient(getApplicationContext(),this,_web);
		_web.addJavascriptInterface(new JSInterface(this), "DOWNLOADINTERFACE");
		_web.setWebViewClient(_myWVC);
		_web.setWebChromeClient(new MyWebChromeClient(this));
		_web.requestFocus(View.FOCUS_DOWN);
		if (this.getIntent().getData()==null) { //no specified url.
			_myWVC.shouldOverrideUrlLoading(_web, "http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}
	
	@Override
	public void onLowMemory() {
		Log.d(TAG,"LOW MEMORY");
		_web.clearHistory();//Not needed, we have our own back/forward stacks.
		_web.clearCache(true);
		super.onLowMemory();
	}
	
	@Override
	protected void onPause() {
		_web.pauseTimers();
		super.onPause();
	}
	@Override
	protected void onResume() {
		_web.resumeTimers();
		super.onResume();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem forward = menu.findItem(R.id.menuForward);
		if (_web != null && forward != null) {
			forward.setEnabled(_myWVC.canGoForward());
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean debugMode = (prefs!= null && prefs.getBoolean("debug", false));
		menu.findItem(R.id.menuSource).setVisible(debugMode);
		menu.findItem(R.id.menuOriginalSource).setVisible(debugMode);
		menu.findItem(R.id.menuCookie).setVisible(debugMode);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle menu
		switch (item.getItemId()) {
		case R.id.search:
			Intent intent = new Intent(TunesViewerActivity.this,Searcher.class);
			startActivity(intent);
			return true;
		case R.id.menuRefresh:
			_myWVC.refresh();
			return true;
		case R.id.menuCopy:
			ClipboardManager c = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			String url = _web.getUrl();
			c.setText(url);
			Toast.makeText(_AppContext, "Copied "+url, 4000).show();
			return true;
		case R.id.menuForward:
			_myWVC.goForward();
			return true;
		case R.id.menuClear:
			_web.clearHistory();
			_web.clearCache(true);
			_myWVC.clearInfo();
			return true;
		case R.id.menuOriginalSource:
			final String source = _myWVC.getOriginal();
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Original Source ("+source.length()+" chars)")
			.setMessage(source)
			.setPositiveButton("Copy Text", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager c = (ClipboardManager)getSystemService(getApplicationContext().CLIPBOARD_SERVICE);
					c.setText(source);
				}
			})
			.setNegativeButton("Close", null)
			.show();
			return true;
		case R.id.menuSource:
			_web.loadUrl("javascript:window.DOWNLOADINTERFACE.source(document.documentElement.innerHTML)");
			return true;
		case R.id.menuCookie:
			final String cookies = _myWVC.getCookies();
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Cookies")
			.setMessage(cookies)
			.setPositiveButton("Copy Text", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager c = (ClipboardManager)getSystemService(getApplicationContext().CLIPBOARD_SERVICE);
					c.setText(cookies);
				}
			})
			.setNegativeButton("Close", null)
			.show();
			return true;
		case R.id.go:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Goto");
			alert.setMessage("Enter a url or javascript:script:");
			final EditText input = new EditText(this);
			alert.setView(input);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				  String value = input.getText().toString();
				  if (value.startsWith("javascript")) {
					  _web.loadUrl(value);
				  } else {
					  _myWVC.shouldOverrideUrlLoading(_web, value);
				  }
			  }
			});
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			alert.show();
			return true;
		case R.id.home:
			Log.d(TAG,"HOMEPAGE");
			//_web.loadUrl("http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");
			_myWVC.shouldOverrideUrlLoading(_web, "http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");
			return true;
		case R.id.menuPrefs:
			startActivity(new Intent(this,PrefsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Loads a url into this activity's WebView. This should be safe for any url, and usable from other threads.
	 * @param url
	 */
	public void loadUrl(String url) {
		final String u = url;
		_web.post(new Runnable() {
			@Override
			public void run() {
				_myWVC.shouldOverrideUrlLoading(_web, u);
			}
		});
	}
	
	public static Context getContext() {
		return _AppContext;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Uri uri = this.getIntent().getData();
		if (uri!=null) {
			_web.stopLoading();
			String u = uri.toString();//fetchUri(uri);
			if (u.substring(0, 4).equals("itms")) {
				u = "http"+u.substring(4);
			}
			Log.d("Loading url: ", u);
			_myWVC.shouldOverrideUrlLoading(_web, u);
			// Clear data so it won't go here again after another activity runs, and user returns here.
			this.getIntent().setData(null);
		}
		//_web.requestFocus(View.FOCUS_DOWN);
   }

	@Override
	protected void onDestroy() {
		_web.destroy();
		super.onDestroy();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && _myWVC.canGoBack()) {
			_myWVC.goBack();
			return true;
		} else if ((keyCode == KeyEvent.KEYCODE_SEARCH)) {
			Intent intent = new Intent(TunesViewerActivity.this,Searcher.class);
			startActivity(intent);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
}