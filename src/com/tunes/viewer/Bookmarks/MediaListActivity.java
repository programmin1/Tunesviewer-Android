package com.tunes.viewer.Bookmarks;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.tunes.viewer.ItunesXmlParser;
import com.tunes.viewer.R;
import com.tunes.viewer.FileDownload.DownloaderTask;
import com.tunes.viewer.WebView.JSInterface;

import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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
	private ArrayAdapter<MediaFile> _adapter;
	private File _folder;
	private ArrayList<MediaFile> items;

    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	// Clean the name of the podcast (eg "some podcast for iphone/ipod")
    	String podcast = DownloaderTask.clean(this.getIntent().getData().toString());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.medialist);
		items = new ArrayList<MediaFile>();
		_folder = new File(prefs.getString(
				/*download directory*/
				"DownloadDirectory", getString(R.string.defaultDL)),
				/*directory of this podcast page passed in intent:*/
				podcast);
		if (_folder.exists() && _folder.isDirectory()) {
			File[] dir = _folder.listFiles();
			for (File media : dir) {
				if (!media.isDirectory()) {
					items.add(new MediaFile(media,this));
				}
			}
			
			//Sort to make sure items are ordered by their metadata number.
			Collections.sort(items);
		}
		setTitle(podcast);
		_adapter = new ArrayAdapter<MediaFile>(this, R.layout.bookmark, R.id.item_title, items);
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
 	        menu.setHeaderTitle(items.get(info.position).getFile().getName());

 		    menu.add(0,1,1,"View details");
 			menu.add(0,2,2,R.string.delete);
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
	}

	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		MediaFile selected = items.get(info.position);
		switch(item.getItemId()) {
		case 1:
		{
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("title:"+selected._title+
					"\ntrack:"+selected._numberOrder+
					"\nartist:"+selected._artist+
					"\ndisplay:"+selected._display+
					"\nduration:"+selected._duration);
			builder.setTitle("Info");
			builder.show();
			break;
		}
		case 2:
		{
			selected.getFile().delete();
			_adapter.remove(selected);
			_adapter.notifyDataSetChanged();
			break;
		}
		}
		
		
		/*Unfortunately JAudiotagger doesn't work.
		  See https://java.net/jira/browse/JAUDIOTAGGER-303
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
		
		
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String file = items.get(position).getFile().toString();
		try {
			JSInterface.previewIntent("file://"+file, this);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, getText(R.string.NoActivity), Toast.LENGTH_LONG).show();
		}
	}
}

/**
 * Simple container for file information.
 * @author luke
 *
 */
class MediaFile implements Comparable<MediaFile> {
	// Immutable file:
	private File _file;
	public int _numberOrder;
	public String _artist;
	public String _title;
	public String _display;
	public String _duration;
	 //Objects for retreiving podcast metadata
    static Uri media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    // select columns:
    static String[] columns = {
                    MediaStore.Audio.Media._ID,                        // 0
                    MediaStore.Audio.Media.ARTIST,                // 1
                    MediaStore.Audio.Media.TITLE,                // 2
                    MediaStore.Audio.Media.DATA,                // 3
                    MediaStore.Audio.Media.DISPLAY_NAME,// 4
                    MediaStore.Audio.Media.DURATION,	// 5
                    MediaStore.Audio.Media.TRACK};        // 6
    // where:
    static final String selection = MediaStore.Audio.Media.DATA + " == ?";

	public MediaFile(File file, MediaListActivity parent) {
		_file = file;
		_numberOrder = 0;
		_artist = "";
		_title = "";
		_display = "";
		_duration = "";
		
		Cursor cursor;
		try {
			cursor = parent.managedQuery(media,
			        columns,
			        selection,
			        new String[] {file.getCanonicalPath().toString()},
			        null);

			while(cursor.moveToNext()){
				_numberOrder = Integer.valueOf(cursor.getString(6));
				_artist = cursor.getString(1);
				_title = cursor.getString(2);
				_display = cursor.getString(4);
				_duration = cursor.getString(5);
				_numberOrder = cursor.getInt(6);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public File getFile() {
		return _file;
	}

	@Override
	public int compareTo(MediaFile another) {
		int comparison = Integer.valueOf(_numberOrder).compareTo(another._numberOrder);
		if (comparison == 0) {
			// If same track number, sort by name display:
			return _display.compareTo(another._display);
		} else {
			return comparison;
		}
	}
	
	@Override
	public String toString() {
		return _file.getName();
	}
}