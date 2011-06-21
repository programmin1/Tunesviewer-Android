package com.tunes.viewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Searcher extends Activity {
	
	private EditText _text;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchbox);
		_text = (EditText)findViewById(R.id.searchText);
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
		Intent intent = new Intent(Intent.ACTION_VIEW ,
			Uri.parse("itms://search.itunes.apple.com/WebObjects/MZSearch.woa/wa/search?media=iTunesU&submit=media&term="
					+Uri.encode(_text.getText().toString())));
			startActivity(intent);
			finish();
	}
}
