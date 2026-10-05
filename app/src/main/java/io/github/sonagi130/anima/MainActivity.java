package io.github.sonagi130.anima;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int REQ_FILE = 4101;
    private static final int REQ_PERMS = 4102;
    private Updater updater;

    /* 本地资源域名：跟网页版同源，localStorage 无缝继承，数据不用迁移 */
    private static final String HOST = "sonagi130.github.io";
    private static final String BASE = "/anima/";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        // 原生桥：暴露给网页调用的接口
        web.addJavascriptInterface(new Bridge(), "HearthBridge");

        /* 本地化：sonagi130.github.io/anima/* → assets/anima/*，秒开 + 离线可用 + 数据同源 */
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                String host = url.getHost();
                String path = url.getPath();
                if ((HOST.equals(host) || "appassets.androidplatform.net".equals(host))
                        && path != null && path.startsWith(BASE)) {
                    String file = path.substring(BASE.length());
                    if (file.isEmpty()) file = "index.html";
                    WebResourceResponse r = asset(file);
                    if (r != null) return r;
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        // 热更新检查：新版本则下载替换，不阻塞启动
        updater = new Updater(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                updater.updateIfNeeded();
            }
        }).start();

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
            }

            // 文件选择桥：网页里 <input type=file> 时系统弹选择器
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    Intent i = params.createIntent();
                    startActivityForResult(i, REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });
        web.loadUrl("https://" + HOST + BASE + "index.html");
        // 启动保活服务（小工宿主）
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(new Intent(this, HearthService.class));
            } else {
                startService(new Intent(this, HearthService.class));
            }
        } catch (Exception e) { /* 用户关了后台限制的话忽略 */ }
        // Android 6.0+ 动态权限：进 App 就请求（相机/录音/定位），文件选择走系统选择器不需要
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQ_PERMS);
            } catch (Exception e) { /* 已经授权或弹不了，无所谓 */ }
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE) {
            if (filePathCallback != null) {
                Uri[] uris = (res == RESULT_OK && data != null)
                        ? WebChromeClient.FileChooserParams.parseResult(res, data) : null;
                filePathCallback.onReceiveValue(uris);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(req, res, data);
    }

    /** 从更新目录或 assets/anima 读文件，返回响应 */
    private WebResourceResponse asset(String file) {
        // 优先读热更新目录
        if (updater != null) {
            InputStream uin = updater.openFile(file);
            if (uin != null) {
                return new WebResourceResponse(mimeOf(file), "utf-8", uin);
            }
        }
        try {
            InputStream in = getAssets().open("anima/" + file);
            return new WebResourceResponse(mimeOf(file), "utf-8", in);
        } catch (IOException e) {
            return null;
        }
    }

    private String mimeOf(String file) {
        if (file.endsWith(".html")) return "text/html";
        if (file.endsWith(".js")) return "application/javascript";
        if (file.endsWith(".css")) return "text/css";
        if (file.endsWith(".json")) return "application/json";
        if (file.endsWith(".png")) return "image/png";
        if (file.endsWith(".jpg") || file.endsWith(".jpeg")) return "image/jpeg";
        if (file.endsWith(".svg")) return "image/svg+xml";
        if (file.endsWith(".webp")) return "image/webp";
        if (file.endsWith(".woff2")) return "font/woff2";
        if (file.endsWith(".woff")) return "font/woff";
        if (file.endsWith(".ttf")) return "font/ttf";
        if (file.endsWith(".enc")) return "application/octet-stream";
        if (file.endsWith(".mp3")) return "audio/mpeg";
        if (file.endsWith(".wav")) return "audio/wav";
        if (file.endsWith(".webm")) return "video/webm";
        if (file.endsWith(".mp4")) return "video/mp4";
        String ext = MimeTypeMap.getFileExtensionFromUrl(file);
        if (ext != null) {
            String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (m != null) return m;
        }
        return "application/octet-stream";
    }

    /** 网页可调的桥：window.HearthBridge */
    private class Bridge {
        @android.webkit.JavascriptInterface
        public String deviceInfo() {
            return Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE;
        }

        @android.webkit.JavascriptInterface
        public boolean hasPermission(String name) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(name) || Manifest.permission.ACCESS_COARSE_LOCATION.equals(name)) {
                return checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }

        // ---- 小工：通知 ----
        @android.webkit.JavascriptInterface
        public String notifications() {
            return HearthNotif.snapshot();
        }

        // ---- 小工：读屏/点击 ----
        @android.webkit.JavascriptInterface
        public String screenText() {
            HearthAccess a = HearthAccess.get();
            return a == null ? "无障碍未开启" : (a.lastScreen == null ? "" : a.lastScreen);
        }

        @android.webkit.JavascriptInterface
        public boolean tapText(String text) {
            HearthAccess a = HearthAccess.get();
            return a != null && a.tapText(text);
        }

        @android.webkit.JavascriptInterface
        public boolean tapAt(float x, float y) {
            HearthAccess a = HearthAccess.get();
            return a != null && a.tapAt(x, y);
        }

        @android.webkit.JavascriptInterface
        public boolean swipe(float x1, float y1, float x2, float y2) {
            HearthAccess a = HearthAccess.get();
            return a != null && a.swipe(x1, y1, x2, y2);
        }

        // ---- 小工：剪贴板 ----
        @android.webkit.JavascriptInterface
        public void copy(String text) {
            try {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("fatum", text));
            } catch (Exception ignored) {}
        }

        // ---- 小工：Toast ----
        @android.webkit.JavascriptInterface
        public void toast(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.widget.Toast.makeText(MainActivity.this, msg,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}