package com.tunes.viewer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Notification manager class, handles a notification with % complete and estimated time remaining.
 */
public class Notifier {
	
	private Context _context;
	private NotificationManager notificationManager;
	private Notification notification;
	private PendingIntent notificationIntent;
	private int _NOTIFICATION_ID;
	private String _url;
	private String _title;
	private long _started;
	
	public Notifier(Context c, int NOTIF_ID, String url, String title) {
		_NOTIFICATION_ID = NOTIF_ID;
		_context = c;
		_url= url;
		_title=title;
		notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		makeNotification(true,title);
		_started = System.currentTimeMillis();
	}
	
	/**
	 * Creates this class' notification.
	 * @param ongoing - True when download still in progress
	 * @param title - initial title
	 */
	private void makeNotification(boolean ongoing,String title) {
		int icon;
		String lowerTitle;
		if (ongoing) {
			icon = android.R.drawable.stat_sys_download;
		} else {
			icon = android.R.drawable.stat_sys_download_done;
		}
		CharSequence tickerText = title;
		notification = new Notification(icon,tickerText,System.currentTimeMillis());
		
		Intent returnIntent = new Intent(_context,DownloadService.class);
		returnIntent.putExtra("url", _url);
		returnIntent.putExtra("name",_title);
		returnIntent.putExtra("notifClick",true);
		returnIntent.setAction("test.test.myAction"+_NOTIFICATION_ID);
		// Important to make a unique action, and FLAG_CANCEL_CURRENT, to make distinct notifications.

		notificationIntent = PendingIntent.getService(_context, 0, returnIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		if (ongoing) {
			lowerTitle = "Tap to cancel download.";
		} else {
			lowerTitle = "Done.";
		}
		notification.setLatestEventInfo(_context, _title, lowerTitle, notificationIntent);
		if (ongoing) {
			notification.flags = Notification.FLAG_ONGOING_EVENT;
		}
		notificationManager.notify(_NOTIFICATION_ID, notification);
	}
	
	/**
	 * Updates the notification's display with percent progress.
	 * @param progress
	 */
	public void progressUpdate(int progress) {
		/*******  To calculate time remaining ********
		 * Assuming elapsedTime / fullTime = percentdownloaded / 100,
		 * fullTime = 100*elapsedTime / percentdownloaded.
		 */
		if (progress > 0) {
			long fullTime = 100*elapsedTime()/progress;
			String remaining = ItunesXmlParser.timeval(String.valueOf(fullTime - elapsedTime()));
			CharSequence contentText = progress + "% ("+remaining+") Tap to cancel download.";
			notification.setLatestEventInfo(_context, _title, contentText, notificationIntent);
			notificationManager.notify(_NOTIFICATION_ID, notification);
		}
	}
	
	private long elapsedTime() {
		return System.currentTimeMillis()-_started;
	}
	
	/**
	 * Called when this download has finished, convert to a new non-ongoing notification.
	 */
	public void showDone() {
		notificationManager.cancel(_NOTIFICATION_ID);
		makeNotification(false,_title);
	}
	
	/**
	 * Removes the notification
	 */
	public void finish() {
		notificationManager.cancel(_NOTIFICATION_ID);
	}
}
