package com.tunes.viewer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class DownloadService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private static final String TAG = "DownloaderService";
	private ArrayList<DownloaderTask> myDownloaders = new ArrayList<DownloaderTask>();
 
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		String url = intent.getStringExtra("url");
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
				DownloaderTask T=(DownloaderTask) new DownloaderTask(getApplicationContext(),myDownloaders,
						name,new URL(url),myDownloaders.size());
				T.execute(new URL(intent.getStringExtra("url")));
				myDownloaders.add(T);
			} catch (MalformedURLException e) {
				Toast.makeText(getApplicationContext(),"Invalid url",1000).show();
				e.printStackTrace();
			}
		}
		//Toast.makeText(getApplicationContext(), "Started Download", 1000).show();
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
