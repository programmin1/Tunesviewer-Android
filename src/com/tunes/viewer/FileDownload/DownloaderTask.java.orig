package com.tunes.viewer.FileDownload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.tunes.viewer.ItunesXmlParser;
import com.tunes.viewer.MyReceiver;
import com.tunes.viewer.R;

/**
 * A class to handle a download and its notification.
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
 */
@TargetApi(3)
public class DownloaderTask extends AsyncTask<URL, Integer, Long> {
	private Notifier _notify;
	private int _ID;
	private static final String TAG = "DownloadService";
	private HttpURLConnection _connection;
	DownloadService _context;
	private String _podcast;
	private String _title;
	// url of podcast:
	private String _podcasturl;
	// url of download:
	private URL _url;
	
	/**
	 * Valid characters a file may have. Note that Android chokes on files with a #, and can't find their type.
	 * MUST match in dest function in javascript.js!
	 */
	private final static String VALIDCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 $%`-_@{}~!().";
	
	/**
	 * The file name marking a directory as "ours".
	 */
	public static final String PODCASTDIR_FILE = "podcast_dir.html";
	String _ErrMSG = "";
	private String _sizeStr = "";
	private File _outFile;
	private ArrayList<DownloaderTask> _alltasks;
	private WifiLock _wifiLock;
	private Handler _handler;
	private Timer _timer;

	//boolean success = false;
	
	// The last updated percent downloaded.
	int _lastProgress;
	
	/**
	 * Constructs a downloader for a certain file.
	 * @param c DownloadService reference.
	 * @param t Arraylist of current tasks.
	 * @param title
	 * @param podcast
	 * @param url
	 * @param podcasturl
	 * @param ID unique id of this task (handy for notification etc.)
	 */
	public DownloaderTask(DownloadService c, ArrayList<DownloaderTask> t, String title, String podcast, URL url,
			String podcasturl, int ID) {
		WifiManager wm = (WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "Tunesviewer Download");
		_lastProgress = -1;
		_ID = ID;
		_url = url;
		_context = c;
		_podcast = (podcast != null) ? podcast : "";
		_title = title;
		_alltasks = t;
		_podcasturl = podcasturl;
		_handler = new Handler();
	}
	
	/**
	 * Opens the downloaded file, with default opener.
	 */
	private void openFile() {
		try {
			_context.startActivity(openFile(getFile()));
		} catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(_context, _context.getString(R.string.NoActivity), Toast.LENGTH_LONG).show();
		}
		_notify.finish();
		cancel(false);
	}
	
	/**
	 * Returns an {@link Intent} appropriate for opening a local file.
	 * @param file
	 * @return the Intent.
	 */
	public static Intent openFile(File file) {
		MimeTypeMap myMime = MimeTypeMap.getSingleton();
		Intent newIntent = new Intent(Intent.ACTION_VIEW);
		String mimeType = myMime.getMimeTypeFromExtension(ItunesXmlParser.fileExt(file.toString()).substring(1));
		newIntent.setDataAndType(Uri.fromFile(file),mimeType);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return newIntent;
	}
	
	/**
	 * Cancels download if not finished, otherwise open the file.
	 * @param notifClicked
	 */
	public void doTapAction(boolean notifClicked) {
		boolean finished = getStatus().equals(AsyncTask.Status.FINISHED);
		//isCancelled could be set sometimes (Android 4.4)
		if (finished /*&& !isCancelled() && notifClicked*/) {//success
			if (!notifClicked) {
				//Toast.makeText(_context, R.string.alreadyDownloaded, Toast.LENGTH_SHORT).show();
			}
			openFile();
		} else if (notifClicked) {
			/*new AlertDialog.Builder(_context)_context doesn't work, getcontext and getapplicationcontext don't work either!?
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Cancel")
			.setMessage("Cancel file download?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Stop
				cancel(false);
			}
	
			}).setNegativeButton("No", null).show();*/
			cancel(false);
		}
	}
	
	public void stop() {
		cancel(false);
	}

	public String getTitle() {
		return _title;
	}
	public URL getURL() {
		return _url;
	}
	
	@Override
	protected Long doInBackground(URL... urls) {
		long downloaded = 0;
		File directory;
		File markDownloading = null;
		try {
			_wifiLock.acquire();
			_url = urls[0];
			_connection =  (HttpURLConnection)urls[0].openConnection();
			_connection.setRequestProperty ("User-agent", "iTunes/10.6.1");
			_connection.connect();
			_connection.setConnectTimeout(1000*30);
			_connection.setReadTimeout(1000*30);
			
			// Make sure response code is in the 200 range.
			if (_connection.getResponseCode() / 100 != 2) {
				Log.e(TAG,"Can't connect. code "+_connection.getResponseCode());
				throw new IOException(String.valueOf(_connection.getResponseCode()));
			}
			final long contentLength = _connection.getContentLength();
			_sizeStr = filesize(contentLength);

			BufferedInputStream in = new BufferedInputStream(_connection.getInputStream(),1024*2);
			BufferedOutputStream out;
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
			String downloadDir = prefs.getString("DownloadDirectory",_context.getString(R.string.defaultDL));
			if (clean(_podcast).equals("")) {//NPE sometimes
				directory = new File(downloadDir);
			} else {
				directory = new File(downloadDir,clean(_podcast));
			}
			if (!directory.isDirectory()) {
				// Creating a new directory, so add a link here.
				// Directory with this link file is a "podcast directory", that is,
				// it has been created by this app. 
				// For security, only podcast-directories should
				// be scannable from the webview to see what has/hasn't been downloaded.
				directory.mkdirs();
				File mark = new File(directory,PODCASTDIR_FILE);
				BufferedWriter outfile = new BufferedWriter(new FileWriter(mark));
				outfile.write("<html><head><title>"+_podcast+"</title></head>"+
				"<body><a href=\""+_podcasturl+"\">"+_podcast+"</a></body></html>");
				outfile.close();
			}
			if (/*directory.mkdirs() ||*/ directory.isDirectory()) { //Folder created or already existed.
				if (_url.getHost().equals("sourceforge.net") && _url.getPath().startsWith("/projects/")) {
					// Not an ordinary download, this is from the update page.
					_outFile = new File(directory, "update.apk");
					markDownloading = new File(directory, "updating");
				} else {
					_outFile        = new File(directory, clean(_title)+ItunesXmlParser.fileExt(_url.toString()));
					markDownloading = new File(directory, "."+clean(_title)+ItunesXmlParser.fileExt(_url.toString()));
				}
				if (_outFile.toString().endsWith(PODCASTDIR_FILE)) {
					Log.e(TAG,"Security exception, not writing podcast-directory link.");
					_ErrMSG = "Security exception, not writing podcast-directory link.";
					throw new IOException();
				}
				if (_outFile.exists() && _outFile.length() == contentLength) {
					onPostExecute(contentLength); //Done. Already downloaded.
				} else if (true) {//(connection.getContentLength()==-1 || available(outFile) <= connection.getContentLength()) {
					//TODO: unfortunately checking for room causes crash. Why?
					//Download the file:
					_outFile.createNewFile();
					markDownloading.createNewFile();
					if (contentLength < 1) {
						Log.e(TAG,"No contentlength.");
						//throw new IOException();
					} else {
						_handler.post(new Runnable() {// necessary for gui call in thread.
							@Override
							public void run() {
								Toast.makeText(_context.getApplicationContext(), 
									_context.getText(R.string.startedDownloadingB)+filesize(contentLength), Toast.LENGTH_LONG).show();
							}
						});
					}
					FileOutputStream file = new FileOutputStream(_outFile);
					out = new BufferedOutputStream(file,1024*4); // too big of a buffer and it will crash, out-of-mem exception!
					byte[] data = new byte[1024];
					int count;
					// Set timeout timer, so it doesn't get stuck on non-working download, resulting in incomplete file.
					_timer = new Timer();
					// Read in chunks, much more efficient than byte by byte, lower cpu usage.
					while((count = in.read(data, 0, 1024)) != -1 && !isCancelled()) { //(Byte = in.read()) != -1 && !isCancelled()) {
						out.write(data,0,count);
						downloaded+=count;
						publishProgress((int) ((downloaded/ (float)contentLength)*100));
						_timer.cancel();
						_timer = new Timer();
						_timer.schedule(new Timeout(this), 1000*20);
					}
					_timer.cancel();
					out.flush();
					Log.w(TAG,"downloaded "+downloaded);
					Log.w(TAG,"expected "+contentLength);
					markDownloading.delete();
					if (isCancelled()) {
						_outFile.delete();
					} else {
						//success = true;
						Intent doneintent = new Intent(DownloadService.DOWNLOADBROADCAST);
						doneintent.putExtra(MyReceiver.PAGEURL, _podcasturl);
						doneintent.putExtra(MyReceiver.NAME, _podcast);
						_context.sendBroadcast(doneintent);
						// Get metadata.
						new MediaScannerWrapper(_context, _outFile.toString(), "audio/mp3").scan();
						// Add to Android media player.
						if (prefs.getBoolean("mount", true)) {
							try {
								_context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
									Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
								Log.i(TAG,"Calling ACTION_MEDIA_MOUNTED");
							} catch (SecurityException e) {
								Log.d(TAG,"Exception calling ACTION_MEDIA_MOUNTED");
							}
						}
					}
					in.close();
					out.close();
				} else {
					_ErrMSG = "Not enough room!";
				}
			} else { // Couldn't create, and directory doesn't exist.
				_ErrMSG = "Couldn't create directory, perhaps there is no free space?";
				publishProgress(0);
				cancel(false);
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			_ErrMSG = _context.getText(R.string.DownloadError)+e.getMessage();
			publishProgress(0);
			cancel(false);
		} catch (IOException e) {
			e.printStackTrace();
			_ErrMSG = _context.getText(R.string.DownloadError)+e.getMessage();
			publishProgress(0);
			cancel(false);
		} finally {
			_wifiLock.release();
		}
		return null;
	}
	
	/**
	 * Cleans a string to a valid file name.
	 * MUST match the function dest in javascript.js!
	 * @param name (e.g. "iphone/ipad podcast")
	 * @return name that will work as a file-name. (e.g. "iphoneipad podcast")
	 */
	public static String clean(String name) {
		StringBuilder fixedName = new StringBuilder(name.length());
		for (int c=0; c<name.length(); c++) { // Make a valid name:
			if (VALIDCHARS.indexOf(name.charAt(c))>-1) {
				fixedName.append(name.charAt(c));
			}
		}
		return fixedName.toString().trim();
	}

	/**
	 * Cleans up file and download entry when cancelled.
	 */
	@Override
	protected void onCancelled() {
		_alltasks.remove(this);
		_notify.finish();
		try {
			_outFile.delete();
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	protected File getFile() {
		return _outFile;
	}
	
	@Override
	protected void onPreExecute() {
		_notify = new Notifier(_context, _ID, _podcast, _podcasturl, _url.toString(), _title);
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (_ErrMSG.equals("")) {
			if (values[0]!=_lastProgress) {
				_notify.progressUpdate(values[0],_sizeStr);
				Log.d(TAG,String.valueOf(values[0]));
				_lastProgress = values[0];
			}
		} else {
			Toast.makeText(_context, _ErrMSG, Toast.LENGTH_LONG).show();
			_notify.finish();
			cancel(false);
		}
	}
	@Override
	protected void onPostExecute(Long result) {
		//success = true;
		_notify.showDone();
	}
	
	/**
	 * Returns bytes available.
	 * @param file
	 * @return
	 */
	private static long available(File f) {
		StatFs stat = new StatFs(f.getPath());
		return (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
	}
	
	/**
	 * Returns readable file-size (such as "5 KB", "10 MB").
	 * @param Size in bytes
	 * @return String file-size
	 */
	public static String filesize(long size) {
		final int MB = 1048576;
		final int KB = 1024;
		String output;
		if (size >= MB) {
			output = Math.round(size/(double)MB*10)/10.0+" MB";
		} else if (size >= KB) {
			output = Math.round(size/(double)KB*10)/10.0+" KB";
		} else {
			output = size + " B";
		}
		return output;
	}

	
	public String toString() {
		return _outFile.toString();
	}
	
}

class Timeout extends TimerTask {
	private DownloaderTask _task;
	
	public Timeout(DownloaderTask task) {
		_task = task;
	}
	
	@Override
	public void run() {
		_task._ErrMSG = (String) _task._context.getText(R.string.DownloadErrorTimeout);
		Log.w("DL","Timed out while downloading.");
		_task.cancel(false);
	}
};