package com.tunes.viewer;

import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;

/**
 * A subclass of BroadcastReceiver that binds to a WebView,
 * updates the download/open buttons with a bit of javascript when called.
 * @author luke
 *
 */
public class MyReceiver extends android.content.BroadcastReceiver {
    private WebView _webview;

	public MyReceiver(WebView w) {
    	_webview = w;
    }

	@Override
    public void onReceive(Context context, Intent intent) {
		System.out.println("Received download-notification");
    	_webview.loadUrl("javascript:updateDownloadOpen();");
    }
}
