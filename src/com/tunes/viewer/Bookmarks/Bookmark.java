package com.tunes.viewer.Bookmarks;

import android.net.Uri;
import android.provider.BaseColumns;

/*
 * Database will have:
 * pk - primary key
 * title - the name of the bookmark.
 * url - the url.
 * 
 * @author Luke
 *          based on SDK notepad database example.
 *
 */
public class Bookmark implements BaseColumns{
	
	public static final String AUTHORITY = "com.tunes.viewer.bookmarks";

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/bookmarks");

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";

    /**
     * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "title";

    /**
     * The title of the note
     * <P>Type: TEXT</P>
     */
    public static final String TITLE = "title";

    /**
     * The url
     * <P>Type: TEXT</P>
     */
    public static final String URL = "url";

}
