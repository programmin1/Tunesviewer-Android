package com.tunes.viewer;
//http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;

/**
 * iTunesU XML parser using Java SaxParser
 * This will parse both mobile (iPhone) XML and desktop (iTunes) XML. 
 * 
 * After parsing with this class, getRedirect() will give you the redirect, if this is a redirect page.
 * If the page describes a download, parser.getUrls().size()==1, and the item's name is getSingleName().
 * Otherwise, use getHTML() to get the generated document.
 * If the page is already HTML, this will throw SAXException.
 * 
 * @author Luke Bryan
 *
 */
public class ItunesXmlParser extends DefaultHandler {

	// HTML before and after the media list:
	private static final String PRE_MEDIA = "<script>function downloadit(title,name) { console.log('download-it'); console.log(title); console.log(name); window.DOWNLOADINTERFACE.download(title,name); }</script>\n"+
	"<style>* {font-family: Helvetica, Arial;}\n"+
	"tr.dl > td {background-image: -webkit-gradient(linear, left bottom, left top, color-stop(0.48, rgb(215,239,245)), color-stop(1, white));}\n"+
	"tr.selection {background:gold;}\n</style>"+
	"<table width='100%' border='1' bgcolor='white' cellspacing=\"1\" cellpadding=\"3\"><tr bgcolor='CCCCCC'><td><b>Name</b></td><td><b>Author</b></td><td><b>Duration</b></td><td><b>Comment</b></td><td><b>Download</b></td></tr>\n";
	private static final String POST_MEDIA = "</table>";
	private static final String TAG = "parser";
	
	// Holds text inside or between the elements,
	// this is printed out and reset at start-tag and end-tag.
	private StringBuilder innerText;
	
	// All of the html/xml original:
	private StringBuilder original;
	
	// Converted html for use in the browser:
	private StringBuilder html;
	
	// This handles the key-value pairs in <dict> tag,
	// such as <key>keyname</key><valuetag>value</value>
	private HashMap<String,String> map;
	//The same, but for sub-map <dict><key/><othervalues/></dict>
	private HashMap<String,String> subMap;
	
	private String lastElement;
	private String lastValue;
	
	private String redirectPage;
	private String backColor;
	private StringBuilder media;
	
	private ArrayList<String> urls;
	private String singleName;
	
	// The specific item id.
	private String reference;
	
	private Context context;
	
	//Any unused value for <key>identifier</key><dict>... storage of key in <dict>.
	private final String KEY = "_KEY_";
	
	// This stack holds current location in the document, 
	// For example, in <document><element><anotherelement> section,
	// document, then element, then anotherelement would be pushed.
	// an end tag pops the element.
	private Stack<StackElement> docStack;
	//(ArrayDeque is only available in newer Android.)
	
	// When true, it's ignoring the <Test comparison='lt' oldversion></Test> values.
	private boolean ignoring = false;
	
	public String toString() {
		return original.toString();
	}
	public String getHTML() {
		return html.toString();
	}
	/**
	 * Returns the redirect if this was a redirect page, otherwise blank "".
	 * @return
	 */
	public String getRedirect() {
		return redirectPage;
	}
	
	/**
	 * Returns urls available
	 * @return
	 */
	public ArrayList<String> getUrls() {
		return urls;
	}
	/**
	 * The name of the single file, when this page describes one file download.
	 * @return
	 */
	public String getSingleName() {
		return singleName;
	}
	/**
	 * Constructs xml parser.
	 * @param reference is the selected part of the document, the text in #id at the end.
	 */
	public ItunesXmlParser(String reference, Context c) {
		this.reference = reference;
		this.context = c;
	}
	
	@Override
	public void startDocument() throws SAXException {
		original = new StringBuilder();
		html = new StringBuilder(1024);
		innerText = new StringBuilder(200);
		media = new StringBuilder(200);
		docStack = new Stack<StackElement>();
		
		redirectPage = "";
		lastElement = "";
		lastValue = "";
		backColor = "";
		map = new HashMap<String, String>();
		subMap = new HashMap<String, String>();
		urls = new ArrayList<String>();
	}

	@Override
	public void startElement(String namespaceURI, String elname,
	                         String qName, Attributes atts) throws SAXException {
		//original.append(innerText);
		StackElement thisEl = new StackElement(elname,atts);
		//original.append(thisEl);
		if (elname.equals("html") && docStack.size()==0) {
			// Even if it happens to parse correctly as xml, HTML should be shown directly!
			throw new SAXException();
		}
		
		// Elements handler. Convert elements except for old-version info and lone FontStyle tags that make empty space.
		if (!ignoring && !elname.equals("FontStyle")) {
			html.append(innerText); //between elements text
			if (elname.equals("Test")) {
				ignoring = shouldIgnore(thisEl.atts);
			}
			
			if (atts.getValue("backColor") != null && backColor=="") {
				backColor = atts.getValue("backColor");
			}
			
			if (!docStack.empty()) { // Action dependent on parent:
				String parentName = docStack.peek().name;
				if (parentName.equals("HBoxView")) {
					html.append("<td>");//new col;
				} else if (parentName.equals("VBoxView")) {
					html.append("<tr><td>");
				}
				if (elname.equals("dict") && docStack.peek().atts.containsKey(KEY)) {
					/**
					 * <dict>ionary of keys inherits key identifier.
					 * For example, dict should be key "artwork-urls" here:
					 * <key>artwork-urls</key>
					 * <array><dict>data...</dict><dict>data...</dict>
					 */
					thisEl.atts.put(KEY, docStack.peek().atts.get(KEY));
				}
				if (lastElement.equals("key")) { //Store keyname of this value element if it has its own.
					thisEl.atts.put(KEY, lastValue);
				}
			}
			
			if (elname.equals("dict") && isHandled(thisEl)) {//move to endelement.
				// New dict, not sub-section, so make new key-val maps:
				map.clear();
				subMap.clear();
				lastElement="";
			} else if (elname.equals("HBoxView")) {
				html.append("<!--HBox--><table><tr>");
			} else if (elname.equals("VBoxView")) {
				html.append("<!--VBox--><table>");
			} else if (elname.equals("GotoURL") || elname.equals("OpenURL")) {
				html.append("<a href=\"");
				html.append(atts.getValue("url"));
				html.append("\">");
			} else if (elname.equals("PictureView")) {
				html.append("<img src=\"");		//Stringbuilder for best performance
				if (atts.getValue("url")!=null) {
					html.append(atts.getValue("url"));
				} else if (atts.getValue("src")!=null) {
					html.append(atts.getValue("src"));
				}
				html.append("\" height=\"");
				html.append(atts.getValue("height"));
				html.append("\" width=\"");
				html.append(atts.getValue("width"));
				html.append("\" alt=\"");
				html.append(atts.getValue("alt"));
				html.append("\">");
			} else if (!(elname.equals("string") || elname.equals("key") || elname.equals("MenuItem"))) {
				//Text shown:
				html.append("<");
				html.append(elname);
				html.append(">");
			}
		}
		//Must be run every time:
		innerText.setLength(0);
		docStack.push(thisEl);
	}

	@Override
	public void endElement(String uri, String elname, String qName) throws SAXException {
		//original.append(innerText);
		StackElement thisEl = docStack.pop();
		assert(elname.equals(thisEl.name));
		
		// Elements handler. Mirror image of the one in StartElement
		if (!ignoring && !elname.equals("FontStyle")) {
			if (lastElement.equals("key")) {
				if (!docStack.empty() && !isHandled(docStack.peek())) {
					/**
					 * It goes in seperate subMap, so it won't overwrite, for example:
					 * <key>url</key><string/>
					 * <key>artwork-url</key><dict>
					 *  <key>url</key><string (imageurl)/> <- this should be stored in subMap.
					 */
					subMap.put(lastValue, innerText.toString());
				} else {
					map.put(lastValue, innerText.toString());
				}
				if (lastValue.equals("URL")) {
					urls.add(innerText.toString());
				} else if (lastValue.equals("songName")) {
					singleName = innerText.toString();
				}
			} else if (elname.equals("dict") && isHandled(thisEl)) {
				//End of key-val definition.
				String type = "";
				if (map.containsKey("type")) {//add type=separator
					type = map.get("type");
				}
				if (map.containsKey("kind") && (map.get("kind").equals("Goto") || map.get("kind").equals("OpenURL")) &&
				    map.containsKey("url")) {
					//This is a redirect.
					redirectPage = map.get("url");
				} else if (type.equals("tab")) {
					if (map.get("active-tab").equals("1")) {
						html.append("<a class='tab sel' href=\"");
					} else {
						html.append("<a class='tab' href=\"");
					}
					html.append(map.get("url"));
					html.append("\">");
					html.append(map.get("title"));
					html.append("</a>");
				} else if (type.equals("squish")) {// An image link
					html.append("<a href=\"");
					html.append(map.get("url"));
					html.append("\"><img src=\"");
					html.append(subMap.get("url"));
					html.append("\"></a>");
				} else if (type.equals("separator")) {
					html.append("<div class='separator' width=100% style='border: 2px;padding: 2px;border-style: solid;'>");
					if (map.containsKey("title")) {
						html.append(map.get("title"));
					} else {
						html.append("&nbsp;");
					}
					html.append("</div>");
				} else if (type.equals("link")) { //A link to page
					html.append("<div class='link'><a href=\"");
					html.append(map.get("url"));
					html.append("\"><img style='vertical-align: top; margin:2px; float:left;' src=\"");
					html.append(subMap.get("url"));
					html.append("\">");
					html.append(map.get("title"));
					html.append("</a></div>");
				} else if (type.equals("podcast")) { // page info.
					html.append("<h2><a href=\"");
					html.append(map.get("url"));
					html.append("\"><img src=\"");
					html.append(subMap.get("url"));
					html.append("\"><br>");
					html.append(map.get("title"));
					html.append("</a></h2><p>");
					html.append(map.get("description"));
					html.append("</p>");
				} else if (type.equals("podcast-episode")) {
					//html.append("<script>function downloadit(title,name) { console.log('download-it'); console.log(title); console.log(name); window.DOWNLOADINTERFACE.download(title,name); }</script>\n");
					html.append("<div class='media' onclick='toggle(this.nextSibling)'>");
					//stoppropagation to prevent clicking container, then download:
					html.append("<a class='media' style='float:right' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.download(this.getAttribute('title'),this.getAttribute('url'));\" title=\"");
					html.append(map.get("title").replace("\"", "&quot;"));
					html.append("\" url=\"");
					html.append(subMap.get("asset-url").replace("\"", "&quot;"));
					html.append("\">Download ");
					html.append(fileExt(subMap.get("asset-url")));
					html.append("</a><a href='javascript:;' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.preview(this.getAttribute('title'),this.getAttribute('url'));\" title=\"");
					html.append(map.get("title").replace("\"", "&quot;"));
					html.append("\" url=\"");
					html.append(subMap.get("asset-url").replace("\"", "&quot;"));
					html.append("\">preview</a><b class='media'>");
					html.append(map.get("title"));
					html.append("</b></div><div style='display:none'><b>");
					if (subMap.containsKey("duration")) {
						html.append("Duration: ");
						html.append(timeval(subMap.get("duration")));
					}
					if (map.containsKey("copyright")) {
						html.append(" Copyright: ");
						html.append(map.get("copyright"));
					}
					html.append("</b><br>");
					html.append(map.get("long-description"));
					html.append("<br></div>");
				} else if (map.containsKey("songName") || map.containsKey("itemName")) {
					addMediaRow();
				}
				//Done with it, don't let parent repeat:
				map.clear();
				subMap.clear();
			} else if (elname.equals("HBoxView")) {
				html.append("</tr></table>");
			} else if (elname.equals("VBoxView")) {
				html.append("</table>");
			} else if (elname.equals("GotoURL") || elname.equals("OpenURL")) {
				html.append(innerText);
				html.append("</a>");
			} else if (elname.equals("PictureView")) {
				html.append("</img>");
			} else if (elname.equals("PathElement")) {
				html.append("<a href=\"");
				html.append(innerText);
				html.append("\">");
				html.append(thisEl.atts.get("displayName"));
				html.append("</a>");
				//html.append(String.format("<a href=\"%s\">%s</a>",innerText,thisEl.atts.get("displayName")));
			} else if (!(elname.equals("string") || elname.equals("key") || elname.equals("MenuItem"))) {
				//Text shown:
				html.append(innerText);
				html.append("</");
				html.append(elname);
				html.append(">");
			}
			lastElement = elname;
			lastValue = innerText.toString();
			
			if (!docStack.empty()) {
				String parentName = docStack.peek().name;
				if (parentName.equals("HBoxView")) {
					html.append("</td>");//end col;
				} else if (parentName.equals("VBoxView")) {
					html.append("</td></tr>");
				}
			}
		} else if (shouldIgnore(thisEl.atts)) {
			//Just got the ending tag of this ignored element, reset to normal:
			ignoring = false;
		}
		//Must be run every time:
		//original.append("</");
		//original.append(elname);
		//original.append(">");
		innerText.setLength(0);
	}

	/**
	 * Adds a row representing the file to the media variable.
	 */
	private void addMediaRow() {
		String name = "";
		String artist = "";
		String duration = "";
		String comments = "";
		//String rtype = "";
		String url = "";
		String directurl = "";
		//String releaseDate = "";
		//String modifiedDate = "";
		String id = "";
		String style = "dl";
		if (map.containsKey("songName")) {
			name = map.get("songName");
		}
		if (map.containsKey("itemName")) {
			name = map.get("itemName");
		}
		if (map.containsKey("artistName")) {
			artist = map.get("artistName");
		}
		if (map.containsKey("duration")) {
			duration = map.get("duration");
		}
		if (map.containsKey("comments")) {
			comments = map.get("comments");
		} else if (map.containsKey("description")) {
			comments = map.get("description");
		} else if (map.containsKey("longDescription")) {
			comments = map.get("longDescription");
		}
		if (map.containsKey("url")) {
			url = map.get("url");
		}
		if (map.containsKey("previewURL")) {
			directurl = map.get("previewURL");
		} else if (map.containsKey("episodeURL")) {
			directurl = map.get("episodeURL");
		} else if (map.containsKey("preview-url")) {
			directurl = map.get("preview-url");
		}
		if (map.containsKey("itemId")) {
			id = map.get("itemId");
		}
		if (id.equals(reference)) {
			style = "dl selection";
		}
		if (!directurl.equals("") && directurl.lastIndexOf(".")>-1) { //valid row:
		 media.append(String.format(
		 "<tr class=\"%s\" onClick=\"downloadit(this.getAttribute('name'),this.getAttribute('url'));\" name=\"%s\" url=\"%s\"><td><a name=\"%s\">%s</a></td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n",
		 style,name.replace("\"", "&quot;"),directurl.replace("\"", "&quot;"),id,name,artist,timeval(duration),comments,directurl.substring(directurl.lastIndexOf("."))));
		}
	}
	
	/**
	 * Same as DownloaderTask.fileExt.
	 * @param url
	 * @return
	 */
	public static String fileExt(String url) {
		String ext = url.substring(url.lastIndexOf(".") );
		if (ext.indexOf("?")>-1) {
			ext = ext.substring(0,ext.indexOf("?"));
		}
		if (ext.indexOf("%")>-1) {
			ext = ext.substring(0,ext.indexOf("%"));
		}
		return ext;
	}
	
	/**
	 * Given attributes of <Test> tag, this returns true if it needs to be ignored, old-version code,
	 * @param atts
	 * @return True if this element and child elements should be ignored.
	 */
	private boolean shouldIgnore(HashMap<String,String> atts) {
		if (atts.get("comparison")!=null) {
			return (atts.get("comparison").equals("lt") || ((String) atts.get("comparison")).indexOf("less")>-1);
		} else {
			return false;
		}
	}
	
	/**
	 * Gives the time value in min:sec format, given milliseconds.
	 * @param ms
	 * @return
	 */
	public static String timeval(String ms) {
		String out = ms;
		try {
			long sec = Long.valueOf(ms)/1000;
			out = String.valueOf(sec/60)+":";
			if ((sec % 60) < 10) {
				out += String.valueOf(sec % 60);
			} else {
				out += "0"+String.valueOf(sec % 60);
			}
		} catch (NumberFormatException e) {
			Log.e(TAG,"Unable to convert time value: \""+out+"\".");
		}
		return out;
	}

	public void characters (char ch[], int start, int len) {
		for (int i = start; i < start + len; i++)
		{
			innerText.append(ch[i]);
		}
	}
	
	public void endDocument() throws SAXException {
		//if mobile:
		html.insert(0,context.getString(R.string.MobileStyles));
		original.append("<!-- (END DOC) -->");
		html.insert(0, String.format("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head><body bgcolor=\"%s\">",backColor));
		if (media.length()>0) {
			html.append(PRE_MEDIA);
			html.append(media);
			html.append(POST_MEDIA);
		}
		html.append("</body></html>");
	}
	
	/**
	 * Returns true when <key>keyid</key><dict... is specifically supported by this parser.
	 * (The dict map should be cleared when starting/ending <dict> tag in this case).
	 * 
	 * @param keyid
	 * @return true if these key-vals should be handled in map, and subelements in submap.
	 */
	private boolean isHandled(StackElement element) {
		String keyid = element.atts.get(KEY);
		return keyid != null && (keyid.equals("action") || keyid.equals("items") || keyid.equals("item-metadata") || keyid.equals("tabs") || keyid.equals("squishes") || keyid.equals("content"));
	}
	
	/**
	 * Container class to represent a tag and its attributes.
	 */
	private class StackElement {
		public String name;
		public HashMap<String,String> atts;
		
		public StackElement(String name, Attributes a) {
			this.name = name;
			atts = new HashMap<String,String>();
			for (int i=0; i<a.getLength(); i++) {
				atts.put(a.getLocalName(i),a.getValue(i));
			}
		}
		
		public String toString() {
			StringBuilder out = new StringBuilder();
			out.append("<");
			out.append(name);
			if (atts.size()>0) {
				out.append(" ");
				out.append(atts.toString());
			}
			out.append(">");
			return out.toString();
		}
	}
}
