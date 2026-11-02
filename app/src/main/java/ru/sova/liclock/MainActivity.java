package ru.sova.liclock;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.List;
import java.util.Locale;

/**
 * Часы для экрана пассажира Lixiang.
 * Весь интерфейс — в assets/clock.html; здесь только полноэкранный WebView
 * и передача координат машины (для восхода/заката и названия города).
 */
public class MainActivity extends Activity {

    private static final int REQ_LOC = 42;
    private WebView web;
    private boolean pageReady = false;
    private Location pending = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#111113"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // чтобы запоминались тема и циферблат
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (pending != null) sendLocation(pending);
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/clock.html");

        startLocation();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        web.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // ---------- геолокация ----------

    private void startLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return;
        }
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm == null) return;
            Location best = null;
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best != null) onLocation(best);
            // одно свежее обновление — машине не нужно следить постоянно
            for (String p : lm.getProviders(true)) {
                lm.requestSingleUpdate(p, new LocationListener() {
                    @Override public void onLocationChanged(Location l) { onLocation(l); }
                    @Override public void onStatusChanged(String a, int b, Bundle c) {}
                    @Override public void onProviderEnabled(String a) {}
                    @Override public void onProviderDisabled(String a) {}
                }, Looper.getMainLooper());
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
            // без координат часы покажут последнюю сохранённую точку или Москву
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_LOC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        }
    }

    private void onLocation(Location l) {
        if (pageReady) sendLocation(l); else pending = l;
    }

    private void sendLocation(final Location l) {
        new Thread(() -> {
            String city = "";
            try {
                if (Geocoder.isPresent()) {
                    List<Address> a = new Geocoder(this, new Locale("ru")).getFromLocation(l.getLatitude(), l.getLongitude(), 1);
                    if (a != null && !a.isEmpty() && a.get(0).getLocality() != null) city = a.get(0).getLocality();
                }
            } catch (Exception ignored) { }
            final String js = String.format(Locale.US, "window.setLoc&&setLoc(%f,%f,%s)",
                    l.getLatitude(), l.getLongitude(), quote(city));
            runOnUiThread(() -> web.evaluateJavascript(js, null));
        }).start();
    }

    private static String quote(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    @Override protected void onResume() { super.onResume(); web.onResume(); web.resumeTimers(); }
    @Override protected void onPause() { web.onPause(); web.pauseTimers(); super.onPause(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
