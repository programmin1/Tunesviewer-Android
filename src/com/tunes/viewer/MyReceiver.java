package com.tunes.viewer;

import java.io.File;

import com.tunes.viewer.FileDownload.DownloadService;
import com.tunes.viewer.FileDownload.DownloaderTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.WebView;

/**
 * A subclass of BroadcastReceiver that binds to a WebView,
 * updates the download/open buttons with a bit of javascript when called.
 * @author luke
 *
 */
public class MyReceiver extends android.content.BroadcastReceiver {
    private TunesViewerActivity _caller;
    public static final String NAME = "Name";
	public static final String PAGEURL = "PageUrl";

	public MyReceiver(TunesViewerActivity a) {
    	_caller = a;
    }

	@Override
    public void onReceive(Context context, Intent intent) {
		System.out.println("Received download-notification");
		
		System.out.println(intent.getStringExtra(PAGEURL));
		System.out.println(intent.getStringExtra(NAME));
		String podcastname = intent.getStringExtra(NAME);
		
		StringBuilder js = new StringBuilder("javascript:updateDownloadOpen([");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_caller);
		String downloadDir = prefs.getString("DownloadDirectory",_caller.getString(R.string.defaultDL));
		if (!DownloaderTask.clean(podcastname).equals("")) {//NPE sometimes
			File directory = new File(downloadDir,DownloaderTask.clean(podcastname));
			if (new File(directory,"podcast_dir.html").exists()) {
				// This is our app's directory, safe for webview to scan.
				// TODO: check the url also, from this file.
				String[] names = directory.list();
				for (int i=0; i<names.length; i++) {
					js.append("\"");
					js.append(names[i].replace("\"", "\\\""));
					js.append("\"");
					if (i != names.length-1) {
						js.append(", ");
					}
				}
			}
			js.append("]);");
		}
		System.out.println(js.toString());
		
    	_caller.getWeb().loadUrl(js.toString());
    }
}
