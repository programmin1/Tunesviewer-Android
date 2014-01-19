package com.tunes.viewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tunes.viewer.Bookmarks.BookmarksActivity;
import com.tunes.viewer.FileDownload.DownloadService;
import com.tunes.viewer.WebView.JSInterface;
import com.tunes.viewer.WebView.MyWebChromeClient;
import com.tunes.viewer.WebView.MyWebViewClient;

/**
 * Tunesviewer main activity.
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
 *
 */
@TargetApi(3)
public class TunesViewerActivity extends Activity {

	private static final float DIALOGTEXTSIZE = 12;
	private final String TAG = "Main";
	private static Context _AppContext;
	private WebView _web;
	//private DownloadViewer myDownloader;
	private MyWebViewClient _myWVC;
	private String originalUA;
	final String UA = "iTunes/10.6.1 ";
	private MyReceiver _receiver;
	
	/**
	 * Handy reference for WebViewClient to set menu state.
	 */
	public Menu mainmenu;
	
	@TargetApi(7)
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
		originalUA = s.getUserAgentString();
		if (originalUA.indexOf("AppleWebKit") > -1) {
			/*Must have webkit-version-info or else there will be many
			variable 'a' not found errors on the 'mouseover' of download links in full version,
			which seems to trigger a bug and crash webkit in 2.2.2 AppleWebKit/533.1?*/
			s.setUserAgentString(UA+originalUA.substring(originalUA.indexOf("AppleWebKit")));
		} else {
			s.setUserAgentString(UA);
		}
		s.setJavaScriptEnabled(true);
		s.setJavaScriptCanOpenWindowsAutomatically(true);//Disable popup-blocker interfering with window.open() interface:// commands.
		s.setPluginsEnabled(true);
		s.setSupportZoom(true);
		s.setBuiltInZoomControls(true);
		s.setUseWideViewPort(false); //disables horizontal scroll
		if (Build.VERSION.SDK_INT >= 8) {
			_web.getSettings().setDomStorageEnabled(true);
		}
		
		_myWVC =  new MyWebViewClient(getApplicationContext(),this,_web);
		//_web.addJavascriptInterface(new JSInterface(this), "DOWNLOADINTERFACE"); bad!
		_web.setWebViewClient(_myWVC);
		_web.setWebChromeClient(new MyWebChromeClient(this));
		_web.requestFocus(View.FOCUS_DOWN);
		// Page title in case it doesn't load, won't stick on "Loading..." Doesn't show in page for some reason.
		_web.loadUrl("javascript:function setTitle() {window.DOWNLOADINTERFACE.setTitle(\""
				+getString(R.string.PageNotDownloaded)+
				"\");document.write(\""
				+getString(R.string.PageNotDownloaded)+
				"\"); }");
		_web.loadData("<html><body><script>function setTitle() {window.DOWNLOADINTERFACE.setTitle('No page loaded');}</script>blank page</body></html>",
				"text/html", "UTF-8");
		
		if (this.getIntent().getData()==null) { //no specified url.
			_myWVC.shouldOverrideUrlLoading(_web, "http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");// redirects https
		}
		((EditText)findViewById(R.id.editFind)).addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				_web.computeScroll();
				if (s.toString().equals("")) {
					_web.clearMatches();
				} else {
					_web.findAll(s.toString());
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		findViewById(R.id.findPrevious).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				_web.findNext(false);
			}
		});
		findViewById(R.id.findNext).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				_web.findNext(true);
			}
		});
	}
	
	public String geturl() {
		return _web.getUrl();
	}
	
	public void hideSearch() {
		_web.clearMatches();
		findViewById(R.id.findLayout).setVisibility(View.GONE);
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(findViewById(R.id.editFind).getWindowToken(), 0);
		try
		{
		    Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
		    m.invoke(_web, false);
		}
		catch (Throwable ignored){}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		mainmenu = menu;
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
		unregisterReceiver(_receiver);
		_web.pauseTimers();
		super.onPause();
	}

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter(DownloadService.DOWNLOADBROADCAST);
		_receiver = new MyReceiver(this);
		registerReceiver(_receiver, filter);
		_web.resumeTimers();
		super.onResume();
		// Might have been deleted/added, check:
		Intent doneintent = new Intent(DownloadService.DOWNLOADBROADCAST);
		if (_web.getUrl() !=null && getTitle() != null) {
			doneintent.putExtra(MyReceiver.PAGEURL, _web.getUrl());
			doneintent.putExtra(MyReceiver.NAME, getTitle());
			sendBroadcast(doneintent);
		}
		
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem forward = menu.findItem(R.id.menuForward);
		if (_web != null && forward != null) {
			forward.setEnabled(_myWVC.canGoForward());
			menu.findItem(R.id.menuStop).setVisible(_myWVC.isLoading());
			menu.findItem(R.id.menuRefresh).setVisible(!_myWVC.isLoading());
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean debugMode = (prefs!= null && prefs.getBoolean("debug", false));
		menu.findItem(R.id.menuSource).setVisible(debugMode);
		menu.findItem(R.id.menuOriginalSource).setVisible(debugMode);
		menu.findItem(R.id.menuCookie).setVisible(debugMode);
		menu.findItem(R.id.menuFirebug).setVisible(debugMode);
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
		case R.id.menuStop:
			_myWVC.stop();
			return true;
		case R.id.menuShare:
			String url = _web.getUrl();
			if (url.startsWith("http")) {
				url = "itms"+url.substring(4);
			}
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("text/plain");
			share.putExtra(Intent.EXTRA_TEXT, url);
			share.putExtra(Intent.EXTRA_SUBJECT, getTitle());
			share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			share = Intent.createChooser(share, getString(R.string.share));
			startActivity(share);
			return true;
		case R.id.menuForward:
			if (_myWVC.canGoForward()) {
				_myWVC.goForward();
			}
			return true;
		case R.id.menuClear:
			_web.clearHistory();
			_web.clearCache(true);
			_myWVC.clearInfo();
			Toast.makeText(this, R.string.clearedData, Toast.LENGTH_LONG).show();
			return true;
		case R.id.menuFindText:
			//Show hidden text so it will be searched:
			_web.loadUrl("javascript:divs = document.getElementsByTagName('div'); for (i=0; i<divs.length; i++) {divs[i].style.display='block';}");
			findViewById(R.id.findLayout).setVisibility(View.VISIBLE);
			findViewById(R.id.findLayout).requestFocus(View.FOCUS_DOWN);
			_web.clearMatches();
			_web.findAll(((EditText)findViewById(R.id.editFind)).getText().toString());
			try
			{
			    Method m = WebView.class.getMethod("setFindIsUp", Boolean.TYPE);
			    m.invoke(_web, true);
			}
			catch (Throwable ignored){}

			return true;
		case R.id.menuOriginalSource:
			final String source = _myWVC.getOriginal();
			AlertDialog dialog = new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Original Source ("+source.length()+" chars)")
			.setMessage(source)
			.setPositiveButton("Copy Text", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager c = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
					c.setText(source);
				}
			})
			.setNegativeButton("Close", null)
			.show();
			TextView text = (TextView) dialog.findViewById(android.R.id.message);
			text.setTextSize(DIALOGTEXTSIZE);
			return true;
		case R.id.menuSource:
			_web.loadUrl("javascript:window.DOWNLOADINTERFACE.source(document.documentElement.innerHTML)");
			return true;
		case R.id.menuFirebug:
			// Based on Firebug Lite here: https://getfirebug.com/firebuglite#Stable
			// with %20 replaced with a space.
			_web.loadUrl("javascript:(function(F,i,r,e,b,u,g,L,I,T,E){if(F.getElementById(b))return;E=F[i+'NS']&&F.documentElement.namespaceURI;E=E?F[i+'NS'](E,'script'):F[i]('script');E[r]('id',b);E[r]('src',I+g+T);E[r](b,u);(F[e]('head')[0]||F[e]('body')[0]).appendChild(E);E=new Image;E[r]('src',I+L);})(document,'createElement','setAttribute','getElementsByTagName','FirebugLite','4','firebug-lite.js','releases/lite/latest/skin/xp/sprite.png','https://getfirebug.com/','#startOpened');");
			return true;
		/*case R.id.menuSecurity:
			InputStream inputStream = getResources().openRawResource(R.raw.securitytest);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			int i;
			try { // Read the Javascript file into memory.
				i = inputStream.read();
				while (i != -1) {
					byteArrayOutputStream.write(i);
					i = inputStream.read();
				}
				inputStream.close();
				String filecontent = byteArrayOutputStream.toString();
				//_web.loadUrl("data:text/html;base64,"+Base64.encodeToString(filecontent.getBytes("UTF-8"), Base64.DEFAULT));
				_web.loadDataWithBaseURL("http://example.com", filecontent, "text/html", "utf-8", null);//("data:text/html,"+filecontent);
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(this, "Couldn't load test", Toast.LENGTH_LONG).show();
			}
			return true;*/
		case R.id.menuCookie:
			final String cookies = _myWVC.getCookies();
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Cookies")
			.setMessage(cookies)
			.setPositiveButton("Copy Text", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager c = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
					c.setText(cookies);
				}
			})
			.setNegativeButton("Close", null)
			.show();
			return true;
		case R.id.go:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Location");
			alert.setMessage("Enter a url or javascript:script:");
			final EditText input = new EditText(this);
			input.setText(_web.getUrl());
			input.selectAll();
			alert.setView(input);
			alert.setPositiveButton("Goto", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				  String value = input.getText().toString();
				  if (value.startsWith("javascript")) {
					  _web.loadUrl(value);
				  } else {
					  _myWVC.shouldOverrideUrlLoading(_web, value);
				  }
			  }
			});
			alert.setNegativeButton("Cancel", null);
			alert.show();
			return true;
		case R.id.menuBookmark:
			Intent Bkmarks = new Intent(this,BookmarksActivity.class);
			Bkmarks.putExtra("title", getTitle());
			Bkmarks.putExtra("url", _web.getUrl());
			startActivity(Bkmarks);
			return true;
		case R.id.home:
			Log.d(TAG,"HOMEPAGE");
			//_web.loadUrl("http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");
			_myWVC.shouldOverrideUrlLoading(_web, "http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753");
			return true;
		/*case R.id.menuShow:
			 Intent i = new Intent();
			   i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			   i.setData(Uri.parse("file://sdcard/i"));
			   i.setAction(Intent.ACTION_VIEW);
			   startActivity(Intent.createChooser(i, "SelectFolder"));
			return true;*/
		case R.id.menuPrefs:
			startActivity(new Intent(this,PrefsActivity.class));
			return true;
		case R.id.menuAbout:
			String versionName;
			try {
				versionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0 ).versionName;
				_myWVC.shouldOverrideUrlLoading(_web, "http://tunesviewer.sourceforge.net/checkversionmobile.php?version="+versionName);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	}

	@Override
	protected void onDestroy() {
		_web.destroy();
		super.onDestroy();
	}
	
	/**
	 * An onKeyDown listener for back (last page) and search (search).
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (findViewById(R.id.findLayout).getVisibility() == View.VISIBLE) {
				hideSearch();
				return true;
			} else if (_myWVC.canGoBack()) {
				_myWVC.goBack();
				return true;
			}
		} else if ((keyCode == KeyEvent.KEYCODE_SEARCH)) {
			Intent intent = new Intent(TunesViewerActivity.this,Searcher.class);
			startActivity(intent);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public WebView getWeb() {
		return _web;
	}
	
	/*@Override
	public void onBackPressed() {
		if (_myWVC.canGoBack()) {
			_myWVC.goBack();
		} else {
			//super.onBackPressed();
		}
	}*/
	
}