package com.tunes.viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
		if (pageurl==null) {
			System.err.println("ERRROR SENT RECEIVER A NULL PAGEURL");
		}
		
		final StringBuilder js = new StringBuilder("javascript:updateDownloadOpen([");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_caller);
		String downloadDir = prefs.getString("DownloadDirectory",_caller.getString(R.string.defaultDL));
		File[] names = null;
		boolean hasdata = false;
		if (!DownloaderTask.clean(podcastname).equals("")) {//NPE sometimes
			File directory = new File(downloadDir,DownloaderTask.clean(podcastname));
			File linkfile = new File(directory,DownloaderTask.PODCASTDIR_FILE);
			if (linkfile.exists()) {
				// This is our app's directory, safe for webview to see.
				try {
				    BufferedReader in = new BufferedReader(new FileReader(linkfile));
				    FileOutputStream test = null;
				    System.out.println("For "+linkfile.getCanonicalPath());
				    String line = in.readLine();
				    if (line != null) {
				    System.out.println(line);
				    if (line.indexOf("\""+pageurl+"\"") != -1) {
				    	// This is the page described in the file, safe.
				    	names = directory.listFiles();
						for (int i=0; i<names.length; i++) {
							try {
								System.out.print((names[i]).getName());
								test = new FileOutputStream(names[i]);
								if (test.getChannel().tryLock() != null) {
									//Not a partial download
									js.append("\"");
									js.append(names[i].getName().replace("\"", "\\\""));
									js.append("\"");
									hasdata = true;
									if (i != names.length-1) {
										js.append(", ");
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								test.close();
							}
						}
				    } else {
				    	System.err.println("Not sending directory info to page, since it is wrong URL!");
				    }
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
