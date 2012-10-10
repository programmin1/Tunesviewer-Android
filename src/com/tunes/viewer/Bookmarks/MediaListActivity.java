package com.tunes.viewer.Bookmarks;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.tunes.viewer.ItunesXmlParser;
import com.tunes.viewer.R;
import com.tunes.viewer.FileDownload.DownloaderTask;

/**
 * A class to show all of a podcast's media.
 * Tap - opens media.
 * Hold, select Delete - deletes media. 
 * 
 * @author luke
 *
 */
public class MediaListActivity extends ListActivity {

	private static final String TAG = "MediaListActivity";
	// The order, id of the context menu items: 
	private static final int IDdetail = 1;
	private static final int IDdelete = 2;
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
				if (!media.isDirectory() && !media.getName().equals(DownloaderTask.PODCASTDIR_FILE)/* Not the marker file */
						&& !media.getName().startsWith(".")/* Not a 0-length file marking file in use */) {
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

 		    menu.add(0,IDdetail,IDdetail,"View details");
 			menu.add(0,IDdelete,IDdelete,R.string.delete);
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
	}

	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final MediaFile selected = items.get(info.position);
		switch(item.getItemId()) {
		case IDdetail:
		{
			Builder builder = new AlertDialog.Builder(this);
			//TODO: Should be moved to strings:
			builder.setMessage("title: "+selected._title+
					"\ntrack: "+selected._numberOrder+
					"\nartist: "+selected._artist+
					"\nduration: "+ItunesXmlParser.timeval(selected._duration));
			builder.setTitle(selected._display);
			builder.show();
			break;
		}
		case IDdelete:
		{
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.deleteFile).replace("%%",
					DownloaderTask.filesize(selected.getFile().length())));
			builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					selected.getFile().delete();
					_adapter.remove(selected);
					_adapter.notifyDataSetChanged();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.show();
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
		try {
			startActivity(DownloaderTask.openFile(items.get(position).getFile()));
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
	public boolean _downloading; //TODO: Maybe mark as not fully downloaded in the list? But movies do work when partially downloaded...
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
		_downloading = false;
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
			if (new File(file.getParent(), "."+file.getName()).exists()) {
				_downloading = true;
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
