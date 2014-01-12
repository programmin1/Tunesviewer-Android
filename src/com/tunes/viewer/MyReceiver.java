package com.tunes.viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tunes.viewer.FileDownload.DownloaderTask;

/**
 * A subclass of BroadcastReceiver that holds reference to main activity,
 * updates the download/open buttons with a bit of javascript when called.
 * 
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
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
		if (intent.getStringExtra(PAGEURL) == null || intent.getStringExtra(NAME)==null) {
			return;
		}
		System.out.println("Received download-notification");
		
		System.out.println(intent.getStringExtra(PAGEURL));
		System.out.println(intent.getStringExtra(NAME));
		String podcastname = intent.getStringExtra(NAME);
		String pageurl = intent.getStringExtra(PAGEURL);
		/*try {
			URI uri = URI.create(pageurl);
			pageurl = uri.getScheme()+"://"+uri.getHost()+uri.getPath();
		} catch (Exception e) {
			System.err.println("MyReceiver current page not valid: "+pageurl);
		}*/
		final StringBuilder js = new StringBuilder("javascript:updateDownloadOpen([");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_caller);
		String downloadDir = prefs.getString("DownloadDirectory",_caller.getString(R.string.defaultDL));
		String[] names = null;
		boolean hasdata = false;
		if (!DownloaderTask.clean(podcastname).equals("")) {//NPE sometimes
			File directory = new File(downloadDir,DownloaderTask.clean(podcastname));
			File linkfile = new File(directory,DownloaderTask.PODCASTDIR_FILE);
			if (linkfile.exists()) {
				// This is our app's directory, safe for webview to see.
				try {
				    BufferedReader in = new BufferedReader(new FileReader(linkfile));
				    String line = in.readLine();
				    if (line != null) {
					    if (line.indexOf("\""+pageurl) != -1 ||
					        line.indexOf("\""+pageurl.replaceFirst("https://", "http://")) != -1) {
					    	// This is the page described in the file, safe.
					    	names = directory.list();
							for (int i=0; i<names.length; i++) {
								if (!(new File(directory, "."+names[i]).exists())) {//Not current download.
									js.append("\"");
									js.append(names[i].replace("\"", "\\\""));
									js.append("\",");
									hasdata = true;
								}
							}
					    } else {
					    	System.err.println("Not sending directory info to page, since it is wrong URL!");
					    }
				    } else {
				    	System.err.println("No line, null line, blank postast marker file?");
				    }
				    in.close();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			} else {
				System.err.println("Not sending directory info to page, since directory doesn't have marker file.");
			}
			js.append("]);");
			if (hasdata) {
				// Injecting JS too many times may cause webview to crash with no error message. (maybe a thread issue?)
		    	_caller.getWeb().post(new Runnable() {
					@Override
					public void run() {
						_caller.getWeb().loadUrl(js.toString());
					}
				});
			}
		}
		//System.out.println(js.toString());
		
    }
}
