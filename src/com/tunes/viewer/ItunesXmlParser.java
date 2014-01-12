package com.tunes.viewer;
//http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.os.Debug;
import android.util.Log;
import android.widget.Toast;

/**
 * iTunesU XML parser using Java SaxParser
 * This will parse both mobile (iPhone) XML and desktop (iTunes) XML. 
 * 
 * After parsing with this class, getRedirect() will give you the redirect, if this is a redirect page.
 * If the page describes a download, parser.getUrls().size()==1, and the item's name is getSingleName().
 * Otherwise, use getHTML() to get the generated document.
 * 
 * If the page is already HTML, this will throw SAXException.
 * 
 * Distributed under GPL2+
 * @author Luke Bryan 2011-2014
 *
 */
public class ItunesXmlParser extends DefaultHandler {
	
	/**
	 * Turn on to enable profiling.
	 * It seems this might also cause crashes due to excessive memory use sometimes, turn off if not needed!
	 */
	private final boolean _profiling = false;
	//Turn on to enable original-source parsing also:
	private final boolean _debug = false;

	// HTML style and scripts before and after the full-page xml-converted media list: (Main styles are in strings.xml)
	private static final String PRE_MEDIA = "<script>function downloadit(title,name) { console.log('download-it'); console.log(title); console.log(name); window.DOWNLOADINTERFACE.download(title, document.title ,name); }</script>\n"+
	"<style>* {font-family: Helvetica, Arial;}\n"+
	"tr.dl > td {}\n"+
	"tr.selection {background:gold;}\n"+
	"tr.selection > td {background-image:-webkit-gradient(linear, left bottom, left top, color-stop(0.48, #FEFF52), color-stop(1, gold));}</style>"+
	"<table width='100%' border='1' cellspacing=\"1\" cellpadding=\"3\"><tr><td><b>Name</b></td><td><b>Author</b></td><td><b>Duration</b></td><td><b>Comment</b></td><td><b>Download</b></td></tr>\n";
	private static final String POST_MEDIA = "</table>";
	private static final String TAG = "parser";
	
	//Url starting with this is not supported:
	private static final String IGNOREENROLL = "http://itunes.apple.com/WebObjects/DZR.woa/wa/iTunesEnroll";
	private static final String IGNOREENROLL2 = "https://itunes.apple.com/WebObjects/DZR.woa/wa/iTunesEnroll";
	
	// Holds mobile_extras file, which is added to the page. 
	public static String mobileExtras;
	
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
	
	//When non-empty, this is color of border of a heading.
	private String nextHeaderBorder;
	
	// The specific item-id.
	private String _reference;
	// The url of the page.
	private URL _url;
	private String _title;
	
	private Context _context;
	
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
	
	int _scrWidth;
	private int _imgPrefSize;
	private boolean _preview = false;
	
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
	public ItunesXmlParser(URL url, Context c, int width, int imgPref) {
		_url = url;
		_title = "";
		_reference = "";
		nextHeaderBorder = "";
		if (url.getQuery() != null) {
			String[] queries = url.getQuery().split("&");
			for (String q : queries) {
				if (q.startsWith("i=")) {
					_reference = q.substring(2);
				}
			}
		}
		_context = c;
		_scrWidth = width;
		_imgPrefSize = imgPref;
		
		InputStream inputStream = c.getResources().openRawResource(R.raw.mobile_extras);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		int i;
		try { // Read the Javascript file into memory.
			i = inputStream.read();
			while (i != -1) {
				byteArrayOutputStream.write(i);
				i = inputStream.read();
			}
	      inputStream.close();
	      mobileExtras = byteArrayOutputStream.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(c, "Couldn't open mobile_extras", Toast.LENGTH_LONG).show();
		}
		if (_profiling) {
			Debug.startMethodTracing("XML");
		}
	}
	
	@Override
	public void startDocument() throws SAXException {
		original = new StringBuilder(2048);
		html = new StringBuilder(2048);
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
		if (_debug) {
			original.append(innerText);
		}
		StackElement thisEl = new StackElement(elname,atts);
		if (_debug) {
			original.append(thisEl);
		}
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
			} else if (elname.equals("array")) {
				if (thisEl.atts.get(KEY) != null && thisEl.atts.get(KEY).equals("tabs")) {
					html.append("<!-- tabs --><div style='margin:10px; text-align:center'>");
				}
			} else if (elname.equals("HBoxView")) {
				html.append("<!--HBox--><table><tr>");
			} else if (elname.equals("VBoxView")) {
				html.append("<!--VBox--><table width='100%'>");
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
				// Without special position css, Textview title in View will sometimes show up twice.
			} else if (elname.equals("TextView") && thisEl.atts.containsKey("headingLevel")) {
				html.append("<TextView class=\"absolute\" ");
				if (!nextHeaderBorder.equals("")) { //bordered heading style:
					html.append(" style=\"border-width:1px; border-style:solid; border-radius: 2px; padding: 2px; border-color:");
					html.append(nextHeaderBorder);
					html.append("; left:"+thisEl.atts.get("leftInset")+"; top:"+thisEl.atts.get("topInset")+";\">");
					nextHeaderBorder = "";
				} else {
					html.append("style=\"left:"+thisEl.atts.get("leftInset")+"; top:"+thisEl.atts.get("topInset")+";\">");
				}
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
		String extra = "";
		if (_debug) {
			original.append(innerText);
		}
		StackElement thisEl = docStack.pop();
		assert(elname.equals(thisEl.name));
		
		// Elements handler. Mirror image of the one in StartElement
		if (!ignoring) {
			if (elname.equals("FontStyle")) {
				if ("default".equals(thisEl.atts.get("styleName"))) {
					//Set default text style. This is important, for example, if it's white text on black!
					html.append("<style> * {color: ");
					html.append(thisEl.atts.get("color"));
					html.append("; font-family: ");
					html.append(thisEl.atts.get("font"));
					html.append("; font-size: ");
					html.append(thisEl.atts.get("size"));
					html.append(";} </style>");
				}
			} else if (elname.equals("title")) {
				_title = innerText.toString();
			} else if (elname.equals("PictureButtonView")
			            && thisEl.atts.containsKey("mask") && thisEl.atts.get("mask").indexOf("masks/outline_box.png") >-1) {
				//Set style without ending quote " .
				nextHeaderBorder = thisEl.atts.get("color");
			} else if (lastElement.equals("key")) {
			
				if (docStack.size()==2 && docStack.peek().name.equals("dict") && lastValue.equals("title")) {
					//special case - <plist><dict><key>title</key>...
					html.append("<h1>");
					html.append(innerText);
					html.append("</h1>");
					_title = innerText.toString();
					html.append("<script>document.title=\""+_title.replace("&amp;", "&").replace("\"", "\\\"")+"\";</script>");
				} else if (!docStack.empty() && !isHandled(docStack.peek())) {
					/** Mobile mode
					 * It goes in separate subMap, so it won't overwrite, for example:
					 * <key>url</key><string/>
					 * <key>artwork-url</key><dict>
					 *  <key>url</key><string (imageurl)/> <- this should be stored in subMap.
					 */
					try {
						//When img preferred size specified, the url should be within the size range.
						if (_imgPrefSize > 0 //there is preferred size
								&& lastValue.toString().equals("url") && subMap.containsKey("url") // and this is url key that will replace other value
								&& subMap.get("box-height")!=null && Integer.valueOf(subMap.get("box-height")) > _imgPrefSize) // and last-seen boxheight isn't in pref range
						{
							//Don't show image that is larger than preferred size. Url will not be overwritten. 
						} else {
							//Add key --> value pair to SUB map.
							subMap.put(lastValue, innerText.toString());
						}
					} catch (NumberFormatException e) {
						Log.e(TAG,"Unexpected subMap box-height.");
					};
				} else {
					map.put(lastValue, innerText.toString());
				}
				if (lastValue.equals("URL")) {
					urls.add(innerText.toString());
				} else if (lastValue.equals("songName")) {
					singleName = innerText.toString();
				}
			} else if (elname.equals("dict") && isHandled(thisEl)) {
				//End of key-val definition, mobile mode:
				String type = "";
				if (thisEl.atts.get(KEY).equals("dialog")) {
					html.append(map.get("message"));
				}
				if (map.containsKey("type")) {//add type=separator
					type = map.get("type");
				}
				if (map.containsKey("kind") && (map.get("kind").equals("Goto") || map.get("kind").equals("OpenURL")) &&
				    map.containsKey("url")) {
					//This is a redirect.
					redirectPage = map.get("url");
				} else if (type.equals("review-header")) {
					appendStars(Float.valueOf(map.get("average-user-rating")));
					html.append("<br><div>");
					html.append(map.get("title"));
					html.append("</div>");
				} else if (type.equals("review")) {
					html.append("<div><br><b>");
					appendStars(Float.valueOf(map.get("average-user-rating")));
					html.append("<br>");
					html.append(map.get("title"));
					html.append("</b><br>");
					html.append(map.get("text"));
					html.append("<br>&nbsp;-&nbsp;");
					html.append(map.get("user-name"));
					html.append("</div>");
				} else if (type.equals("more")) {
					addLink(map.get("title"), map.get("url"), null);
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
					html.append("\"><img class='rounded' src=\"");
					html.append(subMap.get("url"));
					html.append("\"></a>");
				} else if (type.equals("separator")) {
					html.append("<div><font class='separator' style='padding: 2px;'>");
					if (map.containsKey("title")) {
						html.append(map.get("title"));
					} else {
						html.append("&nbsp;");
					}
					html.append("</font></div>");
				} else if (type.equals("link")) { //A link to page
					if (map.get("url") != null && (map.get("url").startsWith(IGNOREENROLL) || map.get("url").startsWith(IGNOREENROLL2))) {
						Log.e(TAG,"Enroll link not shown, since AppleID enrollment is not supported.");
					} else {
						if (map.get("average-user-rating") == null) {
							addLink(map.get("title"), map.get("url"), subMap.get("url"),map.get("artist-name"));
						} else {
							addLink(map.get("title"), map.get("url"), subMap.get("url"),
								map.get("title2"), Float.valueOf(map.get("average-user-rating")),
								map.get("artist-name"));
						}
					}
				} else if (type.equals("pagination")) {
					addLink(map.get("title"), map.get("url"), null);
				} else if (type.equals("podcast")) { // page info.
					// could add background -webkit-gradient(linear, left top, left bottom, color-stop(0%,#DACE95), color-stop(100%,#E2E2E2));?
					html.append("<div style='display:table; width:100%; margin:7px 0 20px 0;'><!-- podcast --><a href='javascript:;' url=\"");
					html.append(map.get("url"));
					html.append("\" onclick=\"window.DOWNLOADINTERFACE.go(this.getAttribute('url'))\"><img style='float:left; padding:3px;' src=\"");
					html.append(subMap.get("url"));
					html.append("\"><font size='+2'>");
					if (map.containsKey("title")) {
						//_title = map.get("title");seen multiple times for some pages.
						html.append(map.get("title"));
					}
					if (subMap.containsKey("label")) {
						html.append(" ["+subMap.get("label")+"]");
					}
					html.append("</font></a><br/>");
					html.append(map.get("description"));
					html.append("<div style=\"margin:7px 0 7px 0;\">");
					showRating();
					html.append("</div>");
					if (map.containsKey("podcast-feed-url")) {
						String rssfeedurl = map.get("podcast-feed-url").replace("\"", "&quot;");
						html.append("<a href='");
						html.append(rssfeedurl);
						html.append("' onclick=\"window.DOWNLOADINTERFACE.subscribe(this.getAttribute('url')); return false;\" url=\"");
						html.append(rssfeedurl);
						html.append("\">Subscribe</a>");
					}
					html.append("</div>");
				} else if (type.equals("podcast-episode")) {
					if (map.containsKey("url") && map.get("url").equals(_url.toString())) {
						extra = "style='background-color:gold;' ";
						html.append("<a name='here'></a>");
					}
					addMiniMedia(map.get("title"),subMap.get("asset-url"),subMap.get("asset-url"),extra);
					
				} else if (type.equals("album")) {
					html.append("<div style='display:table; margin: 8px 0px; width:100%;'>");
					if (subMap.containsKey("url")) {
						html.append("<img style='vertical-align: top; margin:2px; float:left; display:block;' src=\"");
						html.append(subMap.get("url").replace("\"","\\\""));
						html.append("\">");
					}
					html.append(map.get("genre-name"));
					html.append("<br/>");
					html.append(map.get("copyright"));
					html.append("<br/>Released ");
					html.append(map.get("release-date-string"));
					html.append("<br/>");
					html.append(subMap.get("price-display"));
					html.append("<br/>");
					showRating();
					html.append("</div>");
				} else if (type.equals("song")) {
					if (map.containsKey("url") && map.get("url").equals(_url.toString())) {
						extra = "style='background-color:gold;' ";
						html.append("<a name='here'></a>");
					}
					addMiniMedia(map.get("title"), subMap.get("preview-url"), null, "");
					_preview = true;
				} else if (map.containsKey("songName") || map.containsKey("itemName")) {
				
					addMediaRow();
				} else if (type.endsWith("-section") && map.containsKey("contents")) {
					//Funny hybrid ipadpage that has htm in xml.
					html.append(map.get("contents"));
				}
				//Done with it, don't let parent repeat:
				map.clear();
				subMap.clear();
			} else if (elname.equals("array")) {
				if (thisEl.atts.get(KEY) != null && thisEl.atts.get(KEY).equals("tabs")) {
					html.append("<!-- end tabs --></div>");
				}
			} else if (elname.equals("HBoxView")) {
				html.append("</tr></table>");
			} else if (elname.equals("VBoxView")) {
				html.append("</table>");
			} else if (elname.equals("GotoURL") || elname.equals("OpenURL")) {
				if (!innerText.toString().trim().equals("TELL A FRIEND")) {//Page wouldn't work.
					html.append(innerText);
				}
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
		
		if (_debug) {
			original.append("</");
			original.append(elname);
			original.append(">");
		}
		//Must be run every time:
		innerText.setLength(0);
	}
	
	/**
	 * If applicable, adds rating and stars of current section:
	 */
	private void showRating() {
		if (map.containsKey("title2") && map.containsKey("view-user-reviews-url")) {
			html.append("<a href='javascript:;' url=\"");
			html.append(map.get("view-user-reviews-url"));//Ratings link
			html.append("\" onclick=\"window.DOWNLOADINTERFACE.go(this.getAttribute('url'))\">");
			html.append(map.get("title2"));
			if (map.containsKey("average-user-rating")) {
				html.append("<br/>");
				appendStars(Float.valueOf(map.get("average-user-rating")));
			}
			html.append("</a><br/>");
		}
	}
	private void addMiniMedia(String title, String preview, String download, String extra) {
		html.append("<div class='media' "+extra+" onclick='toggle(this.nextSibling)'>");
		//stoppropagation to prevent clicking container, then download:
		if (download != null) {
			html.append("<a class='media' style='float:right' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.download(this.getAttribute('title'), document.title, this.getAttribute('download-url'));\" title=\"");
			html.append(title.replace("\"", "&quot;"));
			html.append("\" download-url=\"");
			html.append(subMap.get("asset-url").replace("\"", "&quot;"));
			html.append("\"><span class='download_open'>Download</span> ");
			html.append(fileExt(subMap.get("asset-url")));
			html.append("</a>");
		}
		if (preview != null) {
			html.append("<a href='javascript:;' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.preview(this.getAttribute('title'),this.getAttribute('url'));\" title=\"");
			html.append(title.replace("\"", "&quot;"));
			html.append("\" url=\"");
			html.append(preview.replace("\"", "&quot;"));
			html.append("\"><span class='preview'></span></a>&nbsp;");
		}
		html.append("<b class='media'>");
		html.append(title);
		if (subMap.containsKey("label")) {
			html.append(" ["+subMap.get("label")+"]");
		}
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
		if (map.containsKey("long-description")) {
			html.append(map.get("long-description"));
		}
		if (subMap.containsKey("price-display")) {
			html.append(subMap.get("price-display"));
		}
		html.append("<br></div>");
	}
	/**
	 * Appends rating stars to html.
	 * @param rating - 0 for 0 stars, 1.0 is 5-star.
	 */
	private void appendStars(Float rating) {
		// The actual image is in mobile_extras file, in css.
		html.append("<div class=\"ratingstars\" style=\"width:");
		// Set width, with 25x25 star, 5-stars would be 125 * 1.0 = 125px.
		html.append(String.valueOf(125*(rating)));
		html.append("px;\"></div></string>");
	}
	
	private void addLink(String text, String url, String image) {
		addLink(text, url, image, null);
	}
	private void addLink(String text, String url, String image, String author) {
		html.append("<div class='link' onclick=\"window.DOWNLOADINTERFACE.go(this.getAttribute('url'))\" url=\"");
		html.append(url.replace("\"", "&quot;"));
		html.append("\">");
		if (image != null) {
			html.append("<img style='vertical-align: top; margin:2px; float:left; display:block;' src=\"");
			html.append(image.replace("\"", "&quot;"));
			html.append("\">");
		}
		if (author != null) {
			html.append("<strong>");
			html.append(text);
			html.append("</strong><br>");
			html.append(author);
		} else {
			html.append(text);
		}
		html.append("</div>");
	}
	
	/**
	 * Appends a full-width link to the html.
	 * @param text String to display
	 * @param url String to go to
	 * @param image String url, or null for no image.
	 * @param ratings count
	 * @param rating a float representing rating (0=0, 1.0=5-star).
	 * @param author
	 */
	private void addLink(String text, String url, String image, String ratings, float rating, String author) {
		html.append("<div class='link' onclick=\"window.DOWNLOADINTERFACE.go(this.getAttribute('url'))\" url=\"");
		html.append(url.replace("\"", "&quot;"));
		html.append("\">");
		if (image != null) {
			html.append("<img style='vertical-align: top; margin:2px; margin-right:8px; float:left; display:block;' src=\"");
			html.append(image.replace("\"", "&quot;"));
			html.append("\"><strong>");
		}
		html.append(text);
		html.append("</strong><br>");
		html.append(author);
		html.append("<br>");
		appendStars(rating);
		html.append("<br>");
		html.append(ratings);
		html.append("</div>");
	}

	/**
	 * Adds a row representing the file to the media variable, based on the current map.
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
		if (id.equals(_reference)) {
			style = "dl selection";
		}
		if (!directurl.equals("") && directurl.lastIndexOf(".")>-1) { //valid media row:
			if (html.toString().equals("<plist><dict><true>")) {//Not in a page, this must be single item.
				urls.add(directurl);
				singleName = name;
				if (map.containsKey("podcastName")) {
					_title = map.get("podcastName");
				}
			} else {//normal
				if (style.equals("dl selection")) {
					media.append("<a name='here'></a>");
				}
				media.append(String.format(
				"<tr class=\"%s\" onClick=\"window.DOWNLOADINTERFACE.preview(this.getAttribute('name'),this.getAttribute('url'));\" name=\"%s\" url=\"%s\"><td><a name=\"%s\">%s</a></td><td>%s</td><td>%s</td><td>%s</td>"+
				"<td><a href='javascript:;' onclick=\"window.event.stopPropagation(); downloadit(this.parentNode.parentNode.getAttribute('name'),this.parentNode.parentNode.getAttribute('url'))\">Download %s</a></td></tr>\n",
				style,name.replace("\"", "&quot;"),directurl.replace("\"", "&quot;"),id,name,artist,timeval(duration),comments,directurl.substring(directurl.lastIndexOf("."))));
			}
		}
	}
	
	/**
	 * Returns the file extension of the url. (replaces .rtf with .zip)
	 * MUST match dest function in javascript.js!
	 * @param url string.
	 * @return lowercase string.
	 */
	public static String fileExt(String url) {
		//May have strange urls like http://media.ccomrcdn.com/media/station_content/1674/JamesBondHeinekenVersion1_1333982114_19481.mp3?CPROG=PCAST&MARKET=NEWYORK-NY&NG_FORMAT=&SITE_ID=1674&STATION_ID=WAXQ-FM&PCAST_AUTHOR=Q104.3_New_York_City&PCAST_CAT=comedy&PCAST_TITLE=Jim_Kerr_Rock_and_Roll_Morning_Show_Parodies
		if (url.indexOf("?")>-1) {
			url = url.substring(0,url.indexOf("?"));
		}
		if (url.lastIndexOf(".") == -1) {
			return null;
		} else {
			String ext = url.substring(url.lastIndexOf(".") );
			if (ext.indexOf("%")>-1) {
				ext = ext.substring(0,ext.indexOf("%"));
			}
			if (ext.indexOf("/")>-1) {
				ext = ext.substring(0,ext.indexOf("/"));
			}
			if (ext.toLowerCase().equals(".rtf")) {
				return ".zip";
			} else if (ext.toLowerCase().equals(".ibooks")) {
				return ".epub";
			} else {
				return ext.toLowerCase();
			}
		}
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
	 * @param milliseconds
	 * @return String in hh:mm format.
	 */
	public static String timeval(String ms) {
		String out = ms;
		try {
			long sec = Long.valueOf(ms)/1000;
			out = String.valueOf(sec/60)+":";
			if ((sec % 60) < 10) {
				out += "0"+String.valueOf(sec % 60);
			} else {
				out += String.valueOf(sec % 60);
			}
		} catch (NumberFormatException e) {
			Log.e(TAG,"Unable to convert time value: \""+out+"\".");
		}
		return out;
	}

	/**
	 * Handles text between tag markers.
	 */
	public void characters(char[] ch, int start, int len) {
		innerText.append(ch, start, len);
	}
	
	public void endDocument() throws SAXException {
		//if mobile:
		html.insert(0,mobileExtras);
		//Add style to keep large images from going off the screen:
		html.insert(0, "<style> img { max-width:"+(_scrWidth-5)+"px; height:auto;}</style>");
		original.append("<!-- (END DOC) -->");
		if (backColor.equals("")) {
			backColor = "#E2E2E2"; //Default background.
		}
		html.insert(0, String.format("<html><head>"+
			//<meta name=\"viewport\" content=\"width=device-width\" />
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head><body class=\"tunesviewerGenerated\" bgcolor=\"%s\">",backColor));
		if (media.length()>0) {
			html.append(PRE_MEDIA);
			html.append(media);
			html.append(POST_MEDIA);
		}
		if (_preview) {
			html.append("Previews available for promotional streaming purposes only, provided courtesy of iTunes. <a href=\"./\">Download on iTunes</a> by holding link and selecting \"Share\" to computer.");
		}
		html.append("</body></html>");
		if (_profiling) {
			Debug.stopMethodTracing();
		}
	}
	
	/**
	 * Returns true when <key>keyid</key><dict>... is specifically supported by this parser.
	 * (The dict map should be cleared when starting/ending <dict> tag in this case).
	 * This is used for mobile mode.
	 * 
	 * @param keyid
	 * @return true if these key-vals should be handled in map, and subelements in submap.
	 */
	private boolean isHandled(StackElement element) {
		String keyid = element.atts.get(KEY);
		//return keyid != null && HandledNames.containsKey(keyid);
		return keyid != null && (keyid.endsWith("section") || keyid.equals("action")
			|| keyid.equals("items") || keyid.equals("item-metadata") || keyid.equals("tabs")
			|| keyid.equals("squishes") || keyid.equals("content") || keyid.equals("dialog")
			|| keyid.equals("album-metadata"));
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

	public String getTitle() {
		return _title.replace("&amp;", "&");
	}
}
