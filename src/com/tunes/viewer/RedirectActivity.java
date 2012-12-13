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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
public class RedirectActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		String uri = this.getIntent().getData().getPath();
		Toast.makeText(getApplicationContext(), "YEEHAA"+uri.toString(), Toast.LENGTH_LONG).show();
		if (ItunesXmlParser.fileExt(uri).equals(".pages")) {
			try {
				File infile = new File(uri);
				ZipInputStream zis = new ZipInputStream(new FileInputStream(infile));
				ZipEntry ze;
				while ((ze = zis.getNextEntry()) != null) {
					if (ze.getName().equals("QuickLook/Thumbnail.jpg")) {
						File outfile = new File(uri.substring(0,uri.lastIndexOf("."))+".jpg" );
						write(zis, new BufferedOutputStream( new FileOutputStream(outfile) ));
					} else if (ze.getName().equals("QuickLook/Preview.pdf")) {
						File outfile = new File(uri.substring(0,uri.lastIndexOf("."))+".pdf" );
						write(zis, new BufferedOutputStream( new FileOutputStream(outfile) ));
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
