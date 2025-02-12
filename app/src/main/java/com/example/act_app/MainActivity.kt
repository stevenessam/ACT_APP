package com.example.act_app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.navView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_main -> {
                    // Already in MainActivity, do nothing
                }
                R.id.nav_wifi_scan -> {
                    startActivity(android.content.Intent(this, WifiScanActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient()

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("https://act.gitlabpages.inria.fr/website/")
    }
}

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun sendDataToAndroid(data: String) {
        Log.d("WebAppInterface", "Data received from WebView: $data")
    }
}
