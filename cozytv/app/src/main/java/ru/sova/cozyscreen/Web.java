package ru.sova.cozyscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Встроенный браузер со скринсейвером. Страница и музыка лежат внутри APK (assets/www)
 * и отдаются по адресу https://cozy.local/ — так работают звук, localStorage
 * и всё остальное без интернета.
 */
final class Web {
    static final String HOST = "cozy.local";

    @SuppressLint("SetJavaScriptEnabled")
    static WebView make(Context ctx, String query) {
        WebView w = new WebView(ctx);
        w.setBackgroundColor(Color.BLACK);
        w.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        w.setVerticalScrollBarEnabled(false);
        w.setHorizontalScrollBarEnabled(false);
        w.setFocusable(true);
        w.setFocusableInTouchMode(true);

        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        w.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (!HOST.equals(u.getHost())) {
                    // наружу ничего не ходит: приложение полностью офлайн
                    return notFound();
                }
                String path = u.getPath();
                if (path == null || path.equals("/") || path.isEmpty()) path = "/index.html";
                try {
                    InputStream in = ctx.getAssets().open("www" + path);
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cache-Control", "no-cache");
                    return new WebResourceResponse(mime(path), mime(path).startsWith("text") ? "utf-8" : null,
                            200, "OK", headers, in);
                } catch (IOException e) {
                    return notFound();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return !HOST.equals(req.getUrl().getHost());
            }
        });

        w.loadUrl("https://" + HOST + "/index.html" + (query == null || query.isEmpty() ? "" : "?" + query));
        return w;
    }

    private static WebResourceResponse notFound() {
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null,
                new java.io.ByteArrayInputStream(new byte[0]));
    }

    private static String mime(String p) {
        p = p.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "text/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private Web() {}
}
