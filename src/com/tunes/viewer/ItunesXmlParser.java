package com.tunes.viewer;
//http://itunes.apple.com/WebObjects/MZStore.woa/wa/viewGrouping?id=27753

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ItunesXmlParser extends DefaultHandler {

	// HTML before and after the media list:
	private static final String PRE_MEDIA = "<script>function downloadit(title,name) { console.log('download-it'); console.log(title); console.log(name); window.DOWNLOADINTERFACE.download(title,name); }</script>\n"+
	"<style>* {font-family: Helvetica, Arial;}\n"+
	"tr.dl > td {background-image: -webkit-gradient(linear, left bottom, left top, color-stop(0.48, rgb(215,239,245)), color-stop(1, white));}\n"+
	"tr.selection {background:gold;}\n</style>"+
	"<table width='100%' border='1' bgcolor='white' cellspacing=\"1\" cellpadding=\"3\"><tr bgcolor='CCCCCC'><td><b>Name</b></td><td><b>Author</b></td><td><b>Duration</b></td><td><b>Comment</b></td><td><b>Download</b></td></tr>\n";
	private static final String POST_MEDIA = "</table>";
	
	// Holds text inside or between the elements,
	// this is printed out and reset at start-tag and end-tag.
	private StringBuilder innerText;
	
	// All of the html/xml original:
	private StringBuilder original;
	
	// Converted html for use in the browser:
	private StringBuilder html;
	
	// These handle the key-value pairs in <dict> tag,
	// such as <key>keyname</key><valuetag>value</value>
	private HashMap<String,String> map;
	private String lastElement;
	private String lastValue;
	
	private String redirectPage;
	private String backColor;
	private StringBuilder media;
	
	private ArrayList<String> urls;
	private String singleName;
	
	// The specific item id.
	private String reference;
	
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
		//workaround for: https://code.google.com/p/android/issues/detail?id=4401
		return html.toString().replace("%", "&#37;");
	}
	public String getRedirect() {
		return redirectPage;
	}
	public ArrayList<String> getUrls() {
		return urls;
	}
	public String getSingleName() {
		return singleName;
	}
	
	public ItunesXmlParser(String reference) {
		this.reference = reference;
	}
	
	@Override
	public void startDocument() throws SAXException {
		original = new StringBuilder();
		html = new StringBuilder();
		innerText = new StringBuilder();
		media = new StringBuilder();
		docStack = new Stack<StackElement>();
		
		redirectPage = "";
		lastElement = "";
		lastValue = "";
		backColor = "";
		map = new HashMap<String, String>();
		urls = new ArrayList<String>();
	}

	@Override
	public void startElement(String namespaceURI, String elname,
	                         String qName, Attributes atts) throws SAXException {
		original.append(innerText);
		original.append("<");
		original.append(elname);
		original.append(">");
		StackElement thisEl = new StackElement(elname,atts);
		
		// Elements handler. This is mirror image of the one in EndElement, do not modify without changing both!
		if (!ignoring && !elname.equals("FontStyle")) {
			html.append(innerText); //between elements text
			if (elname.equals("Test")) {
				ignoring = shouldIgnore(thisEl.atts);
			}
			
			if (atts.getValue("backColor") != null && backColor=="") {
				backColor = atts.getValue("backColor");
			}
			
			if (!docStack.empty()) {
				String parentName = docStack.peek().name;
				if (parentName.equals("HBoxView")) {
					html.append("<td>");//new col;
				} else if (parentName.equals("VBoxView")) {
					html.append("<tr><td>");
				}
			}
			
			if (elname.equals("dict")/* && last was key unimportant. */) {
				// New key-val map:
				map.clear();
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
		original.append(innerText);
		StackElement thisEl = docStack.pop();
		assert(elname.equals(thisEl.name));
		
		// Elements handler. This is mirror image of the one in StartElement, do not modify without changing both!
		if (!ignoring && !elname.equals("FontStyle")) {
			if (lastElement.equals("key")) {
				map.put(lastValue, innerText.toString());
				if (lastValue.equals("URL")) {
					urls.add(innerText.toString());
				} else if (lastValue.equals("songName")) {
					singleName = innerText.toString();
				}
			} else if (elname.equals("dict")) {
				//End of key-val definition.
				if (map.containsKey("kind") && (map.get("kind").equals("Goto") || map.get("kind").equals("OpenURL")) &&
				    map.containsKey("url")) {
					//This is a redirect.
				    redirectPage = map.get("url");
				} else if (map.containsKey("songName") || map.containsKey("itemName")) {
					addMediaRow();
				}
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
		original.append("</");
		original.append(elname);
		original.append(">");
		innerText.setLength(0);
	}

	private void addMediaRow() {
		String name = "";
		String artist = "";
		String duration = "";
		String comments = "";
		String rtype = "";
		String url = "";
		String directurl = "";
		String releaseDate = "";
		String modifiedDate = "";
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
	 * Given attributes of <Test> tag, this returns true if it needs to be ignored old-version code.
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
	
	private String timeval(String ms) {
		String out = ms;
		try {
			int sec = Integer.valueOf(ms)/1000;
			out = String.valueOf(sec/60)+":"+String.valueOf(sec % 60);
		} catch (NumberFormatException e) {
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
		original.append("<!-- (END DOC) -->");
		html.insert(0, String.format("<html><body bgcolor=\"%s\">",backColor));
		if (media.length()>0) {
			html.append(PRE_MEDIA);
			html.append(media);
			html.append(POST_MEDIA);
		}
		html.append("</body></html>");
	}
	
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
			return name;
		}
	}
}
