package com.tunes.viewer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A search activity
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
 *
 */
public class Searcher extends Activity {
	
	private EditText _text;
	private SharedPreferences _prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchbox);
		_text = (EditText)findViewById(R.id.searchText);
		_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Button searchb = (Button) findViewById(R.id.SearchButton);
		searchb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				search();
			}
		});
		_text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
			   search();
				return true;
			}
			return false;
		}
		});
	}
	/*
	 * For search parameters see:
	 * http://ax.init.itunes.apple.com/WebObjects/MZInit.woa/wa/initiateSession
	 * To search all:
	 * http://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?submit=media&restrict=true&term=math&media=all
	 * to search itunesu:
	 * http://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?entity=allITunesUPlaylist&term=math&media=all
	@Override
	protected void onResume() {
		super.onResume();
		findViewById(R.id.searchText).requestFocus();
		EditText editText = (EditText) findViewById(R.id.searchText);
		InputMethodManager mgr = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
		// only will trigger it if no physical keyboard is open
		mgr.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
	}*/
	
	/**
	 * Calls the intent to search iTunesU
	 * @param searchString
	 */
	public void search() {
		String gotourl = null;
		String terms = Uri.encode(_text.getText().toString());//search terms
		boolean Ucourse = ((CheckBox)findViewById(R.id.checkCourse)).isChecked();
		boolean Ucollection = ((CheckBox)findViewById(R.id.checkCollection)).isChecked();
		boolean podcast = ((CheckBox)findViewById(R.id.checkPodcast)).isChecked();
		String ua = _prefs.getString("UserAgent", "iTunes-iPhone/1.2.0");// sometimes not initialized preference?
		if (ua.indexOf("-")>-1) {
			//mobile mode
			// http://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?displayIndex=2&entity=iTunesUCollection&term=Math&media=all#here
			// http://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?displayIndex=2&entity=iTunesUMaterial&term=App&media=all#here
			if (Ucollection && !podcast) {
				gotourl = "itms://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?entity=iTunesUPodcast&term="+terms+"&media=all";
			} else if (Ucourse && !podcast) {
				gotourl = "itms://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?entity=iTunesUCourse&term="+terms+"&media=all";
			} else if (podcast && !Ucollection && !Ucourse) {
				gotourl = "itms://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?submit=media&term="+terms+"&media=podcast"; 
			} else {
				gotourl = "itms://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?submit=media&restrict=true&term="+terms+"&media=all";
			}
		} else {
			if (podcast) {
				gotourl = "itms://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?submit=media&term="+terms+"&media=podcast";
			} else {
				gotourl = "itms://ax.search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?submit=media&restrict=true&term="
				+terms+"&media=iTunesU";
			}
			
		}
		Intent intent = new Intent(Intent.ACTION_VIEW ,
			Uri.parse(gotourl));
			startActivity(intent);
			//finish();
	}
}
