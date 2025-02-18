package com.example.act_app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class ACTMapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_map_activity_main)

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
                R.id.nav_act_map -> {
                    // Already in ACTMapActivity, do nothing
                }
                R.id.nav_wifi_scan -> {
                    startActivity(Intent(this, WifiScanActivity::class.java))
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setGeolocationEnabled(true) // Enable geolocation

        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("https://act.gitlabpages.inria.fr/website/")

        // Retrieve and log cached Wi-Fi networks
        retrieveAndLogCachedNetworks()
    }

    private fun retrieveAndLogCachedNetworks() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val savedSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()
        for (ssid in savedSsids) {
            Log.d("ACTMapActivity", "Cached Network: $ssid")
        }
    }
}

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun sendDataToAndroid(data: String) {
        Log.d("WebAppInterface", "Data received from WebView: $data")

        // Check if the received data is empty
        if (data.isEmpty()) {
            showAlertDialog("Safe Zone", emptyList(), false)
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
            if (matches.isNotEmpty()) {
                matchingSsids.add(part) // Add the SSID from the data
            }
        }

        // Determine alert message based on matches
        val isContaminated = matchingSsids.isNotEmpty()

        // Show the custom dialog
        showAlertDialog("Zone Contaminated", matchingSsids, isContaminated)
    }

    private fun showAlertDialog(alertMessage: String, matchingSsids: List<String>, isContaminated: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null)
        val alertIconView = dialogView.findViewById<ImageView>(R.id.alertIcon)
        val alertMessageView = dialogView.findViewById<TextView>(R.id.alertMessage)
        val matchingSsidsLabelView = dialogView.findViewById<TextView>(R.id.matchingSsidsLabel)
        val matchingSsidsView = dialogView.findViewById<TextView>(R.id.matchingSsids)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        // Set the background drawable based on the alert type
        if (isContaminated) {
            dialogView.setBackgroundResource(R.drawable.dialog_background_contaminated)
            alertIconView.setImageResource(R.drawable.square_xmark_solid)
        } else {
            dialogView.setBackgroundResource(R.drawable.dialog_background_safe)
            alertIconView.setImageResource(R.drawable.square_check_solid)
        }

        // Set the message based on the alert type
        alertMessageView.text = alertMessage

        // Show matching SSIDs if contaminated
        if (isContaminated && matchingSsids.isNotEmpty()) {
            matchingSsidsLabelView.visibility = View.VISIBLE
            matchingSsidsView.text = matchingSsids.joinToString(", ")
            matchingSsidsView.visibility = View.VISIBLE
        } else {
            matchingSsidsLabelView.visibility = View.GONE
            matchingSsidsView.visibility = View.GONE
        }

        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = dialogBuilder.create()

        // Vibrate the phone when showing the alert
        vibratePhone()

        okButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun vibratePhone() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Deprecated in API 26
            vibrator.vibrate(500)
        }
    }
}
