package com.tunes.viewer.WebView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.tunes.viewer.ItunesXmlParser;
import com.tunes.viewer.MyReceiver;
import com.tunes.viewer.R;
import com.tunes.viewer.TunesViewerActivity;
import com.tunes.viewer.FileDownload.DownloadService;
import com.tunes.viewer.FileDownload.DownloaderTask;

public class MyWebViewClient extends WebViewClient {
	
	//One WebLoader at a time.
	private Executor executor = Executors.newSingleThreadExecutor();
	
	private final int BACK = -1;
	private final int REFRESH = 0;
	private final int FORWARD = 1;
	private final int NEWURL = 2;
	
	// True when loading, can be set to false to cancel (See makeString function)
	private boolean isLoading;
	
	private final String TAG = "WVC";
	private Context callerContext;
	private TunesViewerActivity activity;
	private IansCookieManager _CM = new IansCookieManager();
	private SharedPreferences _prefs;
	private String _originalDownload = "";
	private String _javascript; //from res/raw/javascript.js
	private String mobileExtras;
	
	//Back and Forward navigation stacks:
	private Stack<String> Back = new Stack<String>();
	private Stack<String> Forward = new Stack<String>();
	private WebView _web;
	private JSInterface _interface;
	
	public MyWebViewClient (Context c, TunesViewerActivity a, WebView v) {
		callerContext = c;
		activity = a;
		_interface = new JSInterface(a);
		_prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		_web = v;
		InputStream inputStream = c.getResources().openRawResource(R.raw.javascript);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int i;
		try { // Read the Javascript file into memory.
			i = inputStream.read();
			while (i != -1) {
				byteArrayOutputStream.write(i);
				i = inputStream.read();
			}
	      inputStream.close();
	      _javascript = byteArrayOutputStream.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(c, "Couldn't open Javascript.js", Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Returns the original source of the current document.
	 * @return string.
	 */
	public String getOriginal() {
		return _originalDownload.toString();
	}
	
	/**
	 * Stops current downloading of page.
	 */
	public void stop() {
		isLoading = false;
	}
	
	/**
	 * Returns true when page is loading.
	 * @return
	 */
	public boolean isLoading() {
		return isLoading;
	}
	
	/**
	 * Returns true when goBack() will work.
	 * @return
	 */
	public boolean canGoBack() {
		return (Back.size()>0);
	}
	
	/**
	 * Goes backward, or causes exception if canGoBack() is false.
	 */
	public void goBack() {
		//Forward.push(_web.getUrl());
		//String url = Back.pop();
		//new Thread(new WebLoader(_web,Back.peek(),this,BACK)).start();
		if (!isLoading) {
			executor.execute(new WebLoader(_web,Back.peek(),this,BACK));
		}
	}
	
	/**
	 * Returns true when goForward() will work.
	 * @return
	 */
	public boolean canGoForward() {
		return (Forward.size()>0);
	}
	
	/**
	 * Goes forward, or causes exception is canGoForward() is false.
	 */
	public void goForward() {
		//Back.push(_web.getUrl());
		//new Thread(new WebLoader(_web,Forward.peek(),this,FORWARD)).start();
		if (!isLoading) {
			executor.execute(new WebLoader(_web,Forward.peek(),this,FORWARD));
		}
	}
	
	/**
	 * Refreshes the WebView, no change to back/forward stack.
	 */
	public void refresh() {
		if (_web.getUrl() != null) {
			//new Thread(new WebLoader(_web,_web.getUrl(),this,0)).start();
			if (!isLoading) {
				executor.execute(new WebLoader(_web,_web.getUrl(),this,REFRESH));
			}
		}
	}
	
	/**
	 * Clears the history and the cookie handler.
	 */
	public void clearInfo() {
		_CM = new IansCookieManager();
		Back.clear();
		Forward.clear();
	}
	
	/**
	 * Called on page load, inserts the javascript required to catch download/preview clicks, etc.
	 */
	@Override
	public void onPageFinished(WebView view, String url) {
		Log.d(TAG,"Inserting script into "+url);
		
		//Since Forward is shown always on actionbar of newer Android, enable/disable as needed:
		if (activity.mainmenu != null) {
			activity.onPrepareOptionsMenu(activity.mainmenu);
		}
		
		// When this line is commented out, many download links wouldn't work,
		// many preview links don't go through to the JSInterface, and other display
		// problems (in mobile mode, html).
		// When it is uncommented, page displays and works fine, but preview
		// is called twice when the preview (number to the left in row) is pressed.
		// view.loadUrl("javascript:"+_javascript);
		//
		// This should be fixed with the DOMContentLoaded event in Javascript.
		
		setUrl(view, "javascript:"+_prefs.getString("extraScript", ""));
		
		if (activity.findViewById(R.id.menuForward)!=null) {
			activity.findViewById(R.id.menuForward).setClickable(view.canGoForward());
		}
		Intent doneintent = new Intent(DownloadService.DOWNLOADBROADCAST);
		doneintent.putExtra(MyReceiver.PAGEURL, url);
		doneintent.putExtra(MyReceiver.NAME, DownloaderTask.clean((String) activity.getTitle()));
		activity.sendBroadcast(doneintent);
	}
	
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		activity.hideSearch();
		super.onPageStarted(view, url, favicon);
	}

	/**
	 * Determines load behavior on "click".
	 */
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		Log.d(TAG,"shouldOverrideUrlLoading");
		if (url.startsWith("copyurl://")) { // JS Copy-clipboard interface
			System.out.println("COPY "+url);
			ClipboardManager clipboard = (ClipboardManager)callerContext.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(url.substring(10));
		} else if (url.startsWith("interface://")) {
			try {
				JSONObject json = new JSONObject(URLDecoder.decode(url.substring(12)));
				String cmd = json.getString("cmd");
				if (cmd.equals("go")) {
					_interface.go(json.getString("url"));
				} else if (cmd.equals("download")) {
					_interface.download(json.getString("title"), json.getString("podcast"), json.getString("url"));
				} else if (cmd.equals("subscribe")) {
					_interface.subscribe(json.getString("url"));
				} else if (cmd.equals("source")) {
					_interface.source(json.getString("src"));
				} else if (cmd.equals("preview")) {
					_interface.preview(json.getString("title"), json.getString("url"));
				} else if (cmd.equals("setTitle")) {
					_interface.setTitle(json.getString("title"));
				}
			} catch (JSONException e) {
				throw new RuntimeException();
			}
			
		} else if (url.startsWith("download://")) {// a download description.
			String xml = URLDecoder.decode(url.substring(11));
			try {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setValidating(false);
				SAXParser saxParser= factory.newSAXParser();
				ItunesXmlParser parser = new ItunesXmlParser(
						new URL(view.getOriginalUrl()),callerContext,55,55);
				XMLReader xr = saxParser.getXMLReader();
				xr.setContentHandler(parser);
				InputSource is = new InputSource(new StringReader(xml));
				long startMS = System.currentTimeMillis();
				xr.parse(is);
				long endMS = System.currentTimeMillis();
				Log.i(TAG,"DOWNLOAD PARSING XML TOOK "+(endMS-startMS)+" MS.");
				if (parser.getUrls().size()==1) {
					Intent intent = new Intent(callerContext,DownloadService.class);
					intent.putExtra(DownloadService.EXTRA_URL, parser.getUrls().get(0));
					intent.putExtra(DownloadService.EXTRA_PODCAST, parser.getTitle());
					intent.putExtra(DownloadService.EXTRA_PODCASTURL, view.getUrl());
					intent.putExtra(DownloadService.EXTRA_ITEMTITLE,parser.getSingleName());
					callerContext.startService(intent);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else { // Normal page load:
			String ua = _prefs.getString("UserAgent", callerContext.getString(R.string.defaultUA));
			System.setProperty("http.agent", ua);
			//view.getSettings().setUserAgentString(ua); may cause crash
			view.stopLoading();//stop previous load.
			activity.setTitle(R.string.loading);
			//new Thread(new WebLoader(view,url,this,0)).start();
			executor.execute(new WebLoader(view,url,this,NEWURL));
		}
		return true;//Always using our own loader
	}
	
	/**
	 * Trust every server - don't check for any certificate
	 */
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new java.security.cert.X509Certificate[] {};
				}

				public void checkClientTrusted(X509Certificate[] chain,
								String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain,
								String authType) throws CertificateException {
				}
		} };

		// Install the all-trusting trust manager
		try {
				SSLContext sc = SSLContext.getInstance("TLS");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection
								.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
				e.printStackTrace();
		}
	}
	

	// always verify the host - don't check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
	/* No longer needed, without loadData()
	private String encode(String text) {
		//workaround for: https://code.google.com/p/android/issues/detail?id=4401
		long start = System.currentTimeMillis();
		//String out = URLEncoder.encode(text).replaceAll("\\+"," ");
		//FastURLEncoder is around 5x faster!
		String out = FastURLEncoder.encode(text).replaceAll("\\+"," ");
		long end = System.currentTimeMillis();
		Log.d(TAG,"ENCODE TOOK "+(end-start)+" MS.");
		
		return out;
	}*/
	
	/**
	 * Turns an InputStream into a String.
	 * @param in
	 * @return String value.
	 * @throws IOException
	 */
	private StringBuilder makeString(InputStream is, int totalLength) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is),1024*8);
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
			if (!isLoading) {//stopped
				
				throw new IOException("Stopped.");
			}
			/* Doesn't work here for some reason.
			length += line.length()+1;
			final int prog = (length*100)/totalLength;
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activity.setProgress(prog);
				}
			});*/
		}
		is.close();
		return sb;
	}
	
	/**
	 * Returns string description of cookies.
	 * @return IansCookieManger.toString().
	 */
	public String getCookies() {
		return _CM.toString();
	}
	
	/**
	 * Page loading thread
	 */
	private class WebLoader implements Runnable {
		private WebView _view;
		private String _url;
		private StringBuilder _download;
		//private String previousURL;
		private int _cmd; // Back/forward
		private MyWebViewClient caller;
		/**
		 * Constructs runnable that will download URL u and display in Webview v.
		 * @param v
		 * @param u
		 * @param caller
		 * @param cmd int FORWARD/BACK or 0 no change
		 */
		public WebLoader(WebView v,String u, MyWebViewClient caller, int cmd) {
			_cmd = cmd;
			_view = v;
			_url = u;
			//previousURL = v.getUrl();
			this.caller = caller;
			activity.setProgressBarIndeterminateVisibility(true);
		}
		
		/**
		 * Loads url into view, or downloads the file specified.
		 * @return true if it worked, no redirect required.
		 * @throws IOException 
		 * @throws FactoryConfigurationError 
		 * @throws SAXException 
		 * @throws ParserConfigurationException 
		 */
		private boolean load() throws IOException, ParserConfigurationException, SAXException, FactoryConfigurationError {
			int length;
			boolean worked = false;
			SharedPreferences myprefs = PreferenceManager.getDefaultSharedPreferences(callerContext);
			if (_url.substring(0, 4).equals("itms")) {
				_url = "http"+_url.substring(4);
			}
			URL u = new URL(_url);
			
			int tries = 4;
			int code = 301;
			URLConnection conn = null;
			// Handles HTTP-level redirects:
			while (tries > 0 && code/100 == 3) {
				conn = null;
				if (myprefs.getBoolean("SSLTrustAll", false)) {
					trustAllHosts();
				}
				conn = u.openConnection();
				_CM.setCookies(conn);
				conn.addRequestProperty("Accept-Encoding", "gzip");
				((HttpURLConnection)conn).setInstanceFollowRedirects(false);
				code =((HttpURLConnection)conn).getResponseCode();
				if (code/100 == 3) {
					String loc = conn.getHeaderField("Location");
					u = new URL(loc);
				} else {
					// try:
					conn.connect();
					//Map<String, List<String>> returnv = conn.getHeaderFields();
					_CM.storeCookies(conn);
					if (conn.getContentType()==null) {
						code = 300;
					}
				}
			}
			
			
			/*if (loc != null) { // Works in Android 4.1, maybe others?
				conn = new URL(loc).openConnection();
				((HttpURLConnection)conn).setInstanceFollowRedirects(false);
				Log.w(TAG,""+((HttpURLConnection)conn).getResponseCode());
				loc = conn.getHeaderField("Location");
				conn = new URL(loc).openConnection();
			} else if (conn.getContentType()==null && _url.startsWith("http://")) { //old android workaround
				conn = new URL(_url.replaceFirst("http://", "https://")).openConnection();
				trustAllHosts();
				Log.w(TAG, "Null content type, that looks like the redirect bug.");
			}*/
			Log.d(TAG,"mime: "+conn.getContentType());
			length = conn.getContentLength();
			if (conn.getContentType()!=null) {
				if (conn.getContentType().startsWith("text")) {
					InputStream input = conn.getInputStream();
		
					if ("gzip".equals(conn.getContentEncoding())) {
						input = new GZIPInputStream(input);
					}
					// Download xml/html to parse:
					_download = makeString(input,length); 
					if (myprefs.getBoolean("debug", false)) {
						// When debug menus are open, save the string
						// (although this might not be so good for memory use?)
						synchronized (caller) {
							caller._originalDownload = _download.toString();
						}
					}
					// Remove unneeded XML declaration that may cause errors on some pages:
					//_download = _download.replace("<?","<!--").replace("?>", "-->");
					int badindex = _download.indexOf("<?");
					if (badindex != -1) {
						_download.replace(badindex, badindex+2, "<!--");
					}
					badindex = _download.indexOf("?>");
					if (badindex != -1) {
						_download.replace(badindex, badindex+2, "-->");
					}
					
					SAXParserFactory factory = SAXParserFactory.newInstance();
					factory.setValidating(false);
					SAXParser saxParser= factory.newSAXParser();
					ItunesXmlParser parser = new ItunesXmlParser(
						u,callerContext,_view.getWidth()
						,Integer.valueOf(myprefs.getString("ImgPref", "0")));
					mobileExtras = parser.mobileExtras;
					XMLReader xr = saxParser.getXMLReader();
					xr.setContentHandler(parser);
					InputSource is = new InputSource(new StringReader(_download.toString()));
					long startMS = System.currentTimeMillis();
					xr.parse(is);
					long endMS = System.currentTimeMillis();
					Log.i(TAG,"PARSING XML TOOK "+(endMS-startMS)+" MS.");
					if (parser.getRedirect().equals("")) {
						// No redirect for this page
						if (parser.getUrls().size()==1) {
							// Single-file description, call downloader:
							Log.d(TAG,"DL "+parser.getUrls().get(0));
							Log.d(TAG,"Name "+parser.getSingleName());
							Intent intent = new Intent(callerContext,DownloadService.class);
							intent.putExtra(DownloadService.EXTRA_URL, parser.getUrls().get(0));
							intent.putExtra(DownloadService.EXTRA_PODCAST, parser.getTitle());
							intent.putExtra(DownloadService.EXTRA_PODCASTURL, _view.getUrl());
							intent.putExtra(DownloadService.EXTRA_ITEMTITLE,parser.getSingleName());
							callerContext.startService(intent);
							// reset "loading..." because it isn't:
							setUrl(_view, "javascript:setTitle()");
						} else {
							// Inject js:
							final StringBuilder data = new StringBuilder(parser.getHTML());
							int endhead = data.indexOf("<head>")+6;
							if (endhead >-1) {
								data.replace(endhead,endhead,"<script>"+_javascript+"</script>");
							}
							_download = null;
							// Load converted html:
							synchronized (_view) {
								_view.post(new Runnable() {
									public void run() {
										prepareView(_view,_cmd);
										_view.loadDataWithBaseURL(_url,data.toString(),"text/html","UTF-8",_url);
										Log.d(TAG,"WebLoader Loaded into WebView.");
									}
								});
							}
						}
						worked = true;
					} else {
						// Redirect:
						_url = parser.getRedirect();
						worked = false;
					}
				} else if (conn.getContentType().equals("application/json")) {
					// Probably an error message, just display it:
					InputStream input = conn.getInputStream();
					
					if ("gzip".equals(conn.getContentEncoding())) {
						input = new GZIPInputStream(input);
					}
					// Download:
					final StringBuilder error = makeString(input,length);
					synchronized (_view) {
						_view.post(new Runnable() {
							public void run() {
								Log.e(TAG,error.toString());
								Toast.makeText(caller.callerContext, 
										error, Toast.LENGTH_LONG).show();
							}
						});
					}
					worked = true;// download worked
					//Set title back to normal.
					setUrl(_view, "javascript:setTitle()");
				} else { //non text url, send to downloader.
					try {
						Log.e(TAG,"Non text");
						Intent intent = new Intent(caller.callerContext,DownloadService.class);
						intent.putExtra(DownloadService.EXTRA_URL, _url);
						intent.putExtra(DownloadService.EXTRA_PODCAST, "");
						intent.putExtra(DownloadService.EXTRA_PODCASTURL, _view.getUrl());
						intent.putExtra(DownloadService.EXTRA_ITEMTITLE, _url);
						caller.callerContext.startService(intent);
						worked = true;
						//Set title back to normal.
						setUrl(_view, "javascript:setTitle()");
					} catch (ActivityNotFoundException e) {
						_view.post(new Runnable() {
							public void run() {
								Toast.makeText(callerContext, R.string.NoActivity, Toast.LENGTH_LONG).show();
							}
						});
					}
				}
			} else {
				throw new IOException("Try again, could not load url. Null content type, for download: "+_url);
			}
			return worked;
		}
		
		@Override
		public void run() {
			synchronized(_view) {
				isLoading = true;
				boolean worked;
				try { // page level redirecting/retry:
					worked = load();
					if (!worked) {
						worked = load();
						if (!worked) { //max 2 redirects.
							load();
						}
					}
				} catch (SSLException e) {
					activity.runOnUiThread(new Runnable() {
						public void run() {
						/*AlertDialog.Builder msg = new AlertDialog.Builder(callerContext.getApplicationContext());
						msg.setMessage(R.string.SSLError)
						.setPositiveButton("ok", null)
						.setNegativeButton("oh", null)
						.create()
						.show();*/
							for (int i=0; i < 3; i++) {
								Toast.makeText(callerContext, R.string.SSLError, Toast.LENGTH_LONG).show();
							}
						}
					});
				} catch (IOException e) {
					// Show error
					synchronized (_view) {
						final String msg = "Error: "+e.getMessage();
						activity.runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(callerContext, msg, Toast.LENGTH_LONG).show();
								setUrl(_web, "javascript:setTitle()");
							}
						});
					}
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					//Not XML, show the downloaded html directly in browser:
					synchronized (_view) {
						//final String data = _download;
						// Inject js:
						final StringBuilder data = _download;
						int endhead = data.indexOf("<head>")+6;
						if (endhead-6 != -1) {
							data.replace(endhead, endhead, "<script>"+_javascript+"</script>"+mobileExtras);
						} else {
							Log.w(TAG, "No end head tag in this page, so not preinserting!!");
						}
						_download = null;
						_view.post(new Runnable() {
							public void run() {
								prepareView(_view,_cmd);
								_view.loadDataWithBaseURL(_url,data.toString(),"text/html","UTF-8",_url);
							}
						});
					}
				} catch (FactoryConfigurationError e) {
					e.printStackTrace();
				}
				//In all cases, finish with...
				synchronized (activity) {//reset title, JS interface will change title if needed.
					activity.runOnUiThread(new Runnable() {
						public void run() {
							activity.setProgressBarIndeterminateVisibility(false);
						}
					});
				}
				isLoading = false;
			}
		}
	
		/**
		 * Called on successful load, updates back/forward stack and sets some view prefs.
		 * @param view
		 * @param cmd - int BACK, FORWARD, NEWURL or 0 for refresh.
		 */
		private void prepareView(WebView view, int cmd) {
			if (cmd != REFRESH) {
				// Update back and forward stacks.
				synchronized (Back) {
					synchronized (Forward) {
						if (cmd==BACK) {
							Forward.push(view.getUrl());
							Back.pop();
						} else if (cmd==FORWARD) {
							Back.push(view.getUrl());
							Forward.pop();
						} else if (cmd==NEWURL) {
							// Clicked link, so clear forward and add this to "back".
							Forward.clear();
							if (view.getUrl() != null && //happens on newer Android:
								!view.getUrl().equals("data:text/html,<html><body><script>function setTitle() {window.DOWNLOADINTERFACE.setTitle('No page loaded');}</script>blank page</body></html>")) {
								
								Back.push(view.getUrl());
							}
						}
					}
				}
			}
			//No need for history, this has custom history handler.
			synchronized(_view) {
				_view.clearHistory();
				//If preference set, disable image loading:
				String pref = PreferenceManager.getDefaultSharedPreferences(callerContext).getString("ImgPref", "0");
				_view.getSettings().setBlockNetworkImage(pref.equals("-1"));
			}
		}
	}
	
	private void setUrl(final WebView view, final String url) {
		view.post(new Runnable() {
			@Override
			public void run() {
				view.loadUrl(url);
			}
		});
	}

	
	/*private String makeStringold (InputStream in) throws IOException {
		StringBuilder out = new StringBuilder(1024);
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}*/

}