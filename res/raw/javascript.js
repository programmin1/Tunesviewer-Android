function setTitle(){if(document.title==""){if(document.getElementsByTagName("h1").length>0){window.DOWNLOADINTERFACE.setTitle(document.getElementsByTagName("h1")[0].innerHTML)}else{window.DOWNLOADINTERFACE.setTitle("TunesViewer")}}else{window.DOWNLOADINTERFACE.setTitle(document.title)}}function fixTransparent(e){"use strict";var t;console.log("TunesViewer: Entering the function <fixTransparent>.");for(t=0;t<e.length;t++){if(window.getComputedStyle(e[t]).color==="rgba(0, 0, 0, 0)"){e[t].style.color="inherit"}if(e[t].parentNode.getAttribute("class")==="title"){e[t].style.background="transparent"}}}function TunesViewerEmptyFunction(){"use strict";}function removeListeners(e){"use strict";var t;for(t=0;t<e.length;t++){e[t].onmouseover=TunesViewerEmptyFunction;e[t].onclick=TunesViewerEmptyFunction;e[t].onmousedown=TunesViewerEmptyFunction}}iTunes={getMachineID:function(){"use strict";console.log("TunesViewer: In function <getMachineID>");return""},getPreferences:function(){"use strict";return{pingEnabled:true}},installedSoftwareApplications:[],doDialogXML:function(e,t){"use strict";},playURL:function(e){"use strict";window.DOWNLOADINTERFACE.preview("preview",e.url);return"not 0"},showMediaPlayer:function(e,t,n){"use strict";console.log("showMediaPlayer "+e);this.playURL({url:e})},openURL:function(e){"use strict";console.log("openURL"+e);location.href=e;setTitle()},addProtocol:function(e){"use strict";console.log("addProtocol called:"+e);if(e.indexOf("<key>navbar</key>")===-1){e=(new DOMParser).parseFromString(e,"text/xml");var t=e.getElementsByTagName("key"),n="",r="";for(var i=0;i<t.length;i++){if(t[i].textContent=="URL"){n=t[i].nextSibling.textContent}else if(t[i].textContent=="songName"||t[i].textContent=="itemName"){r=t[i].nextSibling.textContent}}if(n!=""){window.DOWNLOADINTERFACE.download(r,document.title,n)}}},stop:function(){"use strict";document.getElementById("previewer-container").parentNode.removeChild(document.getElementById("previewer-container"));return true},doPodcastDownload:function(e,t){"use strict";window.open("download://"+encodeURIComponent(e.innerHTML))},doAnonymousDownload:function(e){"use strict";console.log("Going to item description url");location.href=e.url;setTitle()},getUserDSID:function(){"use strict";return 0},putURLOnPasteboard:function(e,t){"use strict";location.href="copyurl://"+encodeURI(e)},webkitVersion:function(){"use strict";return/AppleWebKit\/([\d.]+)/.exec(navigator.userAgent)[0]}};document.addEventListener("DOMContentLoaded",function(){"use strict";var e,t,n,r,i,s,o,u,a,f,l,c,h;setTitle();var p=document.getElementsByTagName("img");for(i=0;i<p.length;i++){if(p[i].getAttribute("src-swap")!=null){p[i].setAttribute("src",p[i].getAttribute("src-swap"))}}f=function(e){console.log("TunesViewer: click event listener: "+e);location.href=e};l=function(e){console.log("TunesViewer: opening: "+e);location.href=e};c=function(e){window.DOWNLOADINTERFACE.subscribe(e)};h=function(e,t,n){iTunes.addProtocol("<xml><key>URL</key><value><![CDATA["+e+"]]></value>"+"<key>artistName</key><value><![CDATA["+t+"]]></value>"+"<key>fileExtension</key><value>zip</value>"+"<key>songName</key><value><![CDATA["+n+"]]></value></xml>")};var a=document.getElementsByTagName("button");r=document.getElementsByTagName("div");for(var i in a){if(a[i]){if(a[i].textContent&&a[i].textContent.trim()==="Subscribe Free"){if(a[i].getAttribute("subscribe-podcast-url")!==null){a[i].addEventListener("click",function(){c(this.getAttribute("subscribe-podcast-url"))},true)}else if(a[i].getAttribute("course-feed-url")!==null){a[i].addEventListener("click",function(){c(this.getAttribute("course-feed-url"))},true)}}if(a[i].hasAttribute&&a[i].hasAttribute("disabled")){removeListeners(a[i]);a[i].addEventListener("click",function(){h(getAttribute("episode-url"),getAttribute("artist-name"),getAttribute("item-name"))},false);a[i].removeAttribute("disabled")}}}e=document.getElementsByTagName("a");for(t in e){if(e.hasOwnProperty(t)){if(e[t].target==="_blank"){e[t].target=""}else if(e[t].target){e[t].target=""}}}r=document.getElementsByTagName("div");for(var i=0;i<r.length;i++){if(r[i].getAttribute("download-url")!=null&&(r[i].textContent.indexOf("FREE")!=-1||r[i].textContent.indexOf("Download")!=-1)){removeListeners(r[i].parentNode.parentNode);removeListeners(r[i].parentNode.childNodes);removeListeners(r[i].childNodes);var d=r[i].getAttribute("download-url");r[i].innerHTML="<a class='media' onclick=\"window.event.stopPropagation();window.DOWNLOADINTERFACE.download(this.getAttribute('title'), document.title, this.getAttribute('download-url'));\" title=\""+r[i].getAttribute("item-title")+'" download-url="'+d+"\"><span class='download_open'>Download</span> "+file_ext(d)+"</a>";r[i].parentNode.parentNode.addEventListener("click",function(){console.log("preview working!");console.log(this.innerHTML);console.log(this.childNodes[5].innerHTML);window.DOWNLOADINTERFACE.preview("preview",this.childNodes[5].childNodes[1].getAttribute("download-url"))})}if(r[i].getAttribute("goto-url")!=null){r[i].addEventListener("click",function(){console.log("goto"+this.getAttribute("goto-url"));location.href=this.getAttribute("goto-url");setTitle()});document.body.style.maxWidth="100%"}if(r[i].getAttribute("role")=="button"&&r[i].getAttribute("aria-label")=="SUBSCRIBE FREE"){o="";console.log("subscribe-button");removeListeners(r[i].parentNode);removeListeners(r[i].parentNode.parentNode);for(s=0;s<r.length;s++){if(r[s].getAttribute("podcast-feed-url")!=null){o=r[s].getAttribute("podcast-feed-url")}}r[i].addEventListener("click",function(){console.log(o);window.DOWNLOADINTERFACE.subscribe(o)});iTSCircularPreviewControl=function(e){return 0}}}fixTransparent(document.getElementsByTagName("h1"));fixTransparent(document.getElementsByTagName("h2"));fixTransparent(document.getElementsByTagName("div"));fixTransparent(e);for(i=0;i<r.length;i++){if(r[i].getAttribute("download-url")!==null&&r[i].textContent.indexOf("FREE")!==-1){console.log("TunesViewer: getting attribute: "+r[i].getAttribute("download-url"));removeListeners(r[i].childNodes);var d=r[i].getAttribute("download-url");r[i].addEventListener("mouseDown",function(){l(getAttribute("download-url"))},false)}if(r[i].getAttribute("role")==="button"&&r[i].getAttribute("aria-label")==="Subscribe Free"){o="";console.log("TunesViewer: subscribe-button");removeListeners(r[i].parentNode);removeListeners(r[i].parentNode.parentNode);for(s=0;s<r.length;s++){if(r[s].getAttribute("podcast-feed-url")!==null){o=r[s].getAttribute("podcast-feed-url")}}r[i].addEventListener("click",f(o),false)}}if(document.getElementById("search-itunes-u")!==null){document.getElementById("search-itunes-u").style.height=90}if(document.getElementById("search-podcast")!==null){document.getElementById("search-podcast").style.height=90}var v=document.getElementsByClassName("absolute");for(t in v){try{v[t].style.position="absolute";v[t].parentNode.style.position="relative";v[t].parentNode.style.display="block";v[t].style.fontWeight="bold";if(v[t].parentNode.parentNode.parentNode.parentNode.parentNode.tagName.toLowerCase()=="table"){v[t].parentNode.parentNode.parentNode.parentNode.parentNode.style.width="100%"}}catch(m){}}var g=document.getElementsByTagName("hr");for(var y in g){if(g[y].parentNode&&g[y].parentNode.tagName.toLowerCase()=="select"){g[y].parentNode.removeChild(g[y])}}n=document.createElement("style");n.type="text/css";n.innerHTML="* { -webkit-user-select: initial !important } div.search-form {height: 90}";document.body.appendChild(n);console.log("TunesViewer: JS OnPageShow Ran Successfully.")},false)