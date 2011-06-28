package com.tunes.viewer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import android.app.Activity;
import android.text.ClipboardManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		_web = (WebView) findViewById(R.id.mainWebView);
		WebSettings s = _web.getSettings();
		s.setJavaScriptEnabled(true);
		s.setPluginsEnabled(true);
		s.setUserAgentString("iTunes/10.2");
		s.setSupportZoom(true);
		s.setBuiltInZoomControls(true);
		s.setUseWideViewPort(true); //enables double tap

		_web.addJavascriptInterface(new MyJavaScriptInterface(getApplicationContext()), "DOWNLOADINTERFACE");
		_myWVC =  new MyWebViewClient(getApplicationContext(),this);
		_web.setWebViewClient(_myWVC);
		_web.setWebChromeClient(new MyWebChromeClient(this));
		Log.d(TAG,"SETUP Done");

		_web.requestFocus(View.FOCUS_DOWN);
		Log.d(TAG,"HOMEPAGE");
		//_web.loadUrl("http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");
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
		_web.clearCache(true);
		super.onLowMemory();
	}
	
	/**
	 * Returns current url of the webview.
	 * @return String url caught with shouldOverrideUrlLoading.
	 * @throws UnsupportedEncodingException 
	 */
	public String getCurrentUrl() throws UnsupportedEncodingException {
		String url = _web.getUrl();
		// Reverse the <!-- url --> comment added with loaddata.
		try {
		url = url.substring(url.indexOf("%3C%21--%20")+11, url.indexOf("%20--%3E"));
		url = URLDecoder.decode(url,"utf-8");
		} catch (NullPointerException e) {
			throw new UnsupportedEncodingException();
		}
		return url;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem forward = menu.findItem(R.id.menuForward);
		if (_web != null && forward != null) {
			forward.setEnabled(_web.canGoForward());
		}
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
			try {
				_myWVC.shouldOverrideUrlLoading(_web, getCurrentUrl());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.menuCopy:
			ClipboardManager c = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			try {
				String url = getCurrentUrl();
				c.setText(url);
				Toast.makeText(_AppContext, "Copied "+url, 4000).show();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.menuForward:
			_web.goForward();
			return true;
		case R.id.menuClear:
			_web.clearHistory();
			_web.clearCache(true);
			_myWVC.clearCookies();
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
			  //_web.loadUrl(value);
			  _myWVC.shouldOverrideUrlLoading(_web, value);
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
			Log.d("url", u);
			_myWVC.shouldOverrideUrlLoading(_web, u);
			//_web.loadUrl(u);
		}
		_web.requestFocus(View.FOCUS_DOWN);
   }

	@Override
	protected void onDestroy() {
		_web.destroy();
		super.onDestroy();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && _web.canGoBack()) {
			_web.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}


class MyJavaScriptInterface {
	private Context _context;
	
	public MyJavaScriptInterface(Context c) {
		_context = c;
	}
	
	public void download(String title, String url) {
		Intent intent = new Intent(_context,DownloadService.class);
		intent.putExtra("url", url);
		intent.putExtra("name",title);
		_context.startService(intent);
	}
	
	public void source(String src) {
		//Log.d("source:",src);
	}
}