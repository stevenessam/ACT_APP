package com.example.act_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

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
                    startActivity(Intent(this, WifiScanActivity::class.java))
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

        // Retrieve and log cached Wi-Fi networks
        retrieveAndLogCachedNetworks()
    }

    private fun retrieveAndLogCachedNetworks() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val savedSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()
        for (ssid in savedSsids) {
            Log.d("MainActivity", "Cached Network: $ssid")
        }
    }
}
class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun sendDataToAndroid(data: String) {
        Log.d("WebAppInterface", "Data received from WebView: $data")

        // Check if the received data is empty
        if (data.isEmpty()) {
            showAlertDialog("Safe Area", emptyList(), android.graphics.Color.GREEN)
            return
        }

        // Split the data using ":" as the delimiter
        val splitData = data.split(":")

        // Retrieve cached SSIDs
        val sharedPreferences = context.getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val savedSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()

        // Find matching SSIDs
        val matchingSsids = mutableListOf<String>()
        for (part in splitData) {
            val matches = savedSsids.filter { ssid -> ssid.contains(part, ignoreCase = true) }
            matchingSsids.addAll(matches)
        }

        // Determine alert message and color based on matches
        val alertMessage = if (matchingSsids.isNotEmpty()) {
            "Contamination Area"
        } else {
            "Safe Area"
        }

        val alertColor = if (matchingSsids.isNotEmpty()) {
            android.graphics.Color.RED
        } else {
            android.graphics.Color.GREEN
        }

        // Show the custom dialog
        showAlertDialog(alertMessage, matchingSsids, alertColor)
    }

    private fun showAlertDialog(alertMessage: String, matchingSsids: List<String>, alertColor: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null)
        val alertMessageView = dialogView.findViewById<TextView>(R.id.alertMessage)
        val matchingSsidsView = dialogView.findViewById<TextView>(R.id.matchingSsids)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        alertMessageView.text = alertMessage
        alertMessageView.setTextColor(alertColor)

        if (matchingSsids.isNotEmpty()) {
            matchingSsidsView.text = "Matching SSIDs: ${matchingSsids.joinToString(", ")}"
            matchingSsidsView.visibility = View.VISIBLE
        } else {
            matchingSsidsView.visibility = View.GONE
        }

        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = dialogBuilder.create()

        okButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
