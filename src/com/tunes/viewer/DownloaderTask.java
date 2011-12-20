package com.tunes.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import android.net.wifi.WifiManager.WifiLock;

/**
 * A class to handle a download and its notification.
 * @author Luke
 */
public class DownloaderTask extends AsyncTask<URL, Integer, Long> {
	private Notifier _notify;
	private int _ID;
	private static final String TAG = "DownloadService";
	private HttpURLConnection _connection;
	private DownloadService _context;
	private String _podcast;
	private String _title;
	private URL _url;
	// Valid characters a file may have. Note that Android chokes on files with a #, and can't find their type.
	private final String VALIDCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 $%`-_@{}~!().";
	private String _ErrMSG = "";
	private String _sizeStr = "";
	private File _outFile;
	private ArrayList<DownloaderTask> _alltasks;
	private WifiLock _wifiLock;
	private Handler _handler;
	//boolean success = false;
	
	// The last updated percent downloaded.
	int _lastProgress;
	
	public DownloaderTask(DownloadService c, ArrayList<DownloaderTask> t, String title, String podcast, URL url, int ID) {
		WifiManager wm = (WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "Tunesviewer Download");
		_lastProgress = -1;
		_ID = ID;
		_url = url;
		_context = c;
		_podcast = podcast;
		_title = title;
		_alltasks = t;
		_handler = new Handler();
	}
	
	/**
	 * Opens the downloaded file, with default opener.
	 */
	private void openFile() {
		MimeTypeMap myMime = MimeTypeMap.getSingleton();

		Intent newIntent = new Intent(Intent.ACTION_VIEW);
		String mimeType = myMime.getMimeTypeFromExtension(ItunesXmlParser.fileExt(getFile().toString()).substring(1));
		newIntent.setDataAndType(Uri.fromFile(getFile()),mimeType);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			_context.startActivity(newIntent);
		} catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(_context, _context.getString(R.string.NoActivity), Toast.LENGTH_LONG).show();
		}
		_notify.finish();
		cancel(false);
	}
	
	/**
	 * Cancels download if not finished, otherwise open the file.
	 * @param notifClicked
	 */
	public void doTapAction(boolean notifClicked) {
		boolean s = getStatus().equals(AsyncTask.Status.FINISHED);
		if (s && !isCancelled()/* && notifClicked*/) {//success
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
		try {
			_wifiLock.acquire();
			_url = urls[0];
			_connection =  (HttpURLConnection)urls[0].openConnection();
			_connection.setRequestProperty ("User-agent", "iTunes/10.4");
			_connection.connect();
			// Make sure response code is in the 200 range.
			if (_connection.getResponseCode() / 100 != 2) {
				Log.e(TAG,"Can't connect. code "+_connection.getResponseCode());
				throw new IOException();
			}
			final long contentLength = _connection.getContentLength();
			_sizeStr = filesize(contentLength);
			if (contentLength < 1) {
				Log.e(TAG,"Invalid contentlength.");
				throw new IOException();
			} else {
				_handler.post(new Runnable() {// necessary for gui call in thread.
					@Override
					public void run() {
						Toast.makeText(_context.getApplicationContext(), 
							"Started downloading "+filesize(contentLength), Toast.LENGTH_LONG).show();
					}
				});
			}
			BufferedInputStream in = new BufferedInputStream(_connection.getInputStream());
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
			String downloadDir = prefs.getString("DownloadDirectory",_context.getString(R.string.defaultDL));
			File directory = new File(downloadDir,clean(_podcast)); 
			if (directory.mkdirs() || directory.isDirectory()) { //Folder created or already existed.
				_outFile = new File(downloadDir
						, clean(_podcast) +"/"+ clean(_title)+ItunesXmlParser.fileExt(_url.toString()));
				if (_outFile.exists() && _outFile.length() == contentLength) {
					onPostExecute(contentLength); //Done. Already downloaded.
				} else if (true) {//(connection.getContentLength()==-1 || available(outFile) <= connection.getContentLength()) {
					//TODO: unfortunately checking for room causes crash. Why?
					//Download the file:
					_outFile.createNewFile();
					FileOutputStream file = new FileOutputStream(_outFile);
					BufferedOutputStream out = new BufferedOutputStream(file);
					int Byte;
					while ((Byte = in.read()) != -1 && !isCancelled()) {
						out.write(Byte);
						downloaded++;
						if (downloaded % 1024 == 0) {
							publishProgress((int) ((downloaded/ (float)contentLength)*100));
						}
					}
					out.flush();
					if (isCancelled()) {
							_outFile.delete();
					} else {
						//success = true;
					}
					in.close();
					out.close();
					new MediaScannerWrapper(_context, _outFile.toString(), "audio/mp3").scan();
					_context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
							Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
				} else {
					_ErrMSG = "Not enough room!";
				}
			} else { // Couldn't create, and directory doesn't exist.
				_ErrMSG = "Couldn't create directory, perhaps there is no free space?";
				publishProgress(0);
				cancel(false);
			}
		} catch (IOException e) {
			e.printStackTrace();
			_ErrMSG = "Download error: "+e.getMessage();
			publishProgress(0);
			cancel(false);
		} finally {
			_wifiLock.release();
		}
		return null;
	}
	
	private String clean(String name) {
		StringBuilder fixedName = new StringBuilder(name.length());
		for (int c=0; c<name.length(); c++) { // Make a valid name:
			if (VALIDCHARS.indexOf(name.charAt(c))>-1) {
				fixedName.append(name.charAt(c));
			}
		}
		return fixedName.toString().trim();
	}

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
		_notify = new Notifier(_context, _ID, _url.toString(), _title);
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
	public static long available(File f) {
		StatFs stat = new StatFs(f.getPath());
		return (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
	}
	
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