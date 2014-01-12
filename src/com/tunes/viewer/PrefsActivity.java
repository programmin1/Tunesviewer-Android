package com.tunes.viewer;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
/**
 * TunesViewer Preferences
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
 *
 */
public class PrefsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final String DL = "DownloadDirectory";
	private static final String TAG = "PrefsActivity";
	SharedPreferences _prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		_prefs.registerOnSharedPreferenceChangeListener(this);
		this.setTitle("Preferences");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(DL)) {
			Log.d(TAG,_prefs.getString(DL, ""));
			final File dir = new File(sharedPreferences.getString(DL,
					Environment.getExternalStorageDirectory().getPath() ));
			final SharedPreferences p = sharedPreferences;
			if (!dir.exists()) {
				//Set it back to default.
				SharedPreferences.Editor edit = p.edit();
				edit.putString(DL,"/sdcard/");
				edit.commit();
				
				new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("No such directory")
				.setMessage("Do you want to create directory\n"+dir.toString())
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dir.mkdirs();
						//Set new dir:
						SharedPreferences.Editor edit = p.edit();
						edit.putString(DL,dir.toString());
						edit.commit();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.show();
			}
		}
	}
}
