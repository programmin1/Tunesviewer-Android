package com.tunes.viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tunes.viewer.FileDownload.DownloaderTask;

/**
 * A subclass of BroadcastReceiver that holds reference to main activity,
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
		String pageurl = intent.getStringExtra(PAGEURL);
		
		StringBuilder js = new StringBuilder("javascript:updateDownloadOpen([");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_caller);
		String downloadDir = prefs.getString("DownloadDirectory",_caller.getString(R.string.defaultDL));
		if (!DownloaderTask.clean(podcastname).equals("")) {//NPE sometimes
			File directory = new File(downloadDir,DownloaderTask.clean(podcastname));
			File linkfile = new File(directory,DownloaderTask.PODCASTDIR_FILE);
			if (linkfile.exists()) {
				// This is our app's directory, safe for webview to see.
				try {
				    BufferedReader in = new BufferedReader(new FileReader(linkfile));
				    if (in.readLine().indexOf("\""+pageurl+"\"") != -1) {
				    	// This is the page described in the file, safe.
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
				    in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			js.append("]);");
		}
		//System.out.println(js.toString());
		
    	_caller.getWeb().loadUrl(js.toString());
    }
}
