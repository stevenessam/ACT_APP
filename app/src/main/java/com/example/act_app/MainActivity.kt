package com.example.act_app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
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

        // Ajouter l'interface JavaScript à la WebView
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.loadUrl("http://192.168.237.32/")  // Your website URL
    }
}

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun sendDataToAndroid(data: String) {
        // Traitez les données reçues ici
        Log.d("WebAppInterface", "Data received from WebView: $data")
    }
}
