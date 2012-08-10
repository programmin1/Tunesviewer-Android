package com.tunes.viewer.Bookmarks;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.tunes.viewer.R;
import com.tunes.viewer.WebView.JSInterface;

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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.tunes.viewer.WebView.JSInterface;

/**
 * A class to show all of a podcast's media.
 * Tap - opens media.
 * Hold, select Delete - deletes media. 
 * 
 * TODO: The delete doesn't yet work correctly, it makes the rest of the list off by one.
 * Maybe a better adapter model needs to be used.
 * 
 * TODO: Haven't looked into setting list order based on metadata track-number data.
 * This would be a good idea.
 * 
 * @author luke
 *
 */
public class MediaListActivity extends ListActivity {

	private static final String TAG = "MediaListActivity";
	private List<String> name = null;
	private List<String> path = null;
	private ArrayAdapter<String> _adapter;

    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String podcast = this.getIntent().getData().toString();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.medialist);
		name = new ArrayList<String>();
		path = new ArrayList<String>();
		File container = new File(prefs.getString(
				/*download directory*/
				"DownloadDirectory", ""),
				/*directory of this podcast page passed in intent:*/
				podcast);
		if (container.exists() && container.isDirectory()) {
			File[] dir = container.listFiles();
			//Arrays.sort(dir);
			for (File media : dir) {
				if (!media.isDirectory()) {
					name.add(media.getName());
					path.add(media.getPath());
				}
			}
		}
		setTitle(podcast);
		_adapter = new ArrayAdapter<String>(this, R.layout.bookmark, R.id.item_title, name);
		setListAdapter(_adapter);
        getListView().setOnCreateContextMenuListener(this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
 	        // Setup the menu header
 	        menu.setHeaderTitle(name.get(info.position));
 	
 	        // Add a menu item to delete the note
 	        menu.add(0, Menu.FIRST, 0, R.string.delete);
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		//Don't need to switch on item.getItemId(), only one.
		new File(path.get(info.position)).delete();
		_adapter.remove(name.get(info.position));
		/*Unfortunately JAudiotagger doesn't work.
	 	try {
			AudioFile f = AudioFileIO.read(new File(path.get(info.position)));
			Tag tag = f.getTag();
			String out = tag.getFirst(FieldKey.TITLE)+tag.getFirst(FieldKey.TRACK);
			Toast.makeText(this, out, 10000).show();
		} catch (CannotReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ReadOnlyFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAudioFrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		path.remove(info.position);
		name.remove(info.position);
		
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String file = (path.get(position));
		String title = name.get(position);
		try {
			JSInterface.previewIntent("file://"+file, this);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, getText(R.string.NoActivity), Toast.LENGTH_LONG).show();
		}
	}
}
