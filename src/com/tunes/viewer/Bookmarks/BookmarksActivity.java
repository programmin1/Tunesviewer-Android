package com.tunes.viewer.Bookmarks;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.tunes.viewer.R;
import com.tunes.viewer.Searcher;
import com.tunes.viewer.TunesViewerActivity;

import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A database list based bookmarks list activity. 
 * Based on the Custom ListView Database Example at:
 * http://joesapps.blogspot.com/2011/02/customized-listview-data-display-in.html
 * 
 * TODO: Maybe import/export to copy to/from SD, maybe add some sort of 
 * subscription, or display "n / n downloaded"?
 */
public class BookmarksActivity extends ListActivity implements OnItemClickListener{

	private static final String TAG = "BookmarksActivity";
	private com.tunes.viewer.Bookmarks.DbAdapter dbHelper;
	private Cursor listCursor;
	private MyListAdapter myAdapter;
	private String _title;
	private String _url;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.bookmarkslist);
	    setTitle(getText(R.string.bookmarks));

        dbHelper = new DbAdapter(this);
        dbHelper.open();
        
        // Get a Cursor for the list items
        listCursor = dbHelper.fetchBookmarks();
        startManagingCursor(listCursor);
        
        // set the custom list adapter
        myAdapter = new MyListAdapter(this, listCursor);
        setListAdapter(myAdapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnCreateContextMenuListener(this);
        findViewById(R.id.addButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dbHelper.insertItem(_title, _url, 0);
				listCursor.requery();
				findViewById(R.id.addButton).setVisibility(View.GONE);
			}
		});
	    
        
	}
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Button addBookmark = (Button) (findViewById(R.id.addButton));
    	_url = getIntent().getExtras().getString("url");
    	_title = getIntent().getExtras().getString("title");
    	if (_url != null && _title != null && !_title.equals("TunesViewer")
    		&& !dbHelper.hasUrl(_url)) {
    		addBookmark.setText("Add "+_title);
    	} else {
    		addBookmark.setVisibility(View.GONE);
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	dbHelper.close();
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
             Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
             if (cursor == null) {
                 // For some reason the requested item isn't available, do nothing
                 Log.e(TAG,"No item selected?");
             } else {
     	
     	        // Setup the menu header
     	        menu.setHeaderTitle(cursor.getString(1));
     	
     	        // Add a menu item to delete the note
     	        //menu.add(0, Menu.FIRST, 0, "Delete");
     	        menu.add(0,1,1,R.string.delete);
     	        menu.add(0,2,2,R.string.showFiles);
             }
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        Toast.makeText(getApplicationContext(), "show?"+item.getItemId(), 1000).show();
        switch (item.getItemId()) {
        
            case 1: {
                // Delete the note that the context menu is for
                //Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                dbHelper.deleteTitle(info.id);

                listCursor.requery();
                return true;
            }
            case 2: {
            	Intent intent = new Intent(getApplicationContext(),MediaListActivity.class);
            	Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
            	
                if (cursor != null) {
	            	intent.setData(Uri.parse(cursor.getString(1)));
	    			startActivity(intent);
                }
    			return true;
            }
        }
        return false;
    }

    
    private class MyListAdapter extends ResourceCursorAdapter {
        
        public MyListAdapter(Context context, Cursor cursor) {
            super(context, R.layout.bookmark, cursor);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	TextView title = (TextView) view.findViewById(R.id.item_title);
        	title.setText(cursor.getString(
        				cursor.getColumnIndex(DbAdapter.COL_TITLE)));
        }

    }


	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		String url = listCursor.getString(2);
		Log.i(TAG,url);
		if (!url.startsWith("itms")) {
			url = "itms"+url.substring(4);
		}
		startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
	}

    

}
