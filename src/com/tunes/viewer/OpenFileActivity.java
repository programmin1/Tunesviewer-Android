package com.tunes.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.tunes.viewer.FileDownload.DownloaderTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity for viewing .pages
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
 */
public class OpenFileActivity extends Activity {

	private File _infile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setContentView(R.layout.openfile);
		String uri = this.getIntent().getData().getPath();
		boolean hasPdf = false;
		boolean hasThumbnail = false;
		Button pdf = (Button) findViewById(R.id.buttonPdf);
		Button thumbnail = (Button) findViewById(R.id.buttonThumbnail);
		if (ItunesXmlParser.fileExt(uri).equals(".pages")) {
			try {
				_infile = new File(uri);
				ZipInputStream zis = new ZipInputStream(new FileInputStream(_infile));
				ZipEntry ze;
				while ((ze = zis.getNextEntry()) != null) {
					if (ze.getName().equals("QuickLook/Thumbnail.jpg")) {
						hasThumbnail = true;
						//File outfile = new File(uri.substring(0,uri.lastIndexOf("."))+".jpg" );
						//write(zis, new BufferedOutputStream( new FileOutputStream(outfile) ));
					} else if (ze.getName().equals("QuickLook/Preview.pdf")) {
						hasPdf = true;
						//File outfile = new File(uri.substring(0,uri.lastIndexOf("."))+".pdf" );
						//write(zis, new BufferedOutputStream( new FileOutputStream(outfile) ));
					}
				}
				zis.close();
			} catch (FileNotFoundException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
			((TextView)findViewById(R.id.textFileDesc)).setText(uri.toString());
		} else {
			((TextView)findViewById(R.id.textFileDesc)).setText(uri.toString()+"\n\n"+ getString(R.string.unsupported));
			Toast.makeText(this, getString(R.string.unsupported), Toast.LENGTH_LONG).show();
		}
		if (hasPdf) {
			pdf.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					OpenFileActivity.this.open("QuickLook/Preview.pdf");
				}
			});
		} else {
			findViewById(R.id.buttonPdf).setVisibility(View.GONE);
		}
		if (hasThumbnail) {
			thumbnail.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					OpenFileActivity.this.open("QuickLook/Thumbnail.jpg");
				}
			});
		} else {
			findViewById(R.id.buttonThumbnail).setVisibility(View.GONE);
		}
		
	}
	
	/**
	 * Opens a certain file in the zip.
	 * @param name - the name of the file.
	 */
	protected void open(String name) {
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(_infile));
			ZipEntry ze;
			String uri = _infile.toString();
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.getName().equals(name)) {
					File outfile = new File(uri.substring(0,uri.lastIndexOf("."))+ItunesXmlParser.fileExt(name) );
					write(zis, new BufferedOutputStream( new FileOutputStream(outfile) ));
					try {
						startActivity(DownloaderTask.openFile(outfile));
					} catch (android.content.ActivityNotFoundException e) {
						Toast.makeText(this, getString(R.string.NoActivity), Toast.LENGTH_LONG).show();
					}
				}
			}
		} catch (FileNotFoundException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	private void write(InputStream in, OutputStream out) throws IOException {
		byte[] data = new byte[1024];
		int count;
		while((count = in.read(data, 0, 1024)) != -1) {
			out.write(data,0,count);
		}
		out.close();
		//in.close();
	}
	
}
