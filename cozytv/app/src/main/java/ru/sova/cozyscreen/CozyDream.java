package ru.sova.cozyscreen;

import android.service.dreams.DreamService;
import android.webkit.WebView;

/** Системная заставка: телевизор сам включает сцены, когда простаивает. Без музыки и меню. */
public class CozyDream extends DreamService {
    private WebView web;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        web = Web.make(this, "dream=1");
        setContentView(web);
    }

    @Override
    public void onDetachedFromWindow() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDetachedFromWindow();
    }
}
