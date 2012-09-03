package com.tunes.viewer.FileDownload;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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
			for (DownloaderTask T : myDownloaders) {
				try {
					if (T.getTitle().equals(name) && T.getURL().getPath().equals(new URL(url).getPath())) {
							T.doTapAction(notifClick);
							return;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			//Start new downloader:
			try {
				DownloaderTask T=(DownloaderTask) new DownloaderTask(this,myDownloaders,
						name,podcast,new URL(url), podcasturl, myDownloaders.size());
				T.execute(new URL(intent.getStringExtra("url")));
				myDownloaders.add(T);
			} catch (MalformedURLException e) {
				Toast.makeText(getApplicationContext(),"Download url is invalid",1000).show();
				e.printStackTrace();
			}
		}
	}
	
	private void announceDownload() {
		sendBroadcast(new Intent(DOWNLOADBROADCAST));
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
