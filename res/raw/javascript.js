/*
 * iTunes Javascript Class, added to the displayed pages.
 * Catches iTunes-api calls from pages, such as http://r.mzstatic.com/htmlResources/6018/dt-storefront-base.jsz
 */
function player () {
	this.playURL = function(input) {
		window.DOWNLOADINTERFACE.preview('preview',input.url);
		return 'not 0';
	};
	
	this.stop = function() {
		document.getElementById('previewer-container').parentNode.removeChild(document.getElementById('previewer-container'))
		return true;
	};
	
	this.openURL = function(url) {
		location.href = url;
		setTitle();
	};
	
	this.showMediaPlayer = function(url,showtype,title) {
		obj = function () {};
		obj.url = url;
		this.playURL(obj);
	};
	
	this.addProtocol = function (xml) {
		console.log(xml);
		xml = new DOMParser().parseFromString(xml, "text/xml");
		keys = xml.getElementsByTagName('key');
		for (var i=0; i<keys.length; i++) {
			if (keys[i].textContent=="URL") {//Goto the download url.
				document.location = keys[i].nextSibling.textContent;
				setTitle();
			}
		}
	};
	
	this.doPodcastDownload = function(obj, number) {
		alert('podcastdownload');
		console.log(obj.getAttribute('description'));
		console.log(obj.getAttribute('episode-url'));
		window.DOWNLOADINTERFACE.download(obj.getAttribute('description'), document.title ,obj.getAttribute('episode-url'));
		alert(obj.getAttribute('episode-url'))
	};
	/*This may mess up normal operation by opening two identical files with different urls, duplicate notification, also
	webview crashing.
	this.doAnonymousDownload = function(obj) {
		window.DOWNLOADINTERFACE.download(obj.itemName, document.title, obj.url);
		}*/
}

function defined(something) {
	return true;
}

function removeListeners(objects) {
	for (var i=0; i<objects.length; i++) {
		objects[i].onmouseover = (function () {});
		objects[i].onclick = (function () {});
		objects[i].onmousedown = (function () {});
	}
}

function iTSVideoPreviewWithObject (obj) {
	alert(obj);
}

function fixTransparent(objects) {
	for (i=0; i<objects.length; i++) {
		// If the heading is transparent, show it.
		if (window.getComputedStyle(objects[i]).color=='rgba(0, 0, 0, 0)') {
			objects[i].style.color = 'inherit';
		}
		
		//Fix odd background box on iTunesU main page
		if (objects[i].parentNode.getAttribute('class')=='title') {
			objects[i].style.background='transparent'
		}
	}
}

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

document.onload= new function() {
	console.log(document.title);
	setTitle();
	iTunes = new player();
	
	abs = document.getElementsByClassName('absolute');
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
			console.log(divs[i].getAttribute("download-url"));

			removeListeners(divs[i].parentNode.parentNode);
			removeListeners(divs[i].parentNode.childNodes);
			removeListeners(divs[i].childNodes);
			divs[i].innerHTML = "<a class='media' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.download(this.getAttribute('title'), document.title, this.getAttribute('url'));\" title=\""
				+divs[i].getAttribute("item-title")+"\" url=\""+divs[i].getAttribute("download-url")+"\";'>Download</a>";
			divs[i].addEventListener('mouseDown',function () {console.log('opening'+this.getAttribute('download-url'));
			                                              location.href = this.getAttribute('download-url'); });
			//Unfortunately it seems some previews aren't working with this:
			divs[i].parentNode.parentNode.addEventListener('mouseDown',function () {
				console.log("preview working!");
				window.DOWNLOADINTERFACE.preview("preview",this.childNodes[2].childNodes[0].getAttribute('download-url'));
			});
		}
		if (divs[i].getAttribute('goto-url')!=null) {
			divs[i].addEventListener('click',function () {console.log('goto'+this.getAttribute('goto-url')); location.href=this.getAttribute('goto-url')});
			//fix width in landscape orientation:
			document.body.style.maxWidth="100%";
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
		if (imgs[i].getAttribute("src")==null && imgs[i].getAttribute("src-swap") != null) {
			imgs[i].setAttribute("src",imgs[i].getAttribute("src-swap"));
		}
	}
	
	
	as = document.getElementsByTagName('a');
	for (a in as) {as[a].target=''}
	fixTransparent(document.getElementsByTagName('h1'));
	fixTransparent(document.getElementsByTagName('h2'));
	fixTransparent(as);
	
	//Fix hr tag in select tag, it will crash the program, for example, when tapping dropdown by the download item.
	// see https://code.google.com/p/android/issues/detail?id=17622
	/*hrs = document.getElementsByTagName('hr');
	for (hr in hrs) {
	 if (hrs[hr].parentNode.tagName.toLowerCase()=='select') {
	   hrs[hr].parentNode.removeChild(hrs[hr]);
	 }
	}*/
	
buttons = document.getElementsByTagName('button');
for (i=0; i<buttons.length; i++) {
	if (buttons[i].getAttribute('subscribe-podcast-url')!=null) {
		buttons[i].addEventListener('click',function ()
		{location.href=this.getAttribute('subscribe-podcast-url');},true);
	} else if (buttons[i].getAttribute('anonymous-download-url')) {
		buttons[i].addEventListener('click',function ()
		{location.href=this.getAttribute('anonymous-download-url');},true);
	} else if (buttons[i].getAttribute('episode-url')!=null) {
		buttons[i].addEventListener('click',function ()
		{ window.DOWNLOADINTERFACE.download(this.getAttribute('title'), document.title, this.getAttribute('episode-url')); },true);
	}
}
if (document.location.hash=='') {
document.location = '#here';
}
}
    