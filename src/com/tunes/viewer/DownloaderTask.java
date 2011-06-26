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
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

/**
 * A class to handle a download and its notification.
 * @author luke
 *
 */
public class DownloaderTask extends AsyncTask<URL, Integer, Long> {
	private Notifier notify;
	private int _ID;
	private static final String TAG = "DownloadService";
	private HttpURLConnection connection;
	private Context _context;
	private String _title;
	private URL _url;
	private final String VALIDCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 $%`-_@{}~!#().";
	private String ErrMSG = "";
	private File outFile;
	private ArrayList<DownloaderTask>_alltasks;
	boolean success = false;
	
	// The last updated percent downloaded.
	int lastProgress;
	
	public DownloaderTask(Context c, ArrayList<DownloaderTask> t, String title, URL url, int ID) {
		lastProgress = -1;
		_ID = ID;
		_url = url;
		_context = c;
		_title = title;
		_alltasks = t;
	}
	
	/**
	 * Opens the file downloaded, with default opener.
	 */
	private void openFile() {
		MimeTypeMap myMime = MimeTypeMap.getSingleton();

		Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
		String mimeType = myMime.getMimeTypeFromExtension(fileExt(getFile().toString()).substring(1));
		newIntent.setDataAndType(Uri.fromFile(getFile()),mimeType);
		newIntent.setFlags(newIntent.FLAG_ACTIVITY_NEW_TASK);
		try {
			_context.startActivity(newIntent);
		} catch (android.content.ActivityNotFoundException e) {
			Toast.makeText(_context, "No app for this type of file.", 4000).show();
		}
		notify.finish();
		cancel(false);
	}
	
	public void doTapAction(boolean notifClicked) {
		boolean s = getStatus().equals(AsyncTask.Status.FINISHED);
		if (s && !isCancelled()/* && notifClicked*/) {//success
			// View file.
			//String type = fileExt(getFile().toString()).toLowerCase();
			openFile();
			return;
		} else if (notifClicked) {
			/*new AlertDialog.Builder(_context)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Cancel")
		.setMessage("Cancel file download?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			//Stop
			
		}

		});*/

			cancel(false);
			//myDownloaders.remove(T);
			return;
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
			_url = urls[0];
			connection =  (HttpURLConnection)urls[0].openConnection();
			connection.setRequestProperty ("User-agent", "iTunes/10.2");
			connection.connect();
			// Make sure response code is in the 200 range.
			if (connection.getResponseCode() / 100 != 2) {
				Log.e(TAG,"Can't connect. code "+connection.getResponseCode());
			throw new IOException();
			}
			long contentLength = connection.getContentLength();
			if (contentLength < 1) {
				Log.e(TAG,"Invalid contentlength.");
			throw new IOException();
			}
			BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
			StringBuffer fixedName = new StringBuffer();
			for (int c=0; c<_title.length(); c++) { // Make a valid name:
				if (VALIDCHARS.indexOf(_title.charAt(c))>-1) {
					fixedName.append(_title.charAt(c));
				}
			}
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
			outFile = new File(prefs.getString("DownloadDirectory","/sdcard/"),fixedName.toString()+fileExt(_url.toString()));
			if (outFile.exists() && outFile.length() == contentLength) {
				onPostExecute(contentLength); //Done. Already downloaded.
			} else {
			outFile.createNewFile();
			FileOutputStream file = new FileOutputStream(outFile);
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
					outFile.delete();
			} else {
				success = true;
			}
			in.close();
			out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			ErrMSG = "Download error: "+e.getMessage();
			//TODO: Fix it so this cancels, not finishes.
			cancel(false);
		}
		return null;
	}
	
	public static String fileExt(String url) {
		String ext = url.substring(url.lastIndexOf(".") );
	if (ext.indexOf("?")>-1) {
		ext = ext.substring(0,ext.indexOf("?"));
	}
	if (ext.indexOf("%")>-1) {
		ext = ext.substring(0,ext.indexOf("%"));
	}
	return ext;
	}
	
	@Override
	protected void onCancelled() {
		_alltasks.remove(this);
		notify.finish();
	}
	
	protected File getFile() {
		return outFile;
	}
	
	@Override
	protected void onPreExecute() {
		notify = new Notifier(_context, _ID, _url.toString(), _title);
	}
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (ErrMSG.equals("")) {
			if (values[0]!=lastProgress) {
				notify.progressUpdate(values[0]);
				Log.d(TAG,String.valueOf(values[0]));
				lastProgress = values[0];
			}
		} else {
			Toast.makeText(_context, ErrMSG, 4000).show();
			notify.finish();
			cancel(false);
		}
	}
	@Override
	protected void onPostExecute(Long result) {
		success = true;
		notify.showDone();
	}
	
	public String toString() {
		return outFile.toString();
	}
}