package com.tunes.viewer;

import java.util.Arrays;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.ClipboardManager;
import android.widget.Toast;

/**
 * Javascript interface for the WebView
 * Note that all of these functions must be safe for untrusted input!
 */
public class JSInterface {

	private TunesViewerActivity _context;
	final String[] audioFormats = {".mp3",".m4a",".amr",".m4p",".aiff",".aif",".aifc"};
	final String[] videoFormats = {".mp4",".m4v",".mov",".m4b"};
	public JSInterface(TunesViewerActivity c) {
		_context = c;
	}
	
	/**
	 * Starts a media file download.
	 * @param title
	 * @param url
	 */
	public void download(String title, String podcast, String url) {
		Intent intent = new Intent(_context,DownloadService.class);
		intent.putExtra("url", url);
		intent.putExtra("podcast", podcast);
		intent.putExtra("name",title);
		_context.startService(intent);
	}
	
	/**
	 * Previews an audio/video stream using the system's default player.
	 * @param title
	 * @param url
	 */
	public void preview(String title, String url) {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW);
			String type = ItunesXmlParser.fileExt(url);
			if (Arrays.asList(audioFormats).indexOf(type) > -1) {
				i.setDataAndType(Uri.parse(url), "audio/*");
			} else if (Arrays.asList(videoFormats).indexOf(type) > -1) {
				i.setDataAndType(Uri.parse(url), "video/*");
			} else {
				i.setDataAndType(Uri.parse(url), "*/*");
			}
			_context.startActivity(i);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(_context, _context.getText(R.string.NoActivity), Toast.LENGTH_LONG).show();
		}
	}
	

	/**
	 * Shows a view-source dialog with given source string.
	 * @param src
	 */
	public void source(String src) {
		final String source = src;
		new AlertDialog.Builder(_context)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setTitle("Page Source ("+source.length()+" chars.)")
		.setMessage(source)
		.setPositiveButton("Copy Text", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ClipboardManager cb = (ClipboardManager)_context.getSystemService(Context.CLIPBOARD_SERVICE);
				cb.setText(source);
			}
		})
		.setNegativeButton("Close", null)
		.show();
	}
	
	/**
	 * Go to a url. Workaround for http://stackoverflow.com/questions/5129112/shouldoverrideurlloading-does-not-work-catch-link-clicks-while-page-is-loading
	 * @param url - the url to go to.
	 */
	public void go(String url) {
		_context.loadUrl(url);
	}
	
	public void subscribe(String url) {
		try {
			//Change it to itpc://url.
			if (!url.startsWith("itpc") && url.indexOf("://")>-1) {
				url = "itpc"+url.substring(url.indexOf("://"));
			}
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.setData(Uri.parse(url));
			_context.startActivity(i);
		} catch (ActivityNotFoundException e) {
			new AlertDialog.Builder(_context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("No Podcatcher")
			.setMessage("No podcast app found to handle this link! You must install a podcast manager app that handles itpc:// links, to subscribe.")
			.setNegativeButton("Done", null)
			.show();
		}
	}
	
	/**
	 * Sets the activity's title.
	 * @param title
	 */
	public void setTitle(final String title) {
		_context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_context.setTitle(title.replace("&amp;","&"));
			}
		});
	}
}
