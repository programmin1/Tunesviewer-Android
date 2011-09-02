/*
 * iTunes Javascript Class, added to the displayed pages.
 * Catches iTunes-api calls from pages, such as http://r.mzstatic.com/htmlResources/6018/dt-storefront-base.jsz
 */
function player () {
	this.playURL = function(input) {
		window.DOWNLOADINTERFACE.preview('preview',input.url);
		return 'not 0';
		var div = document.createElement('div');
		div.setAttribute('class','quick-view video movie active activity-video-dialog');
		div.setAttribute('style','width:50%; height:auto; position:fixed; left: 25%; float: top ; top:10px');
		div.setAttribute('id','previewer-container')
		a = document.createElement('a');
		a.setAttribute('class','close-preview');
		a.addEventListener('click',function() {
			this.parentNode.parentNode.removeChild(this.parentNode);
		} );
		div.appendChild(a);
		var vid = document.createElement('video');
		vid.id = 'previewPlayer';
		vid.setAttribute('controls','true')
		div.appendChild(vid)
		document.body.appendChild(div);
		document.getElementById('previewPlayer').src=input.url;
		document.getElementById('previewPlayer').play()
		return 'not 0';
	};
	this.stop = function() {
		document.getElementById('previewer-container').parentNode.removeChild(document.getElementById('previewer-container'))
		return true;
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
    