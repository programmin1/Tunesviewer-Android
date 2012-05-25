package com.tunes.viewer.Bookmarks;
import com.tunes.viewer.R;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class BookmarkActivity extends ListActivity {

	private String _url;
	private String _title;
	private Uri _myuri;
	private Cursor cursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.bookmarkview);
	    cursor = managedQuery(Uri.parse("content://"+Bookmark.AUTHORITY+"/bookmarks"), 
	    		new String[] {"_id",Bookmark.TITLE},
	    		null, null, Bookmark.DEFAULT_SORT_ORDER);
	    
	    setListAdapter(new SimpleCursorAdapter(this, R.layout.bookmarkitem, cursor,
                new String[] { Bookmark.TITLE }, new int[] { android.R.id.text1 }));
	    findViewById(R.id.addBookmark).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ContentValues values = new ContentValues();
				values.put("url", _url);
				values.put("title", _title);

                // When the update completes,
                // the content provider will notify the cursor of the change, which will
                // cause the UI to be updated.
                getContentResolver().update(Uri.parse("content://"+Bookmark.AUTHORITY+"/bookmarks"), values, null, null);
                cursor.requery();
			}
		});
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		_url = this.getIntent().getExtras().getString("url");
		_title = this.getIntent().getExtras().getString("title");
		_myuri = this.getIntent().getData();
		if (_title != null && _url != null && !_title.equals("TunesViewer")) {
			((Button)findViewById(R.id.addBookmark)).setText("Add Bookmark: "+_title);
		} else {
			findViewById(R.id.addBookmark).setVisibility(View.GONE);
		}
	}

}
