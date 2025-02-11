package com.example.act_app

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the ActionBar
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient()  // Makes the links open within the WebView

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true  // Enable JavaScript if needed
        webSettings.domStorageEnabled = true  // Enable DOM Storage if needed

        webView.loadUrl("https://act.gitlabpages.inria.fr/website/")  // Your website URL
    }
}
