/*
 * iTunes Javascript Class, added to the displayed pages.
 * Catches iTunes-api calls from pages, such as
 * http://r.mzstatic.com/htmlResources/6018/dt-storefront-base.jsz
 */

/*global window */

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

/*	systemVersion: function() {
		"use strict";
		return "5.0";
	},
	mediaLibrary: {
		containsAdamIDs : function(el) {
			console.log(el);
			return true;
		}
	},
*/	

	getPreferences: function() {
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
		window.DOWNLOADINTERFACE.preview('preview',input.url);
		return 'not 0';
	},


	showMediaPlayer: function (url_, showtype, title) {
		"use strict";
		console.log(url_);
		playURL({url: url_});
	},


	openURL: function (url) {
		"use strict";
		console.log("openURL"+url);
		location.href = url;
		setTitle();
	},


	/** Download a file described as XML */
	addProtocol: function (xml) {
		"use strict";
		//console.log(xml);
		xml = new DOMParser().parseFromString(xml, "text/xml");
		var keys = xml.getElementsByTagName('key');
		url = "";
		name = "";
		for (var i=0; i<keys.length; i++) {
			if (keys[i].textContent=="URL") {//Goto the download url.
				url = keys[i].nextSibling.textContent;
				//document.location = keys[i].nextSibling.textContent;
				//setTitle();
			} else if (keys[i].textContent=="songName" || keys[i].textContent=="itemName") {
				name = keys[i].nextSibling.textContent;
			}
			
		}
		window.DOWNLOADINTERFACE.download(name, document.title, url);
		/*if (xml.indexOf("<key>navbar</key>") === -1) {
			console.log("TunesViewer: adding download: " + xml);
			location.href = "download://" + xml;
		}*/
	},


	/** Stops the preview player */
	stop: function () {
		"use strict";
		document.getElementById("previewer-container").parentNode.removeChild(document.getElementById("previewer-container"));
		return true;
	},


	doPodcastDownload: function (obj, number) {
		"use strict";
		alert("podcastdownload");
		console.log(obj.getAttribute('description'));
		console.log(obj.getAttribute('episode-url'));
		window.DOWNLOADINTERFACE.download(obj.getAttribute('description'), document.title ,obj.getAttribute('episode-url'));
		//var keys = obj.getElementsByTagName('key');
	},


	doAnonymousDownload: function (obj) {
		"use strict";
		//location.href = obj.url;
		doPodcastDownload(obj,0);
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
	console.log("TunesViewer: Entering the function <removeListeners>.");
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
document.onpageshow = (function () {
	"use strict";
	var as, a, css, divs, i, j, rss, previews, buttons, clickEvent, downloadMouseDownEvent, subscribePodcastClickEvent, disabledButtonClickEvent;
	console.log("ONPAGESHOW");
	setTitle();
	// Fix <a target="external" etc.

	// `as` is a list of anchors, `a` iterates over the list
	as = document.getElementsByTagName("a");
	for (a in as) {
		if (as.hasOwnProperty(a)) {
			if (as[a].target === "_blank") {
				as[a].target = "";
				as[a].href = "web" + as[a].href;
			} else if (as[a].target) {
				as[a].target = "";
			}
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

	divs = document.getElementsByTagName("div");

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

	// fix free-download links, mobile
	for (i = 0; i < divs.length; i++) {
		console.log("divs "+i+" "+divs[i]);
		if (divs[i].getAttribute("download-url") !== null &&
		    divs[i].textContent.indexOf("FREE") !== -1) {
			console.log("TunesViewer: getting attribute: " + divs[i].getAttribute("download-url"));
			removeListeners(divs[i].childNodes);
			//divs[i].innerHTML = "<button onclick='window.event.stopPropagation();location.href=\"" + divs[i].getAttribute("download-url") + "\";'>Download</button>";
			divs[i].addEventListener('mouseDown',function() {downloadMouseDownEvent(divs[i].getAttribute('download-url'))}, false);
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
					console.log("TunesViewer: RSS:" + rss);
				}
			}
			divs[i].addEventListener('click', clickEvent(rss), false);
		}
	}

	buttons = document.getElementsByTagName('button');

	// FIXME: Should we change this to be a separate function "attached"
	// to an object that is, finally, assigned to the onpageshow event?
	subscribePodcastClickEvent = function (subscribePodcastUrl) {
		location.href = subscribePodcastUrl;
	};

	disabledButtonClickEvent = function (episodeUrl, artistName, itemName) {
		iTunes.addProtocol("<xml><key>URL</key><value><![CDATA[" + episodeUrl + "]]></value>" +
			"<key>artistName</key><value><![CDATA[" + artistName + "]]></value>" +
			"<key>fileExtension</key><value>zip</value>" +
			"<key>songName</key><value><![CDATA[" + itemName + "]]></value></xml>");
	};

	for (i = 0; i < buttons.length; i++) {
		if (buttons[i].innerHTML === "Subscribe Free" &&
		    buttons[i].getAttribute('subscribe-podcast-url') !== null) {
			buttons[i].addEventListener('click',
						    subscribePodcastClickEvent(buttons[i].getAttribute('subscribe-podcast-url')),
						    true);
		}
		if (buttons[i].hasAttribute("disabled")) {
			removeListeners(buttons[i]);
			buttons[i].addEventListener('click',
						    disabledButtonClickEvent(buttons[i].getAttribute("episode-url"),
									     buttons[i].getAttribute("artist-name"),
									     buttons[i].getAttribute('item-name')),
						    false);
			buttons[i].removeAttribute("disabled");
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
	
	divs = document.getElementsByTagName("div");
	for (var i=0; i<divs.length; i++) {
		if (divs[i].getAttribute("download-url") != null && divs[i].textContent.indexOf("FREE")!=-1) {
			console.log("Free download div, "+divs[i].getAttribute("download-url"));
			removeListeners(divs[i].parentNode.parentNode);
			removeListeners(divs[i].parentNode.childNodes);
			removeListeners(divs[i].childNodes);
			divs[i].innerHTML = "<a class='media' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.download(this.getAttribute('title'), document.title, this.getAttribute('url'));\" title=\""
				+divs[i].getAttribute("item-title")+"\" url=\""+divs[i].getAttribute("download-url")+"\";'>Download</a>";
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
			divs[i].addEventListener('click',function () {console.log('goto'+this.getAttribute('goto-url')); location.href=this.getAttribute('goto-url')});
			//fix width in landscape orientation: (broken?)
			//document.body.style.maxWidth="100%";
		}
		if (divs[i].getAttribute("role")=="button" && divs[i].getAttribute("aria-label")=="SUBSCRIBE FREE") {
			rss = "";
			console.log("subscribe-button");
			removeListeners(divs[i].parentNode);
			removeListeners(divs[i].parentNode.parentNode);
			for (var j=0; j<divs.length; j++) {
				if (divs[j].getAttribute("podcast-feed-url") != null) {
					rss = divs[j].getAttribute("podcast-feed-url");
					console.log("rss:"+rss);
				}
			}
			divs[i].addEventListener('click', function () {console.log(rss);window.DOWNLOADINTERFACE.subscribe(rss);});
			iTSCircularPreviewControl = function(a) {return 0}
		}
	}
	imgs = document.getElementsByTagName("img");
	for (var i=0; i<imgs.length; i++) {
		if (/*imgs[i].getAttribute("src")==null && */imgs[i].getAttribute("src-swap") != null) {
			imgs[i].setAttribute("src",imgs[i].getAttribute("src-swap"));
		}
	}
	
	
	//Fix hr tag in select tag, it will crash the program, for example, when tapping dropdown by the download item.
	// see https://code.google.com/p/android/issues/detail?id=17622
	/*hrs = document.getElementsByTagName('hr');
	for (hr in hrs) {
	 if (hrs[hr].parentNode.tagName.toLowerCase()=='select') {
	   hrs[hr].parentNode.removeChild(hrs[hr]);
	 }
	}*/

	// Fix selectable text, and search form height
	css = document.createElement("style");
	css.type = "text/css";
	css.innerHTML = "* { -webkit-user-select: initial !important } div.search-form {height: 90}";
	//document.body.appendChild(css);
	console.log("TunesViewer: JS OnPageShow Ran Successfully.");

}()); // end Pageshow.

    