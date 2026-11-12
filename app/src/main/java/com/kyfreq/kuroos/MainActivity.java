package com.kyfreq.kuroos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** The home screen: a transparent WebView over the phone wallpaper, running assets/index.html. */
public class MainActivity extends Activity {
    private static final int FILE_REQ = 42;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new Bridge(this, web), "Android");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            // Lets the sprite and wallpaper upload buttons open the phone's file picker.
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQ);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQ && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /** Back closes menus and the app drawer; a home screen never exits. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.kuroBack&&kuroBack()", null);
    }

    /** Pressing Home while already on the home screen jumps back to page 1. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        web.evaluateJavascript("window.kuroHome&&kuroHome()", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.evaluateJavascript("window.kuroResume&&kuroResume()", null);
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }
}
