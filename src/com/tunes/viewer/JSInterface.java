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

public class JSInterface {

	private Context _context;
	final String[] audioFormats = {".mp3",".m4a",".amr",".m4p",".aiff",".aif",".aifc"};
	
	public JSInterface(Context c) {
		_context = c;
	}
	
	/**
	 * Starts a media file download.
	 * @param title
	 * @param url
	 */
	public void download(String title, String url) {
		Intent intent = new Intent(_context,DownloadService.class);
		intent.putExtra("url", url);
		intent.putExtra("name",title);
		_context.startService(intent);
	}
	
	/**
	 * Shows a view-source dialog with given source string.
	 * @param src
	 */
	public void source(String src) {
		final String source = src;
		new AlertDialog.Builder(_context)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Page Source")
		.setMessage(source)
		.setPositiveButton("Copy Text", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ClipboardManager c = (ClipboardManager)_context.getSystemService(_context.CLIPBOARD_SERVICE);
				c.setText(source);
			}
		})
		.setNegativeButton("Close", null)
		.show();
	}
	
	public void preview(String title, String url) {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW);
			String type = DownloaderTask.fileExt(url);
			if (Arrays.asList(audioFormats).indexOf(type) > -1) {
				i.setDataAndType(Uri.parse(url), "audio/*");
			} else {
				i.setDataAndType(Uri.parse(url), "video/*");
			}
			_context.startActivity(i);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(_context, _context.getText(R.string.NoActivity), Toast.LENGTH_LONG).show();
		}
		
	    /*
		Intent intent = new Intent(_context,Player.class);
		intent.putExtra("url", url);
		intent.putExtra("title", title);
		_context.startActivity(intent);*/
		
	}
}
