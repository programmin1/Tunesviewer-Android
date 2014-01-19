/*
 * Important! Any changes to this must be saved in javascript.js, the minified version!
 * 
 * iTunes Javascript Class, added to the displayed pages.
 * Catches iTunes-api calls from pages, such as
 * http://r.mzstatic.com/htmlResources/6018/dt-storefront-base.jsz
 */

/*global window */
window.DOWNLOADINTERFACE = function(){
var sendI = function(obj) {
	window.open("interface://"+encodeURIComponent(JSON.stringify(obj)));
};
return {
	go: function(url) {
		sendI({cmd:'go',url:url});
	},
	subscribe: function(url) {
		sendI({cmd:'subscribe',url:url});
	},
	source: function(url) {
		window.location.href = "interface://"+encodeURIComponent(JSON.stringify({cmd:'source',src:url}));
	},
	download: function(title,podcast,url) {
		sendI({cmd:'download', title:title, podcast:podcast, url:url});
	},
	preview:function(title, url) {
		sendI({cmd:'preview', title:title, url:url});
	},
	setTitle: function(title) {
		sendI({cmd:'setTitle', title:title});
	}
}
}();// create the object
iTunes = { // All called from the page js:
	getMachineID: function () {
		"use strict";
		// FIXME: Apple's javscript tries to identify what machine we are
		// when we click on the "Subscribe Free" button of a given course to
		// create an URL.
		//
		// We should see what are some valid values and use those.
		console.log("TunesViewer: In function <getMachineID>");

		return "";
	},
/*for tablet-mode, interferes with normal mode sometimes:
	systemVersion: function() {
		"use strict";
		return "5.0";
	},
	
	addEventListener: function(a,b,c) {
	  console.log("addevent");
	  console.log(a);
	  console.log(b);
	  console.log(c);
	},
	
	device: {
		mainScreenScale: 1.0
	},
	
	protocol: "view",
	
	createWindow: function() {
		return function() {
			backViewController: {}
		};
	},

	createStorePage: function(url) {
		console.log("open url "+url);
	},
	createPopOver: function(url) {
		console.log("popover "+url);
	},
	

	addEventListener : document.addEventListener,

	mediaLibrary: {
		containsAdamIDs : function(el) {
			console.log(el);
			return true;
		}
	},

	//protocol: {
	//	clientIdentifier : " "
	//},

*/	

	getPreferences: function () {
		"use strict";
		return {
			pingEnabled: true
		};
	},
	
	installedSoftwareApplications: [],


	doDialogXML: function (b, d) {
		"use strict";
		/*
		// FIXME: This seems to be called for the creation of a
		// confirmation / license agreement dialog.
		//
		// Not yet sure how exactly this should work.
		var i;
		console.log("TunesViewer: In function <doDialogXML>, b:" + b);
		console.log("TunesViewer: In function <doDialogXML>, d:" + d);

		for (i in d) {
		console.log("TunesViewer: " + i + " " + d[i]);
		}

		return d.okButtonAction(); */
	},


	playURL: function (input) {
		"use strict";
		window.DOWNLOADINTERFACE.preview('preview', input.url);
		return 'not 0';
	},


	showMediaPlayer: function (mediaurl, showtype, title) {
		"use strict";
		console.log("showMediaPlayer "+mediaurl);
		this.playURL({url: mediaurl});
	},


	openURL: function (url) {
		"use strict";
		console.log("openURL" + url);
		location.href = url;
		setTitle();
	},


	/** Download a file described as XML */
	addProtocol: function (xml) {
		"use strict";
		console.log('addProtocol called:'+xml);
		if (xml.indexOf("<key>navbar</key>") === -1) {
			xml = new DOMParser().parseFromString(xml, "text/xml");
			var keys = xml.getElementsByTagName('key'), url = "", name = "";
			for (var i=0; i<keys.length; i++) {
				if (keys[i].textContent=="URL") {//Goto the download url.
					url = keys[i].nextSibling.textContent;
					//document.location = keys[i].nextSibling.textContent;
					//setTitle();
				} else if (keys[i].textContent=="songName" || keys[i].textContent=="itemName") {
					name = keys[i].nextSibling.textContent;
				}
				
			}
			if (url != '') {
				window.DOWNLOADINTERFACE.download(name, document.title, url);
			}
			/*if (xml.indexOf("<key>navbar</key>") === -1) {
				console.log("TunesViewer: adding download: " + xml);
				location.href = "download://" + xml;
			}*/
		}
	},


	/** Stops the preview player */
	stop: function () {
		"use strict";
		document.getElementById("previewer-container").parentNode.removeChild(document.getElementById("previewer-container"));
		return true;
	},


	doPodcastDownload: function (obj, number) {
		"use strict";
		window.open("download://"+encodeURIComponent(obj.innerHTML));
	},


	doAnonymousDownload: function (obj) {
		"use strict";
		console.log("Going to item description url");
		// This will have title name etc. for the media:
		location.href = obj.url;
		setTitle();
	},


	getUserDSID: function () { // no user id.
		"use strict";
		return 0;
	},


	putURLOnPasteboard: function (a, bool) {
		"use strict";
		location.href = "copyurl://" + encodeURI(a);
	},


	/** What version of webkit we're using, eg 'AppleWebKit/531.2' */
	webkitVersion: function () {
		"use strict";
		return (/AppleWebKit\/([\d.]+)/).exec(navigator.userAgent)[0];
	}

};

/*jslint unparam: false*/

function setTitle() {
	if (document.title=='') {
		if (document.getElementsByTagName('h1').length > 0) {
			window.DOWNLOADINTERFACE.setTitle(document.getElementsByTagName('h1')[0].innerHTML);
		} else {
			window.DOWNLOADINTERFACE.setTitle('TunesViewer');
		}
	} else {
		window.DOWNLOADINTERFACE.setTitle(document.title);
	}
}

function fixTransparent(objects) {
	"use strict";
	var i;
	console.log("TunesViewer: Entering the function <fixTransparent>.");
	for (i = 0; i < objects.length; i++) {
		// If the heading is transparent, show it.
		if (window.getComputedStyle(objects[i]).color === "rgba(0, 0, 0, 0)") {
			objects[i].style.color = "inherit";
		}

		// Fix odd background box on iTunesU main page
		if (objects[i].parentNode.getAttribute("class") === "title") {
			objects[i].style.background = "transparent";
		}
	}
}


/**
 * Empty function to assign to events that we want to kill.
 */
function TunesViewerEmptyFunction() {
	"use strict";
}


/**
 * Function to remove event listeners (onmouseover, onclick, onmousedown)
 * of objects that we don't want to "have life".
 */
function removeListeners(objects) {
	"use strict";
	var i;
	for (i = 0; i < objects.length; i++) {
		objects[i].onmouseover = TunesViewerEmptyFunction;
		objects[i].onclick = TunesViewerEmptyFunction;
		objects[i].onmousedown = TunesViewerEmptyFunction;
	}
}


/* Hooking everything when the document is shown.
 *
 * FIXME: This huge thing has to be broken down into smaller pieces with
 * properly named functions.
 */
document.addEventListener("DOMContentLoaded", function () {
	"use strict";
	var as, a, css, divs, i, j, rss, previews, buttons, clickEvent, downloadMouseDownEvent, subscribePodcastClickEvent, disabledButtonClickEvent;
	
	setTitle();
	
	var imgs = document.getElementsByTagName("img");
	for (i=0; i<imgs.length; i++) {
		if (/*imgs[i].getAttribute("src")==null && */imgs[i].getAttribute("src-swap") != null) {
			imgs[i].setAttribute("src",imgs[i].getAttribute("src-swap"));
		}
	}

	// FIXME: Should we change this to be a separate function "attached"
	// to an object that is, finally, assigned to the onpageshow event?
	clickEvent = function (rss) {
		console.log("TunesViewer: click event listener: " + rss);
		location.href = rss;
	};

	// FIXME: Should we change this to be a separate function "attached"
	// to an object that is, finally, assigned to the onpageshow event?
	downloadMouseDownEvent = function (downloadUrl) {
		console.log('TunesViewer: opening: ' + downloadUrl);
		location.href = downloadUrl;
	};

	// FIXME: Should we change this to be a separate function "attached"
	// to an object that is, finally, assigned to the onpageshow event?
	subscribePodcastClickEvent = function (subscribePodcastUrl) {
		window.DOWNLOADINTERFACE.subscribe(subscribePodcastUrl);
	};

	disabledButtonClickEvent = function (episodeUrl, artistName, itemName) {
		iTunes.addProtocol("<xml><key>URL</key><value><![CDATA[" + episodeUrl + "]]></value>" +
			"<key>artistName</key><value><![CDATA[" + artistName + "]]></value>" +
			"<key>fileExtension</key><value>zip</value>" +
			"<key>songName</key><value><![CDATA[" + itemName + "]]></value></xml>");
	};
	
	var buttons = document.getElementsByTagName('button');
	divs = document.getElementsByTagName("div");
	for (var i in buttons) {
		if (buttons[i]) {
			if (buttons[i].textContent && buttons[i].textContent.trim() === "Subscribe Free") {
				if (buttons[i].getAttribute('subscribe-podcast-url') !== null) {
					buttons[i].addEventListener('click',
								function(){
									subscribePodcastClickEvent(this.getAttribute('subscribe-podcast-url'))
								},
								true);
				} else if (buttons[i].getAttribute('course-feed-url') !== null) {
					buttons[i].addEventListener('click',
								function(){
									subscribePodcastClickEvent(this.getAttribute('course-feed-url'))
								},
								true);
				}
			}
			// TODO: See why hasAttribute is not defined sometimes.
			if (buttons[i].hasAttribute && buttons[i].hasAttribute("disabled")) {
				removeListeners(buttons[i]);
				buttons[i].addEventListener('click',
								function() {disabledButtonClickEvent(getAttribute("episode-url"),
											 getAttribute("artist-name"),
											 getAttribute('item-name'))
								},
								false);
				buttons[i].removeAttribute("disabled");
			}
		}
	}
	// Fix <a target="external" etc.

	// `as` is a list of anchors, `a` iterates over the list
	as = document.getElementsByTagName("a");
	for (a in as) {
		if (as.hasOwnProperty(a)) {
			if (as[a].target === "_blank") {
				as[a].target = "";
				//as[a].href = "web" + as[a].href;
			} else if (as[a].target) {
				as[a].target = "";
			}
		}
	}

	divs = document.getElementsByTagName("div");
	for (var i=0; i<divs.length; i++) {
		if (divs[i].getAttribute("download-url") != null && (divs[i].textContent.indexOf("FREE")!=-1 || divs[i].textContent.indexOf("Download")!=-1)) {
			//console.log("Free download div, "+divs[i].getAttribute("download-url"));
			removeListeners(divs[i].parentNode.parentNode);
			removeListeners(divs[i].parentNode.childNodes);
			removeListeners(divs[i].childNodes);
			var url = divs[i].getAttribute("download-url");
			divs[i].innerHTML = "<a class='media' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.download(this.getAttribute('title'), document.title, this.getAttribute('download-url'));\" title=\""
				+divs[i].getAttribute("item-title")+"\" download-url=\""+url+"\"><span class='download_open'>Download</span> "+file_ext(url)+"</a>";
			//divs[i].addEventListener('mouseDown',function () {console.log('opening'+this.getAttribute('download-url'));
			//                                              location.href = this.getAttribute('download-url'); });
			//Unfortunately it seems some previews aren't working with this:
			divs[i].parentNode.parentNode.addEventListener('click',function () {
				console.log("preview working!");
				//
				// Enables previewing in courses by selecting the number to the left of the item.
				// This could really be written better:
				//
				console.log(this.innerHTML);
				console.log(this.childNodes[5].innerHTML);
				window.DOWNLOADINTERFACE.preview("preview",this.childNodes[5].childNodes[1].getAttribute('download-url'));
			});
		}
		if (divs[i].getAttribute('goto-url')!=null) {
			divs[i].addEventListener('click',function () {console.log('goto'+this.getAttribute('goto-url')); location.href=this.getAttribute('goto-url'); setTitle();});
			//fix width in landscape orientation: (broken?)
			document.body.style.maxWidth="100%";
		}
		if (divs[i].getAttribute("role")=="button" && divs[i].getAttribute("aria-label")=="SUBSCRIBE FREE") {
			rss = "";
			console.log("subscribe-button");
			removeListeners(divs[i].parentNode);
			removeListeners(divs[i].parentNode.parentNode);
			for (j=0; j<divs.length; j++) {
				if (divs[j].getAttribute("podcast-feed-url") != null) {
					rss = divs[j].getAttribute("podcast-feed-url");
					//console.log("rss:"+rss); too many
				}
			}
			divs[i].addEventListener('click', function () {console.log(rss);window.DOWNLOADINTERFACE.subscribe(rss);});
			iTSCircularPreviewControl = function(a) {return 0}
		}
	}
	
	/* This fixes the color=transparent style on some headings.
	 * Unfortunately, you can't use document.styleSheets' CSSRules/rules
	 * property, since it's cross-domain:
	 *
	 * http://stackoverflow.com/questions/5678040/cssrules-rules-are-null-in-chrome
	 *
	 * So, it manually checks for elements with the style:
	 */
	fixTransparent(document.getElementsByTagName("h1"));
	fixTransparent(document.getElementsByTagName("h2"));
	fixTransparent(document.getElementsByTagName("div"));
	fixTransparent(as);

	// fix free-download links, mobile
	for (i = 0; i < divs.length; i++) {
		if (divs[i].getAttribute("download-url") !== null &&
		    divs[i].textContent.indexOf("FREE") !== -1) {
			console.log("TunesViewer: getting attribute: " + divs[i].getAttribute("download-url"));
			removeListeners(divs[i].childNodes);
			var url = divs[i].getAttribute("download-url");
			//divs[i].innerHTML = "<button class='download_open' onclick='window.event.stopPropagation();location.href=\"" + url + "\";'>Download "+file_ext(url)+"</button>";
			divs[i].addEventListener('mouseDown',function() {downloadMouseDownEvent(getAttribute('download-url'))}, false);
		}
		if (divs[i].getAttribute("role") === "button" &&
			divs[i].getAttribute("aria-label") === "Subscribe Free") {
			rss = "";
			console.log("TunesViewer: subscribe-button");
			removeListeners(divs[i].parentNode);
			removeListeners(divs[i].parentNode.parentNode);
			for (j = 0; j < divs.length; j++) {
				if (divs[j].getAttribute("podcast-feed-url") !== null) {
					rss = divs[j].getAttribute("podcast-feed-url");
					//console.log("TunesViewer: RSS:" + rss); too many
				}
			}
			divs[i].addEventListener('click', clickEvent(rss), false);
		}
	}



	//Fix 100% height
	if (document.getElementById('search-itunes-u') !== null) {
		document.getElementById('search-itunes-u').style.height = 90;
	}
	if (document.getElementById('search-podcast') !== null) {
		document.getElementById('search-podcast').style.height = 90;
	}
	
	// For some mobile html
	var abs = document.getElementsByClassName('absolute');
	for (a in abs) {
		try {//Styling for titles.
			abs[a].style.position='absolute';
			abs[a].parentNode.style.position='relative';
			abs[a].parentNode.style.display='block';
			abs[a].style.fontWeight = 'bold';
			if (abs[a].parentNode.parentNode.parentNode.parentNode.parentNode.tagName.toLowerCase()=='table') {
				abs[a].parentNode.parentNode.parentNode.parentNode.parentNode.style.width='100%';
			}
			//abs[a].innerHTML = abs[a].innerHTML.trim().replace(/\ /g,'&nbsp;');
		} catch (e) {}	
	}
	
	
	//Fix hr tag in select tag, it will crash the program, for example, when tapping dropdown by the download item.
	// see https://code.google.com/p/android/issues/detail?id=17622
	var hrs = document.getElementsByTagName('hr');
	for (var hr in hrs) {
	 if (hrs[hr].parentNode && hrs[hr].parentNode.tagName.toLowerCase()=='select') {
	   hrs[hr].parentNode.removeChild(hrs[hr]);
	 }
	}

	// Fix selectable text, and search form height
	css = document.createElement("style");
	css.type = "text/css";
	css.innerHTML = "* { -webkit-user-select: initial !important } div.search-form {height: 90}";
	document.body.appendChild(css);
	console.log("TunesViewer: JS OnPageShow Ran Successfully.");

}, false); // end Pageshow.

    