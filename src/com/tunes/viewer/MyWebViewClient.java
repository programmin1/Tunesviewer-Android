package com.tunes.viewer;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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

import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MyWebViewClient extends WebViewClient {
		
	final String TAG = "WVC";
	private String _currentUrl = "";
	private Context callerContext;
	private Activity activity;
	CookieManager cookieManager = CookieManager.getInstance();
	IansCookieManager _CM = new IansCookieManager();
	
	public MyWebViewClient (Context c, Activity a) {
		callerContext = c;
		activity = a;
	}
	
	/**
	 * Returns current url of the webview. (even if loadData was used when converting xml).
	 * @return String url caught with shouldOverrideUrlLoading.
	 */
	public String getCurrentUrl() {
		return _currentUrl;
	}
	
	/**
	 * Called on page load, inserts the javascript required to catch download clicks, etc.
	 */
	@Override
	public void onPageFinished(WebView view, String url) {
		Log.d(TAG,"Inserting script into "+url);
		view.loadUrl("javascript:"+TunesViewerActivity.getContext().getString(R.string.Javascript));
		//view.loadUrl("javascript:window.DOWNLOADINTERFACE.source(document.documentElement.innerHTML);");
	}
	
	/**
	 * Determines load behavior.
	 * If it's html, this lets webview show it, if it's special xml file, it converts it and loads it.
	 */
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		System.setProperty("http.agent", "iTunes/10.3");
		view.requestFocus(View.FOCUS_DOWN);
		view.stopLoading();
		activity.setTitle("XML Loading...");
		new Thread(new WebLoader(view,url)).start();
		return true;
	}
	
	// always verify the host - dont check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
				return true;
		}
	};

	/**
	 * Trust every server - dont check for any certificate
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
	
	private class WebLoader implements Runnable {
		private WebView _view;
		private String _url;
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
			
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			ItunesXmlParser parser = new ItunesXmlParser(u.getRef());
			//Pass http stream to the iTunes parser:
			saxParser.parse(conn.getInputStream(), parser);
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
					final String htm = parser.getHTML();
					synchronized (_view) {
						_view.post(new Runnable() {
							public void run() {
								_view.loadData(htm,"text/html","UTF-8");
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
				_currentUrl = _url;
			} catch (IOException e) {
				// Show error
				synchronized (_view) {
					final String msg = e.getMessage();
					_view.post(new Runnable() {
						public void run() {
							Toast.makeText(callerContext, msg, 4000);
						}
					});
				}
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				//Not xml, show in browser.
				_currentUrl = _url;
				synchronized (_view) {
					_view.post(new Runnable() {
						public void run() {
							_view.loadUrl(_url);
						}
					});
				}
			} catch (FactoryConfigurationError e) {
				// TODO Auto-generated catch block
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
}