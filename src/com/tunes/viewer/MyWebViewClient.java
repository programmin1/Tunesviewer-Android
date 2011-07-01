package com.tunes.viewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Stack;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MyWebViewClient extends WebViewClient {
		
	private final String TAG = "WVC";
	private Context callerContext;
	private Activity activity;
	private IansCookieManager _CM = new IansCookieManager();
	private SharedPreferences _prefs;
	
	//Back and Forward navigation stacks:
	private Stack<String> Back = new Stack<String>();
	private Stack<String> Forward = new Stack<String>();
	private WebView _web;
	
	public MyWebViewClient (Context c, Activity a, WebView v) {
		callerContext = c;
		activity = a;
		_prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		_web = v;
	}
	
	public boolean canGoBack() {
		return (Back.size()>0);
	}
	/**
	 * Goes backward, or causes exception if canGoBack() is false.
	 */
	public void goBack() {
		Forward.push(_web.getUrl());
		String url = Back.pop();
		new Thread(new WebLoader(_web,url)).start();
	}
	
	public boolean canGoForward() {
		return (Forward.size()>0);
	}
	/**
	 * Goes forward, or causes exception is canGoForward() is false.
	 */
	public void goForward() {
		Back.push(_web.getUrl());
		new Thread(new WebLoader(_web,Forward.pop())).start();
	}
	/**
	 * Refreshes the WebView, no change to back/forward stack.
	 */
	public void refresh() {
		new Thread(new WebLoader(_web,_web.getUrl())).start();
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
	 * Called on page load, inserts the javascript required to catch download clicks, etc.
	 */
	@Override
	public void onPageFinished(WebView view, String url) {
		Log.d(TAG,"Inserting script into "+url);
		view.loadUrl("javascript:"+callerContext.getString(R.string.Javascript));
		//view.loadUrl("javascript:window.DOWNLOADINTERFACE.source(document.documentElement.innerHTML);");
		if (activity.findViewById(R.id.menuForward)!=null) {
			activity.findViewById(R.id.menuForward).setClickable(view.canGoForward());
		}
	}
	
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		// TODO Auto-generated method stub
		super.onPageStarted(view, url, favicon);
	}
	
	/**
	 * Determines load behavior on "click".
	 * If it's HTML, this lets WebView show it, if it's special XML file, it converts it and loads it.
	 */
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		String ua = _prefs.getString("UserAgent", callerContext.getString(R.string.defaultUA));
		System.setProperty("http.agent", ua);
		view.getSettings().setUserAgentString(ua);
		//view.requestFocus(View.FOCUS_DOWN);
		view.stopLoading();
		activity.setTitle("XML Loading...");
		// Clicked link, so clear forward and add this to "back".
		Forward.clear();
		if (view.getUrl() != null) {
			Back.push(view.getUrl());
		}
		new Thread(new WebLoader(view,url)).start();
		return true;
	}
	
	// always verify the host - don't check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
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
	/* No longer needed without loadData()
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
	
	private class WebLoader implements Runnable {
		private WebView _view;
		private String _url;
		private String _download;
		public WebLoader(WebView v,String u) {
			_view = v;
			_url = u;
		}
		
		/**
		 * Loads url into view.
		 * @return true if it worked, no redirect required.
		 * @throws IOException 
		 * @throws FactoryConfigurationError 
		 * @throws SAXException 
		 * @throws ParserConfigurationException 
		 */
		private boolean load() throws IOException, ParserConfigurationException, SAXException, FactoryConfigurationError {
			boolean worked = false;
			if (_url.substring(0, 4).equals("itms")) {
				_url = "http"+_url.substring(4);
			}
			URL u = new URL(_url);
			if (u.getProtocol().toLowerCase().equals("https")) {
				//TODO: This is ugly
				trustAllHosts(); // stop javax.net.ssl.SSLException: Not trusted server certificate
			}
			URLConnection conn = u.openConnection();
			_CM.setCookies(conn);
			conn.connect();
			_CM.storeCookies(conn);
			
			// Download xml/html to parse:
			_download = makeString(conn.getInputStream()); 
			// Remove unneeded XML declaration that may cause errors on some pages:
			_download = _download.replace("<?","<!--").replace("?>", "-->");
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser saxParser= factory.newSAXParser();
			ItunesXmlParser parser = new ItunesXmlParser(u.getRef(),callerContext);
			XMLReader xr = saxParser.getXMLReader();
			xr.setContentHandler(parser);
			InputSource is = new InputSource(new StringReader(_download));
			long startMS = System.currentTimeMillis();
			xr.parse(is);
			long endMS = System.currentTimeMillis();
			Log.d(TAG,"PARSING XML TOOK "+(endMS-startMS)+" MS.");
			if (parser.getRedirect().equals("")) {
				// No redirect for this page
				if (parser.getUrls().size()==1) {
					// Single-file description, call downloader:
					Log.d(TAG,"DL "+parser.getUrls().get(0));
					Log.d(TAG,"Name "+parser.getSingleName());
					Intent intent = new Intent(callerContext,DownloadService.class);
					intent.putExtra("url", parser.getUrls().get(0));
					intent.putExtra("name",parser.getSingleName());
					callerContext.startService(intent);
				} else {
					// Load converted html:
					final String data = /*encode*/("<!-- "+_url+" -->"+parser.getHTML());
					_download = null;
					synchronized (_view) {
						_view.post(new Runnable() {
							public void run() {
								_view.loadDataWithBaseURL(_url,data,"text/html","UTF-8",_url);
								_view.clearHistory();
							}
						});
					}
				}
				worked = true;
			} else {
				// Redirect:
				_url = parser.getRedirect();
				worked = false;
				//new Thread(new WebLoader(_view,parser.getRedirect())).start();
			}
			return worked;
		}
		
		@Override
		public void run() {
			boolean worked;
			// no shared access to the WebView! 
			try {
				worked = load();
				if (!worked) {
					worked = load();
					if (!worked) {
						load();
					}
				}
			} catch (IOException e) {
				// Show error
				synchronized (_view) {
					e.printStackTrace();
					final String msg = e.getMessage();
					_view.post(new Runnable() {
						public void run() {
							Toast.makeText(callerContext, msg, 4000);
						}
					});
				}
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				Toast.makeText(callerContext,"ParserConfigurationError", 5000);
				e.printStackTrace();
			} catch (SAXException e) {
				//Not XML, show the downloaded html directly in browser:
				synchronized (_view) {
					final String data = /*encode*/("<!-- "+_url+" -->"+_download);
					_download = null;
					_view.post(new Runnable() {
						public void run() {
							_view.loadDataWithBaseURL(_url,data,"text/html","UTF-8",_url);
							//If not cleared, when back is not handled it
							_view.clearHistory();
						}
					});
				}
			} catch (FactoryConfigurationError e) {
				Toast.makeText(callerContext,"FactoryConfigurationError", 5000);
				e.printStackTrace();
			}
			synchronized (activity) {//reset title
				activity.runOnUiThread(new Runnable() {
					public void run() {
						activity.setTitle("TunesViewer");
					}
				});
			}
		}
	}
	
	/*private String makeStringold (InputStream in) throws IOException {
		StringBuilder out = new StringBuilder(1024);
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}*/

	/**
	 * Turns an InputStream into a String.
	 * @param in
	 * @return String value.
	 * @throws IOException
	 */
	private String makeString(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		is.close();
		return sb.toString();
	}
}