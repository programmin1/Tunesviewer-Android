package com.tunes.viewer.Bookmarks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.tunes.viewer.R;
import com.tunes.viewer.FileDownload.DownloaderTask;

/**
 * A database list based bookmarks list activity. 
 * Based on the Custom ListView Database Example at:
 * http://joesapps.blogspot.com/2011/02/customized-listview-data-display-in.html
 * 
 * TODO: Maybe import/export to copy to/from SD, maybe add some sort of 
 * subscription, or display "n / n downloaded"?
 */
public class BookmarksActivity extends ListActivity implements OnItemClickListener{

	static final String TAG = "BookmarksActivity";
	// Order, ID, of the context menu items:
	private static final int IDshow = 1;
	private static final int IDdelete = 2;
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
				dbHelper.insertItem(_title, _url);
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
    	if (_url != null && _title != null 
    		&& !_title.equals("TunesViewer") && !_title.equals(getString(R.string.loading))
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
     	        menu.add(0,IDdelete,IDdelete,R.string.delete);
     	        menu.add(0,IDshow,IDshow,R.string.showFiles);
             }
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info;
        final Context context = this;
        try {
        	info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
	        switch (item.getItemId()) {
	            case IDdelete: {
	                // Delete the note that the context menu is for
	                //Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
	            	Builder builder = new AlertDialog.Builder(this);
	            	builder.setTitle(R.string.bookmarkDeleteTitle);
	            	builder.setPositiveButton(R.string.deleteBookmarked, new DialogInterface.OnClickListener() {
						@Override // Delete All files, and remove bookmark:
						public void onClick(DialogInterface dialog, int which) {
			                Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
			                String podcast = cursor.getString(1);
			                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
							File folder = new File(prefs.getString(
			        				/*download directory*/
			        				"DownloadDirectory", getString(R.string.defaultDL)),
			        				/*directory of this podcast page passed in intent:*/
			        				DownloaderTask.clean(podcast));
							if (folder.exists() && folder.isDirectory()) {
								for (File f : folder.listFiles()) {
									f.delete();
								}
								folder.delete();
							}
							
							dbHelper.deleteTitle(info.id);
			                listCursor.requery();
						}
					});
	            	builder.setNeutralButton(R.string.removeBookmark, new DialogInterface.OnClickListener() {
						@Override // Remove bookmark
						public void onClick(DialogInterface dialog, int which) {
			                dbHelper.deleteTitle(info.id);
			                listCursor.requery();
						}
					});
	            	builder.setNegativeButton(android.R.string.cancel, null);
	            	builder.show();
	                return true;
	            }
	            case IDshow: {
	            	Intent intent = new Intent(getApplicationContext(),MediaListActivity.class);
	            	Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
	            	
	                if (cursor != null) {
		            	intent.setData(Uri.parse(cursor.getString(1)));
		    			startActivity(intent);
	                }
	    			return true;
	            }
	        }

        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bookmarkmenu, menu);
		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	File dir = new File(Environment.getExternalStorageDirectory(), "Tunesviewer");
    	switch(item.getItemId()) {
    	case R.id.menuExport:
    		dir.mkdirs();
    		File output = new File(dir, "bookmarks.htm");
    		try {
				BufferedWriter outfile = new BufferedWriter(new FileWriter(output));
    			XmlSerializer xhtml = Xml.newSerializer();
    			xhtml.setOutput(outfile);
    			xhtml.startDocument("UTF-8", true);
    			xhtml.startTag("", "html");
    			xhtml.startTag("", "head");
    			xhtml.startTag("", "title");
    			xhtml.text("Bookmarks");
    			xhtml.endTag("", "title");
    			xhtml.endTag("", "head");
    			xhtml.startTag("", "body");
				Cursor c = dbHelper.fetchBookmarks();
				while (!c.isAfterLast()) {
					xhtml.startTag("", "b");
					xhtml.attribute("", "class", "title");
					xhtml.text(c.getString(1));
					xhtml.endTag("", "b");
					xhtml.startTag("", "a");
					xhtml.attribute("", "href", c.getString(2));
					xhtml.text("link");
					xhtml.endTag("", "a");
					xhtml.startTag("","br"); xhtml.endTag("","br");
					c.moveToNext();
				}
				c.close();
				xhtml.endTag("", "body");
				xhtml.endTag("", "html");
				xhtml.endDocument();
				outfile.close();
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
    		return true;
    	case R.id.menuImport:
    		 File input = new File(dir, "bookmarks.htm");
    		 try {
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(input));
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setValidating(false);
				SAXParser saxParser= factory.newSAXParser();
				XMLReader xr = saxParser.getXMLReader();
				xr.setContentHandler(new GetHTML(dbHelper));
				xr.parse(new InputSource(bis));
				
				listCursor.requery();
			} catch (FileNotFoundException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
    		return true;
    	}
    	return false;
    }

}

/**
 * A SAXParser class to read XHTML file of links and names, adding to bookmarks
 * database when needed, to import the saved bookmarks.
 * 
 * @author luke
 *
 */
class GetHTML extends DefaultHandler {
	public StringBuffer out;
	private DbAdapter _adapter;
	private String _name;
	private boolean _isTitletag;
	
	public GetHTML(DbAdapter adapter) {
		_adapter = adapter;
		out = new StringBuffer();
		_name = "";
		_isTitletag = false;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		_isTitletag = false;
		if (localName.equals("a") && attributes.getValue("href")!=null) {
			String link = attributes.getValue("href");
			// Now we have name and link.
			if (!_adapter.hasUrl(link)) {
				//insert into db:
				_adapter.insertItem(_name, link);
				Log.i(BookmarksActivity.TAG,"Inserting "+_name+", "+link);
			}
		} else if (attributes.getValue("class")!=null && attributes.getValue("class").equals("title")) {
			_isTitletag = true;
		} 
		out.setLength(0); // reset outertext.
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (_isTitletag) {
			_name = out.toString();
		}
		out.setLength(0); // reset innertext
	}
	
	public void characters(char[] buffer, int start, int length) {
		out.append(buffer, start, length);
	}
}