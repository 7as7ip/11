package ir.laitno.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Base64;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.ServiceWorkerControllerCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewFeature;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    static volatile boolean foreground = false;

    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/www/index.html";
    private static final int REQ_RUNTIME = 11, REQ_NOTIF = 12, REQ_MIC = 13, REQ_FILE = 21;

    private WebView web;
    private WebViewAssetLoader loader;
    private SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private ValueCallback<Uri[]> fileCb;

    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

    private SpeechRecognizer sr;
    private String srId = null;
    private String[] pendingSr = null;

    private volatile boolean setupPending = false;
    private boolean waitingAllFiles = false;

    // ------------------------------------------------------------------ lifecycle

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = Reminders.prefs(this);
        Reminders.channel(this);

        loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/__ltfs/", new FsHandler())
                .build();

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.addJavascriptInterface(new Bridge(), "LTN");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true; // لینک‌های بیرونی در مرورگر باز شوند
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*"); // پسوندهایی مثل .json در اندروید نوع درستی ندارند؛ همه نمایش داده شوند
                if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(i, "انتخاب فایل"), REQ_FILE);
                } catch (ActivityNotFoundException e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(new ServiceWorkerClientCompat() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    return loader.shouldInterceptRequest(request.getUrl());
                }
            });
        }

        tts = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                tts.setLanguage(Locale.US);
                js("window.__ltn&&__ltn.voices()");
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { js("window.__ltn&&__ltn.tts(" + q(id) + ",'start')"); }
            @Override public void onDone(String id) { js("window.__ltn&&__ltn.tts(" + q(id) + ",'end')"); }
            @Override public void onError(String id) { js("window.__ltn&&__ltn.tts(" + q(id) + ",'error')"); }
            @Override public void onStop(String id, boolean interrupted) { js("window.__ltn&&__ltn.tts(" + q(id) + ",'end')"); }
        });

        startSetup(false);

        web.loadUrl(START_URL);
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        web.onResume();
        if (waitingAllFiles) { waitingAllFiles = false; finishSetup(); }
    }

    @Override protected void onPause() {
        foreground = false;
        web.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        try { if (tts != null) tts.shutdown(); } catch (Exception ignored) {}
        try { if (sr != null) sr.destroy(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    // ------------------------------------------------------------------ permissions (asked once, on first launch)

    private boolean granted(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    boolean hasAllFiles() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return granted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private List<String> missingRuntime() {
        List<String> m = new ArrayList<>();
        if (!granted(Manifest.permission.RECORD_AUDIO)) m.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS))
            m.add(Manifest.permission.POST_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT <= 29) {
            if (!granted(Manifest.permission.READ_EXTERNAL_STORAGE)) m.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (!granted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) m.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return m;
    }

    private void startSetup(boolean force) {
        if (setupPending) return;
        boolean need = !hasAllFiles() || !missingRuntime().isEmpty();
        boolean asked = prefs.getBoolean("askedOnce", false);
        // بعد از بار اول، فقط اگر دسترسی حافظه هنوز نیست دوباره بپرس (مگر کاربر «دیگه نپرس» زده باشد)
        if (!need || (!force && asked && (hasAllFiles() || prefs.getBoolean("neverAsk", false)))) {
            finishSetup();
            return;
        }
        setupPending = true;
        String msg = "برای اینکه LAITNO مثل یک برنامهٔ واقعی کار کنه این دسترسی‌ها لازمه:\n\n"
                + "📁 حافظه: عکس‌ها و تلفظ‌ها در پوشهٔ «LAITNO» حافظهٔ گوشی ذخیره می‌شن و برنامه همیشه یادش می‌مونه کجان (دیگه هر بار نمی‌پرسه)\n\n"
                + "🎤 میکروفون: تمرین تشخیص تلفظ\n\n"
                + "🔔 اعلان: یادآوری مرور روزانه";
        new AlertDialog.Builder(this)
                .setTitle("دسترسی‌های LAITNO")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("اجازه می‌دم", (d, w) -> askRuntime())
                .setNegativeButton("بعداً", (d, w) -> finishSetup())
                .setNeutralButton("دیگه نپرس", (d, w) -> {
                    prefs.edit().putBoolean("neverAsk", true).apply();
                    finishSetup();
                })
                .show();
    }

    private void askRuntime() {
        prefs.edit().putBoolean("askedOnce", true).apply();
        List<String> m = missingRuntime();
        if (!m.isEmpty()) requestPermissions(m.toArray(new String[0]), REQ_RUNTIME);
        else askAllFiles();
    }

    private void askAllFiles() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            waitingAllFiles = true;
            toast("گزینهٔ «اجازهٔ دسترسی به همهٔ فایل‌ها» رو برای LAITNO روشن کن و برگرد");
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                try { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
                catch (Exception e2) { waitingAllFiles = false; finishSetup(); }
            }
        } else {
            finishSetup();
        }
    }

    private void finishSetup() {
        setupPending = false;
        if (hasAllFiles()) migrateFallback();
        String st = hasAllFiles() ? "granted" : "fallback";
        js("window.__ltn&&__ltn.onAccess(" + q(st) + ")");
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        for (String p : perms) if (Manifest.permission.POST_NOTIFICATIONS.equals(p))
            prefs.edit().putBoolean("askedNotif", true).apply();
        if (code == REQ_RUNTIME) {
            askAllFiles();
        } else if (code == REQ_NOTIF) {
            js("window.__ltn&&__ltn.onNotif(" + q(notifPerm()) + ")");
        } else if (code == REQ_MIC) {
            String[] a = pendingSr;
            pendingSr = null;
            if (a == null) return;
            if (granted(Manifest.permission.RECORD_AUDIO)) srStartNow(a[0], a[1], a[2]);
            else { srSend(a[0], "error", q("not-allowed")); srSend(a[0], "end", "null"); }
        }
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != REQ_FILE || fileCb == null) return;
        Uri[] out = null;
        if (result == RESULT_OK && data != null) {
            ClipData cd = data.getClipData();
            if (cd != null && cd.getItemCount() > 0) {
                out = new Uri[cd.getItemCount()];
                for (int i = 0; i < cd.getItemCount(); i++) out[i] = cd.getItemAt(i).getUri();
            } else if (data.getData() != null) {
                out = new Uri[]{data.getData()};
            }
        }
        fileCb.onReceiveValue(out);
        fileCb = null;
    }

    String notifPerm() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (granted(Manifest.permission.POST_NOTIFICATIONS)) return "granted";
            return prefs.getBoolean("askedNotif", false) ? "denied" : "default";
        }
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return (nm == null || nm.areNotificationsEnabled()) ? "granted" : "denied";
    }

    // ------------------------------------------------------------------ storage: /sdcard/LAITNO (همیشه همین‌جا)

    File publicRoot() { return new File(Environment.getExternalStorageDirectory(), "LAITNO"); }

    File fallbackRoot() {
        File d = getExternalFilesDir(null);
        if (d == null) d = getFilesDir();
        return new File(d, "LAITNO");
    }

    File root() {
        File r = hasAllFiles() ? publicRoot() : fallbackRoot();
        if (!r.exists()) r.mkdirs();
        return r;
    }

    File resolve(String rel) {
        File r = root();
        if (rel == null) rel = "";
        rel = rel.replace('\\', '/');
        while (rel.startsWith("/")) rel = rel.substring(1);
        for (String seg : rel.split("/")) if (seg.equals("..")) return null;
        File f = rel.isEmpty() ? r : new File(r, rel);
        try {
            String rp = r.getCanonicalPath(), fp = f.getCanonicalPath();
            if (!fp.equals(rp) && !fp.startsWith(rp + File.separator)) return null;
        } catch (IOException e) { return null; }
        return f;
    }

    /** اگر قبلاً بدون دسترسی، فایل‌ها در پوشهٔ خصوصی برنامه ذخیره شده بودند، به پوشهٔ LAITNO منتقل شوند. */
    private void migrateFallback() {
        final File from = fallbackRoot();
        if (!from.isDirectory()) return;
        new Thread(() -> {
            try { copyTree(from, publicRoot()); deleteTree(from); } catch (Exception ignored) {}
        }).start();
    }

    private static void copyTree(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            File[] kids = src.listFiles();
            if (kids != null) for (File k : kids) copyTree(k, new File(dst, k.getName()));
        } else if (!dst.exists()) {
            copyFile(src, dst);
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static boolean deleteTree(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteTree(k);
        }
        return f.delete();
    }

    static String mime(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".m4a") || n.endsWith(".aac")) return "audio/mp4";
        if (n.endsWith(".ogg") || n.endsWith(".oga")) return "audio/ogg";
        if (n.endsWith(".opus")) return "audio/ogg";
        if (n.endsWith(".wav")) return "audio/wav";
        if (n.endsWith(".webm")) return "audio/webm";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".avif")) return "image/avif";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".zip")) return "application/zip";
        if (n.endsWith(".ics")) return "text/calendar";
        if (n.endsWith(".txt") || n.endsWith(".csv") || n.endsWith(".tsv")) return "text/plain";
        return "application/octet-stream";
    }

    /** فایل‌های پوشهٔ LAITNO از آدرس https://appassets.androidplatform.net/__ltfs/<base64> خوانده می‌شوند (سریع، بدون base64 در JS). */
    private class FsHandler implements WebViewAssetLoader.PathHandler {
        @Override
        public WebResourceResponse handle(String path) {
            Map<String, String> h = new HashMap<>();
            h.put("Cache-Control", "no-store");
            h.put("Access-Control-Allow-Origin", "*");
            try {
                String rel = new String(Base64.decode(path, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP), StandardCharsets.UTF_8);
                File f = resolve(rel);
                if (f != null && f.isFile())
                    return new WebResourceResponse(mime(f.getName()), null, 200, "OK", h, new FileInputStream(f));
            } catch (Exception ignored) {}
            return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", h, new ByteArrayInputStream(new byte[0]));
        }
    }

    // ------------------------------------------------------------------ helpers

    static String q(String s) { return JSONObject.quote(s == null ? "" : s); }

    void js(final String code) {
        ui.post(() -> { if (web != null) web.evaluateJavascript(code, null); });
    }

    void toast(final String m) {
        ui.post(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
    }

    private void srSend(String id, String type, String dataJson) {
        js("window.__ltn&&__ltn.sr(" + q(id) + "," + q(type) + "," + dataJson + ")");
    }

    private void srStartNow(final String id, String lang, String max) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                srSend(id, "error", q("service-not-allowed"));
                srSend(id, "end", "null");
                return;
            }
            if (sr == null) sr = SpeechRecognizer.createSpeechRecognizer(this);
            srId = id;
            sr.setRecognitionListener(new RecognitionListener() {
                boolean sent = false;
                @Override public void onReadyForSpeech(Bundle b) { srSend(id, "start", "null"); }
                @Override public void onBeginningOfSpeech() { srSend(id, "speechstart", "null"); }
                @Override public void onRmsChanged(float v) {}
                @Override public void onBufferReceived(byte[] b) {}
                @Override public void onEndOfSpeech() { srSend(id, "speechend", "null"); }
                @Override public void onError(int e) {
                    if (sent) return;
                    sent = true;
                    String err;
                    switch (e) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: err = "no-speech"; break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: err = "not-allowed"; break;
                        case SpeechRecognizer.ERROR_NETWORK:
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        case SpeechRecognizer.ERROR_SERVER: err = "network"; break;
                        case SpeechRecognizer.ERROR_AUDIO: err = "audio-capture"; break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: err = "busy"; break;
                        default: err = "aborted";
                    }
                    srSend(id, "error", q(err));
                    srSend(id, "end", "null");
                }
                @Override public void onResults(Bundle b) {
                    if (sent) return;
                    sent = true;
                    JSONArray arr = new JSONArray();
                    ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    float[] c = b.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                    if (r != null) for (int i = 0; i < r.size(); i++) {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("t", r.get(i));
                            o.put("c", (c != null && i < c.length && c[i] >= 0) ? c[i] : 0.8);
                            arr.put(o);
                        } catch (Exception ignored) {}
                    }
                    if (arr.length() > 0) srSend(id, "result", arr.toString());
                    else srSend(id, "error", q("no-speech"));
                    srSend(id, "end", "null");
                }
                @Override public void onPartialResults(Bundle b) {}
                @Override public void onEvent(int t, Bundle b) {}
            });
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, Math.max(1, Integer.parseInt(max)));
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            sr.startListening(i);
        } catch (Exception e) {
            srSend(id, "error", q("aborted"));
            srSend(id, "end", "null");
        }
    }

    // ------------------------------------------------------------------ JS bridge  (window.LTN)

    private class Bridge {

        @JavascriptInterface public String accessState() {
            if (setupPending) return "pending";
            return hasAllFiles() ? "granted" : "fallback";
        }

        @JavascriptInterface public String rootPath() { return root().getAbsolutePath(); }

        @JavascriptInterface public void requestAccess() { ui.post(() -> startSetup(true)); }

        @JavascriptInterface public String list(String rel) {
            File d = resolve(rel);
            if (d == null || !d.isDirectory()) return "null";
            JSONArray a = new JSONArray();
            File[] kids = d.listFiles();
            if (kids != null) for (File k : kids) {
                String n = k.getName();
                if (n.endsWith(".lttmp")) continue;
                try {
                    JSONObject o = new JSONObject();
                    o.put("n", n);
                    o.put("d", k.isDirectory());
                    o.put("s", k.isDirectory() ? 0 : k.length());
                    o.put("m", k.lastModified());
                    a.put(o);
                } catch (Exception ignored) {}
            }
            return a.toString();
        }

        @JavascriptInterface public String stat(String rel) {
            File f = resolve(rel);
            if (f == null || !f.exists()) return "null";
            try {
                JSONObject o = new JSONObject();
                o.put("d", f.isDirectory());
                o.put("s", f.isDirectory() ? 0 : f.length());
                o.put("m", f.lastModified());
                return o.toString();
            } catch (Exception e) { return "null"; }
        }

        @JavascriptInterface public boolean mkdir(String rel) {
            File f = resolve(rel);
            return f != null && (f.isDirectory() || f.mkdirs());
        }

        @JavascriptInterface public boolean writeChunk(String rel, String b64, boolean append) {
            File f = resolve(rel);
            if (f == null) return false;
            File p = f.getParentFile();
            if (p != null && !p.exists()) p.mkdirs();
            try (OutputStream out = new FileOutputStream(f, append)) {
                if (b64 != null && !b64.isEmpty()) out.write(Base64.decode(b64, Base64.DEFAULT));
                return true;
            } catch (Exception e) { return false; }
        }

        @JavascriptInterface public boolean rename(String from, String to) {
            File a = resolve(from), b = resolve(to);
            if (a == null || b == null || !a.exists()) return false;
            if (b.exists() && !b.isDirectory()) b.delete();
            if (a.renameTo(b)) return true;
            try { copyFile(a, b); a.delete(); return true; } catch (Exception e) { return false; }
        }

        @JavascriptInterface public boolean remove(String rel, boolean recursive) {
            File f = resolve(rel);
            if (f == null || !f.exists()) return false;
            if (f.equals(root())) return false;
            if (f.isDirectory()) {
                String[] k = f.list();
                if (k != null && k.length > 0 && !recursive) return false;
                return deleteTree(f);
            }
            return f.delete();
        }

        @JavascriptInterface public void openFile(String rel, String mime) {
            ui.post(() -> {
                try {
                    File f = resolve(rel);
                    if (f == null || !f.isFile()) return;
                    Uri u = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(u, mime == null || mime.isEmpty() ? mime(f.getName()) : mime);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "باز کردن با"));
                } catch (Exception e) { toast("برنامه‌ای برای باز کردن این فایل پیدا نشد"); }
            });
        }

        // ---- TTS (تلفظ)
        @JavascriptInterface public boolean ttsReady() { return ttsReady; }

        @JavascriptInterface public String ttsVoices() {
            JSONArray a = new JSONArray();
            if (!ttsReady) return a.toString();
            Set<String> seen = new LinkedHashSet<>();
            try {
                Set<Voice> vs = tts.getVoices();
                if (vs != null) for (Voice v : vs) {
                    String tag = v.getLocale().toLanguageTag();
                    if (!tag.startsWith("en") && !tag.startsWith("fa")) continue;
                    if (seen.add(tag)) {
                        JSONObject o = new JSONObject();
                        o.put("name", "Android " + tag);
                        o.put("lang", tag);
                        a.put(o);
                    }
                }
            } catch (Exception ignored) {}
            if (a.length() == 0) {
                for (String t : new String[]{"en-US", "en-GB"}) {
                    try { JSONObject o = new JSONObject(); o.put("name", "Android " + t); o.put("lang", t); a.put(o); } catch (Exception ignored) {}
                }
            }
            return a.toString();
        }

        @JavascriptInterface public void ttsSpeak(String id, String text, String lang, String rate, String pitch) {
            if (!ttsReady) { js("window.__ltn&&__ltn.tts(" + q(id) + ",'error')"); return; }
            try {
                Locale loc = Locale.forLanguageTag(lang == null || lang.isEmpty() ? "en-US" : lang);
                if (tts.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) tts.setLanguage(loc);
                tts.setSpeechRate(Float.parseFloat(rate));
                tts.setPitch(Float.parseFloat(pitch));
            } catch (Exception ignored) {}
            Bundle b = new Bundle();
            tts.speak(text, TextToSpeech.QUEUE_ADD, b, id);
        }

        @JavascriptInterface public void ttsCancel() { try { if (tts != null) tts.stop(); } catch (Exception ignored) {} }

        // ---- تشخیص گفتار (تمرین تلفظ)
        @JavascriptInterface public boolean srAvailable() { return SpeechRecognizer.isRecognitionAvailable(MainActivity.this); }

        @JavascriptInterface public void srStart(String id, String lang, String max) {
            ui.post(() -> {
                if (!granted(Manifest.permission.RECORD_AUDIO)) {
                    pendingSr = new String[]{id, lang, max};
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
                } else srStartNow(id, lang, max);
            });
        }

        @JavascriptInterface public void srStop(String id) { ui.post(() -> { try { if (sr != null) sr.stopListening(); } catch (Exception ignored) {} }); }

        @JavascriptInterface public void srAbort(String id) {
            ui.post(() -> {
                try { if (sr != null) sr.cancel(); } catch (Exception ignored) {}
                srSend(id, "error", q("aborted"));
                srSend(id, "end", "null");
            });
        }

        // ---- اعلان و یادآوری
        @JavascriptInterface public String notifPerm() { return MainActivity.this.notifPerm(); }

        @JavascriptInterface public void notifRequest() {
            ui.post(() -> {
                if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS))
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
                else js("window.__ltn&&__ltn.onNotif(" + q(MainActivity.this.notifPerm()) + ")");
            });
        }

        @JavascriptInterface public void notify(String title, String body, String tag) {
            Reminders.notify(MainActivity.this, title, body, tag == null || tag.isEmpty() ? (int) (System.currentTimeMillis() % 100000) : tag.hashCode());
        }

        @JavascriptInterface public void scheduleReminder(String json) { Reminders.save(MainActivity.this, json); }

        // ---- بقیه
        @JavascriptInterface public void share(String title, String text) {
            ui.post(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, title);
                i.putExtra(Intent.EXTRA_TEXT, text);
                try { startActivity(Intent.createChooser(i, title == null || title.isEmpty() ? "اشتراک" : title)); } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface public void copy(String text) {
            ui.post(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("LAITNO", text));
            });
        }

        @JavascriptInterface public void keepScreen(boolean on) {
            ui.post(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface public void toast(String m) { MainActivity.this.toast(m); }
    }
}
