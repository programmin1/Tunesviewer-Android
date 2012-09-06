package com.tunes.viewer.FileDownload;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.tunes.viewer.ItunesXmlParser;
import com.tunes.viewer.R;
import com.tunes.viewer.Bookmarks.DbAdapter;


import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * The downloader service, manages DownloaderTasks.
 * When called, it opens selected file, or starts new download.
 * 
 * @author Luke Bryan
 *
 */
@TargetApi(3)
public class DownloadService extends Service {

	public static final String DOWNLOADBROADCAST = "com.tunes.viewer.DownloadService.action.DOWNLOADED";
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private static final String TAG = "DownloaderService";
	private ArrayList<DownloaderTask> myDownloaders = new ArrayList<DownloaderTask>();

	@Override
	public void onStart(Intent intent, int startId) {
		if (intent==null) {
			Log.e(TAG,"Null intent.");
			return;
		}
		super.onStart(intent, startId);
		
		String url = intent.getStringExtra("url");
		String podcast = intent.getStringExtra("podcast");
		String podcasturl = intent.getStringExtra("podcasturl");
		String name = intent.getStringExtra("name");
		boolean notifClick = intent.getBooleanExtra("notifClick", false);
		// Check if exists, if so, open or cancel:
		synchronized(myDownloaders) {
			/* This Could keep file from downloading, if file has been downloaded, and deleted:
			 * 
			 * for (DownloaderTask T : myDownloaders) {
				try {
					if (T.getTitle().equals(name) && T.getURL().getPath().equals(new URL(url).getPath())) {
							T.doTapAction(notifClick);
							return;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}*/
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			File possibleFile = StoredFile(this, podcast, name, url);
			if (possibleFile.exists()) {
				try {
					startActivity(DownloaderTask.openFile(possibleFile));
				} catch (android.content.ActivityNotFoundException e) {
					Toast.makeText(this, getString(R.string.NoActivity), Toast.LENGTH_LONG).show();
				}
			} else {
				//Start new downloader:
				try {
					DownloaderTask T=(DownloaderTask) new DownloaderTask(this,myDownloaders,
							name,podcast,new URL(url), podcasturl, myDownloaders.size());
					T.execute(new URL(intent.getStringExtra("url")));
					myDownloaders.add(T);
					// And add to bookmarks?
					if (prefs.getBoolean("bookmarkDownloads", true)) {
						DbAdapter dbHelper = new DbAdapter(this);
				        dbHelper.open();
				        dbHelper.insertItem(podcast, podcasturl);
				        dbHelper.close();
					}
					
				} catch (MalformedURLException e) {
					Toast.makeText(getApplicationContext(),"Download url is invalid",1000).show();
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Returns the File object of where an item would be stored, given podcast-name,
	 * title, and url for file-extension.
	 * @param context
	 * @param podcastname
	 * @param title
	 * @param urlFileExt
	 * @return File object.
	 */
	public File StoredFile(Context context, String podcastname, String title, String urlFileExt) {
		File directory;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String downloadDir = prefs.getString("DownloadDirectory",context.getString(R.string.defaultDL));
		if (DownloaderTask.clean(podcastname).equals("")) {//NPE sometimes
			directory = new File(downloadDir);
		} else {
			directory = new File(downloadDir,DownloaderTask.clean(podcastname));
		}
		return new File(directory, DownloaderTask.clean(title)+ItunesXmlParser.fileExt(urlFileExt.toString()));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(getApplicationContext(), "DownloadService Destroy", 4000).show();
		for (DownloaderTask T : myDownloaders) {
			T.cancel(false);
		}
	}
	
}
