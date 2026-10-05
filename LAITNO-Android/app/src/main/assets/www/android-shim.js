/* LAITNO Android · native bridge shim
   - پوشه‌های عکس/تلفظ همیشه در حافظهٔ گوشی: /LAITNO/Images و /LAITNO/Audio (بدون پرسیدن دوباره)
   - File System Access API روی حافظهٔ واقعی گوشی
   - تلفظ (TTS)، تشخیص گفتار، اعلان، دانلود/پشتیبان، اشتراک، روشن ماندن صفحه */
(function(){
"use strict";
var N=window.LTN;if(!N)return; /* فقط داخل برنامهٔ اندروید */
var L=window.__ltn={};
var J=function(s){try{return JSON.parse(s)}catch(e){return null}};
var DE=function(name,msg){try{return new DOMException(msg||name,name)}catch(e){var x=new Error(msg||name);x.name=name;return x}};
var join=function(a,b){return a?a+'/'+b:b};
var badName=function(n){return !n||n==='.'||n==='..'||/[\/\\]/.test(n)};
var clean=function(n){return String(n||'file').replace(/[\\\/:*?"<>|\x00-\x1f]+/g,' ').trim()||'file'};
var b64url=function(p){var b=unescape(encodeURIComponent(p));return btoa(b).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'')};
var fsURL=function(p){return location.origin+'/__ltfs/'+b64url(p)};
var MIME={mp3:'audio/mpeg',m4a:'audio/mp4',aac:'audio/mp4',ogg:'audio/ogg',oga:'audio/ogg',opus:'audio/ogg',wav:'audio/wav',webm:'audio/webm',jpg:'image/jpeg',jpeg:'image/jpeg',png:'image/png',webp:'image/webp',gif:'image/gif',avif:'image/avif',svg:'image/svg+xml',json:'application/json',zip:'application/zip',ics:'text/calendar',txt:'text/plain',csv:'text/csv',tsv:'text/tab-separated-values'};
var mimeOf=function(n){var m=/\.([a-z0-9]+)$/i.exec(n||'');return m&&MIME[m[1].toLowerCase()]||''};
L.root=function(){return N.rootPath()};

/* ---------- دسترسی حافظه (یک بار، اولین اجرا) ---------- */
var waiters=[];
L.onAccess=function(st){var w=waiters;waiters=[];w.forEach(function(f){try{f(st)}catch(e){}})};
function accessReady(){return new Promise(function(res){var st=N.accessState();if(st!=='pending')return res(st);waiters.push(res)})}
L.requestAccess=function(){return new Promise(function(res){waiters.push(res);N.requestAccess()})};

/* ---------- File System Access API روی پوشهٔ LAITNO ---------- */
function stat(p){return J(N.stat(p))}
function H(kind,path,name){this.kind=kind;this.name=name;this.__ltfs=path}
H.prototype.isSameEntry=function(o){return Promise.resolve(!!o&&o.__ltfs===this.__ltfs&&o.kind===this.kind)};
H.prototype.queryPermission=function(){return accessReady().then(function(){return 'granted'})};
H.prototype.requestPermission=function(){return accessReady().then(function(){return 'granted'})};

function Dir(path,name){H.call(this,'directory',path,name)}
Dir.prototype=Object.create(H.prototype);Dir.prototype.constructor=Dir;
function Fil(path,name,st){H.call(this,'file',path,name);if(st)Object.defineProperty(this,'_size',{value:st.s,writable:true,enumerable:false,configurable:true})}
Fil.prototype=Object.create(H.prototype);Fil.prototype.constructor=Fil;
try{Object.defineProperty(Dir.prototype,Symbol.toStringTag,{value:'FileSystemDirectoryHandle'});Object.defineProperty(Fil.prototype,Symbol.toStringTag,{value:'FileSystemFileHandle'})}catch(e){}
window.FileSystemHandle=window.FileSystemHandle||H;
window.FileSystemDirectoryHandle=window.FileSystemDirectoryHandle||Dir;
window.FileSystemFileHandle=window.FileSystemFileHandle||Fil;

Dir.prototype.getDirectoryHandle=async function(n,o){if(badName(n))throw DE('TypeError','bad name');var p=join(this.__ltfs,n),s=stat(p);
  if(s){if(!s.d)throw DE('TypeMismatchError')}else{if(!(o&&o.create))throw DE('NotFoundError');if(!N.mkdir(p))throw DE('NotAllowedError')}
  return new Dir(p,n)};
Dir.prototype.getFileHandle=async function(n,o){if(badName(n))throw DE('TypeError','bad name');var p=join(this.__ltfs,n),s=stat(p);
  if(s){if(s.d)throw DE('TypeMismatchError')}else{if(!(o&&o.create))throw DE('NotFoundError');if(!N.writeChunk(p,'',false))throw DE('NotAllowedError');s={s:0}}
  return new Fil(p,n,s)};
Dir.prototype.removeEntry=async function(n,o){var p=join(this.__ltfs,n);if(!stat(p))throw DE('NotFoundError');if(!N.remove(p,!!(o&&o.recursive)))throw DE('InvalidModificationError')};
Dir.prototype.resolve=async function(h){if(!h||!h.__ltfs)return null;var a=this.__ltfs,b=h.__ltfs;if(b===a)return[];if(b.indexOf(a+'/')!==0)return null;return b.slice(a.length+1).split('/')};
Dir.prototype.entries=async function*(){var p=this.__ltfs,l=J(N.list(p));if(!l)throw DE('NotFoundError');
  for(var i=0;i<l.length;i++){var e=l[i];yield [e.n,e.d?new Dir(join(p,e.n),e.n):new Fil(join(p,e.n),e.n,e)]}};
Dir.prototype.keys=async function*(){for await(var x of this.entries())yield x[0]};
Dir.prototype.values=async function*(){for await(var x of this.entries())yield x[1]};
Dir.prototype[Symbol.asyncIterator]=function(){return this.entries()};

Fil.prototype.getFile=async function(){var s=stat(this.__ltfs);if(!s||s.d)throw DE('NotFoundError');
  var r=await fetch(fsURL(this.__ltfs)+'?v='+s.m+'_'+s.s,{cache:'no-store'});if(!r.ok)throw DE('NotReadableError');
  var b=await r.blob();return new File([b],this.name,{type:mimeOf(this.name)||b.type,lastModified:s.m})};
Fil.prototype.createWritable=async function(o){var self=this,parts=[],closed=false;
  if(o&&o.keepExistingData){try{parts.push(await this.getFile())}catch(e){}}
  function blob(){return new Blob(parts)}
  var w={
    write:async function(d){if(closed)throw DE('InvalidStateError');
      if(d&&typeof d==='object'&&!(d instanceof Blob)&&!(d instanceof ArrayBuffer)&&!ArrayBuffer.isView(d)&&d.type){
        if(d.type==='truncate'){parts=[blob().slice(0,d.size)];return}
        if(d.type==='seek')return;
        d=d.data}
      if(d==null)return;parts.push(d)},
    truncate:async function(n){parts=[blob().slice(0,n)]},
    seek:async function(){},
    close:async function(){if(closed)return;closed=true;await writeBlob(self.__ltfs,blob());try{self._size=stat(self.__ltfs).s}catch(e){}},
    abort:async function(){closed=true;parts=[]},
    getWriter:function(){return{write:w.write,close:w.close,abort:w.abort,releaseLock:function(){},ready:Promise.resolve(),closed:Promise.resolve()}},
    locked:false};
  return w};

function readB64(b){return new Promise(function(res,rej){var fr=new FileReader();fr.onload=function(){var s=String(fr.result);res(s.slice(s.indexOf(',')+1))};fr.onerror=function(){rej(fr.error)};fr.readAsDataURL(b)})}
async function writeBlob(path,b){var tmp=path+'.lttmp',CH=786432; /* 768KB */
  if(!b.size){if(!N.writeChunk(tmp,'',false))throw DE('NotAllowedError')}
  for(var off=0,first=true;off<b.size;off+=CH,first=false){var s=await readB64(b.slice(off,off+CH));if(!N.writeChunk(tmp,s,!first)){N.remove(tmp,false);throw DE('NotAllowedError','write failed')}}
  if(!N.rename(tmp,path)){N.remove(tmp,false);throw DE('NotAllowedError','rename failed')}}
L.writeBlob=writeBlob;

/* پوشه‌های ثابت داخل /LAITNO */
var DIRS={'laitno-images':'Images','laitno-audio':'Audio','laitno-zip':'Files'};
window.showDirectoryPicker=async function(o){await accessReady();var sub=DIRS[o&&o.id]||'Files';N.mkdir(sub);return new Dir(sub,'LAITNO/'+sub)};
window.showSaveFilePicker=async function(o){await accessReady();var n=clean(o&&o.suggestedName||'file'),sub=/\.json$/i.test(n)?'Backups':'Exports';N.mkdir(sub);
  var p=sub+'/'+n,s=stat(p);if(!s)N.writeChunk(p,'',false);return new Fil(p,n,s||{s:0})};
/* showOpenFilePicker عمداً تعریف نشده تا برنامه از انتخابگر فایل اندروید استفاده کند */

/* handleها در IndexedDB به شکل شیء ساده ذخیره می‌شوند؛ هنگام خواندن دوباره زنده می‌شوند */
function revive(v){if(!v||typeof v!=='object')return v;
  if(typeof v.__ltfs==='string'&&!(v instanceof H))return v.kind==='directory'?new Dir(v.__ltfs,v.name):new Fil(v.__ltfs,v.name);
  if(Array.isArray(v)){var ch=false,o=v.map(function(x){var y=revive(x);if(y!==x)ch=true;return y});return ch?o:v}
  return v}
function patchGetter(proto,prop){try{var d=Object.getOwnPropertyDescriptor(proto,prop);if(!d||!d.get)return;
  Object.defineProperty(proto,prop,{configurable:true,enumerable:d.enumerable,get:function(){return revive(d.get.call(this))}})}catch(e){}}
if(window.IDBRequest)patchGetter(IDBRequest.prototype,'result');
if(window.IDBCursorWithValue)patchGetter(IDBCursorWithValue.prototype,'value');

/* ---------- دانلود فایل‌ها (پشتیبان، تقویم، ZIP) → /LAITNO/Backups یا Exports ---------- */
function handleDl(a){try{if(!a||!a.hasAttribute||!a.hasAttribute('download'))return false;var h=a.href||'';if(!/^(blob:|data:)/i.test(h))return false;
  var name=clean(a.getAttribute('download')||'download');
  (async function(){var b=await (await fetch(h)).blob();await accessReady();var sub=/\.json$/i.test(name)?'Backups':'Exports';N.mkdir(sub);var p=sub+'/'+name;
    await writeBlob(p,b);N.toast('ذخیره شد: LAITNO/'+p);if(/\.ics$/i.test(name))N.openFile(p,'text/calendar')})()
  .catch(function(){N.toast('ذخیرهٔ فایل انجام نشد')});
  return true}catch(e){return false}}
var _click=HTMLAnchorElement.prototype.click;
HTMLAnchorElement.prototype.click=function(){if(handleDl(this))return;return _click.apply(this,arguments)};
document.addEventListener('click',function(e){var a=e.target&&e.target.closest&&e.target.closest('a[download]');if(a&&handleDl(a))e.preventDefault()},true);

/* ---------- تلفظ: موتور TTS اندروید ---------- */
var VOICES=[],SEQ=0,UM=new Map();
function fire(t,type,extra){var e;try{e=new Event(type)}catch(_){e={type:type}}if(extra)for(var k in extra){try{Object.defineProperty(e,k,{value:extra[k]})}catch(_){}}
  try{t['on'+type]&&t['on'+type].call(t,e)}catch(_){}try{t.dispatchEvent&&t.dispatchEvent(e)}catch(_){}}
function Utt(text){var et=new EventTarget();et.text=text==null?'':String(text);et.lang='';et.rate=1;et.pitch=1;et.volume=1;et.voice=null;
  et.onstart=et.onend=et.onerror=et.onboundary=et.onpause=et.onresume=et.onmark=null;try{Object.setPrototypeOf(et,Utt.prototype)}catch(_){}return et}
Utt.prototype=Object.create(EventTarget.prototype);Utt.prototype.constructor=Utt;
var SS=new EventTarget();try{Object.setPrototypeOf(SS,Object.create(EventTarget.prototype))}catch(_){}
SS.speaking=false;SS.pending=false;SS.paused=false;SS.onvoiceschanged=null;
SS.getVoices=function(){return VOICES.slice()};
SS.speak=function(u){var id=++SEQ;UM.set(id,u);SS.speaking=true;
  N.ttsSpeak(String(id),String(u.text||''),String((u.voice&&u.voice.lang)||u.lang||'en-US'),String(+u.rate||1),String(+u.pitch||1))};
SS.cancel=function(){N.ttsCancel();var l=Array.from(UM.values());UM.clear();SS.speaking=false;l.forEach(function(u){fire(u,'error',{error:'interrupted',utterance:u})})};
SS.pause=function(){};SS.resume=function(){};
L.tts=function(id,ev){var u=UM.get(+id);if(!u)return;if(ev==='start'){fire(u,'start',{utterance:u});return}
  UM.delete(+id);SS.speaking=UM.size>0;fire(u,ev==='error'?'error':'end',ev==='error'?{error:'synthesis-failed',utterance:u}:{utterance:u})};
L.voices=function(){var l=J(N.ttsVoices())||[];VOICES=l.map(function(v,i){return{name:v.name,lang:v.lang,voiceURI:v.name,localService:true,default:i===0}});
  fire(SS,'voiceschanged')};
try{Object.defineProperty(window,'speechSynthesis',{value:SS,configurable:true,writable:true})}catch(e){window.speechSynthesis=SS}
window.SpeechSynthesisUtterance=Utt;
if(N.ttsReady())L.voices();

/* ---------- تشخیص گفتار (تمرین تلفظ با میکروفون) ---------- */
if(N.srAvailable()){
  var RSEQ=0,RM=new Map();
  var SR=function(){var et=new EventTarget();et.lang='en-US';et.maxAlternatives=1;et.interimResults=false;et.continuous=false;et._id=0;
    et.onresult=et.onerror=et.onend=et.onstart=et.onspeechstart=et.onspeechend=et.onnomatch=et.onaudiostart=et.onaudioend=null;try{Object.setPrototypeOf(et,SR.prototype)}catch(_){}return et};
  SR.prototype=Object.create(EventTarget.prototype);SR.prototype.constructor=SR;
  SR.prototype.start=function(){if(this._id)throw DE('InvalidStateError');this._id=++RSEQ;RM.set(this._id,this);N.srStart(String(this._id),String(this.lang||'en-US'),String(this.maxAlternatives||1))};
  SR.prototype.stop=function(){if(this._id)N.srStop(String(this._id))};
  SR.prototype.abort=function(){if(this._id)N.srAbort(String(this._id))};
  L.sr=function(id,type,data){var r=RM.get(+id);if(!r)return;
    if(type==='result'){var alts=(data||[]).map(function(a){return{transcript:a.t,confidence:a.c}});var res=alts.slice();res.isFinal=true;res.item=function(i){return res[i]};
      var results=[res];results.item=function(i){return results[i]};fire(r,'result',{results:results,resultIndex:0})}
    else if(type==='error')fire(r,'error',{error:data,message:data});
    else if(type==='end'){RM.delete(+id);r._id=0;fire(r,'end')}
    else fire(r,type)};
  window.SpeechRecognition=SR;window.webkitSpeechRecognition=SR;
}

/* ---------- اعلان ---------- */
var nW=[];
L.onNotif=function(p){var w=nW;nW=[];w.forEach(function(f){f(p)})};
function Notif(title,o){o=o||{};this.title=String(title||'');this.body=o.body||'';this.tag=o.tag||'';this.onclick=this.onclose=this.onerror=this.onshow=null;
  N.notify(this.title,String(this.body),String(this.tag))}
Notif.prototype.close=function(){};Notif.prototype.addEventListener=function(){};Notif.prototype.removeEventListener=function(){};
Object.defineProperty(Notif,'permission',{get:function(){return N.notifPerm()}});
Notif.requestPermission=function(cb){return new Promise(function(res){var p=N.notifPerm();var done=function(v){try{cb&&cb(v)}catch(e){}res(v)};
  if(p!=='default')return done(p);nW.push(done);N.notifRequest()})};
Notif.maxActions=0;
try{Object.defineProperty(window,'Notification',{value:Notif,configurable:true,writable:true})}catch(e){window.Notification=Notif}

/* یادآوری روزانه حتی وقتی برنامه بسته است: تنظیمات یادآوری برنامه به اندروید داده می‌شود */
function syncRem(){try{var s=JSON.parse(localStorage.getItem('rms-leitner-v1')||'{}');if(s&&s.rem)N.scheduleReminder(JSON.stringify(s.rem))}catch(e){}}
document.addEventListener('visibilitychange',function(){if(document.visibilityState==='hidden')syncRem()});
window.addEventListener('pagehide',syncRem);
setInterval(syncRem,60000);

/* ---------- اشتراک، کلیپ‌بورد، روشن ماندن صفحه، حالت برنامه ---------- */
navigator.share=function(d){d=d||{};N.share(String(d.title||''),[d.text,d.url].filter(Boolean).join('\n'));return Promise.resolve()};
navigator.canShare=function(d){return !(d&&d.files&&d.files.length)};
try{var wt=function(t){N.copy(String(t));return Promise.resolve()};
  if(navigator.clipboard)Object.defineProperty(navigator.clipboard,'writeText',{value:wt,configurable:true});
  else Object.defineProperty(navigator,'clipboard',{value:{writeText:wt,readText:function(){return Promise.resolve('')}},configurable:true})}catch(e){}
var WL=0;
try{Object.defineProperty(navigator,'wakeLock',{configurable:true,value:{request:async function(){WL++;N.keepScreen(true);
  var s={type:'screen',released:false,onrelease:null,addEventListener:function(){},removeEventListener:function(){},
    release:async function(){if(s.released)return;s.released=true;WL=Math.max(0,WL-1);if(!WL)N.keepScreen(false);try{s.onrelease&&s.onrelease()}catch(e){}}};return s}}})}catch(e){}
try{Object.defineProperty(navigator,'standalone',{value:true,configurable:true})}catch(e){}
var _mm=window.matchMedia.bind(window);
window.matchMedia=function(q){if(/display-mode\s*:\s*(standalone|fullscreen)/i.test(q))return{matches:true,media:q,onchange:null,addListener:function(){},removeListener:function(){},addEventListener:function(){},removeEventListener:function(){},dispatchEvent:function(){return false}};return _mm(q)};
try{if(navigator.storage){navigator.storage.persist=function(){return Promise.resolve(true)};navigator.storage.persisted=function(){return Promise.resolve(true)}}}catch(e){}

/* ---------- اولین اجرا: پوشهٔ عکس و تلفظ خودکار روی حافظهٔ گوشی تنظیم شود ---------- */
async function autoSetup(){if(localStorage.getItem('ltn-auto-folders')==='1')return;await accessReady();
  var run=function(f){return window.LT_PERM?window.LT_PERM(f):f()};
  try{var I=window.LAITNO_IMGFS;if(I&&I.pick&&!(I.on&&I.on()))await run(function(){return I.pick()})}catch(e){}
  try{var A=window.LAITNO_AUFS;if(A&&A.pick&&!(A.st&&A.st()==='ok'))await run(function(){return A.pick()})}catch(e){}
  localStorage.setItem('ltn-auto-folders','1')}
window.addEventListener('load',function(){setTimeout(function(){autoSetup().catch(function(){})},1800)});
})();
