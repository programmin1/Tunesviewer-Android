package com.tunes.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MyWebViewClient extends WebViewClient {
		
	final String TAG = "WVC";
	private Context callerContext;
	private Activity activity;
	CookieManager cookieManager = CookieManager.getInstance();
	IansCookieManager _CM = new IansCookieManager();
	SharedPreferences _prefs;
	
	public MyWebViewClient (Context c, Activity a) {
		callerContext = c;
		activity = a;
		_prefs = PreferenceManager.getDefaultSharedPreferences(activity);
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
		String ua = _prefs.getString("UserAgent", "");
		System.setProperty("http.agent", ua);
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
	
	private String encode(String text) {
		//workaround for: https://code.google.com/p/android/issues/detail?id=4401
		return URLEncoder.encode(text).replaceAll("\\+"," ");
	}
	
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
			// Remove unneeded xml declaration that may cause errors on some pages:
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
			//xr.parse(new InputSource())
			//Pass http stream to the iTunes parser:
			//saxParser.parse(conn.getInputStream(), parser);
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
								_view.loadData(encode("<!-- "+_url+" -->"+htm),"text/html","UTF-8");
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				//Not xml, show the downloaded html directly in browser:
				synchronized (_view) {
					final String data = encode("<!-- "+_url+" -->"+_download); 
					_view.post(new Runnable() {
						public void run() {
							_view.loadData(data,"text/html","UTF-8");
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
	
	/**
	 * Turns an InputStream into a String.
	 * @param in
	 * @return String value.
	 * @throws IOException
	 */
	public static String makeString (InputStream in) throws IOException {
	    StringBuffer out = new StringBuffer();
	    byte[] b = new byte[4096];
	    for (int n; (n = in.read(b)) != -1;) {
	        out.append(new String(b, 0, n));
	    }
	    return out.toString();
	}
}