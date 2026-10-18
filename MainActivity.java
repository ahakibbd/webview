package com.example.webviewapp;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.myWebView);

        // WebSettings কনফিগারেশন
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // JavaScript সাপোর্ট চালু করা
        webSettings.setDomStorageEnabled(true); // ক্যাশিং সাপোর্ট

        // লিঙ্ক অ্যাপের ভেতরেই খোলার জন্য WebViewClient যুক্ত করা
        webView.setWebViewClient(new WebViewClient());

        // আপনার ওয়েবসাইটের URL লোড করুন
        webView.loadUrl("https://duck.ai/");
    }

    // ব্যাক বাটন চাপলে আগের পেজে যাওয়ার লজিক
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}