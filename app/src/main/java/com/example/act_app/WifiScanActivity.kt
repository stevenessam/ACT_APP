package com.example.act_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle

class WifiScanActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var locationManager: LocationManager
    private val wifiPermissionCode = 100
    private val locationRequestCode = 101
    private lateinit var handler: Handler
    private lateinit var ssidPrefixInput: EditText
    private lateinit var allNetworksListView: ListView
    private lateinit var cachedNetworksListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var scanningMessage: TextView
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_scan)

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
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.nav_wifi_scan -> {
                    // Already in WifiScanActivity, do nothing
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        handler = Handler()
        ssidPrefixInput = findViewById(R.id.ssidPrefixInput)
        allNetworksListView = findViewById(R.id.allNetworksListView)
        cachedNetworksListView = findViewById(R.id.cachedNetworksListView)
        progressBar = findViewById(R.id.progressBar)
        scanningMessage = findViewById(R.id.scanningMessage)

        val clearCacheButton: Button = findViewById(R.id.clearCacheButton)
        clearCacheButton.setOnClickListener {
            clearWifiCache()
            updateCachedNetworksListView()
            Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show()
        }

        // Load cached networks and SSID prefix when the activity is created
        updateCachedNetworksListView()
        loadSsidPrefix()

        // Save SSID prefix whenever it changes
        ssidPrefixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                saveSsidPrefix(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        requestWifiPermissionsAndScan()
    }

    override fun onResume() {
        super.onResume()
        // Restart scanning when the activity is resumed
        if (!isScanning) {
            startScanning()
        }
        // Optionally, refresh the cached networks list when the activity is resumed
        updateCachedNetworksListView()
    }

    override fun onPause() {
        super.onPause()
        // Stop scanning when the activity is paused
        stopScanning()
    }

    private fun startScanning() {
        isScanning = true
        progressBar.visibility = View.VISIBLE
        scanningMessage.visibility = View.VISIBLE
        handler.post(object : Runnable {
            override fun run() {
                requestWifiPermissionsAndScan()
                if (isScanning) {
                    handler.post(this) // Continuously scan
                }
            }
        })
    }

    private fun stopScanning() {
        isScanning = false
        handler.removeCallbacksAndMessages(null)
        progressBar.visibility = View.INVISIBLE
        scanningMessage.visibility = View.INVISIBLE
    }

    private fun requestWifiPermissionsAndScan() {
        if (!isWifiEnabled()) {
            Toast.makeText(this, "Please enable Wi-Fi", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable Location", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanWifiNetworks()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, wifiPermissionCode)
        }
    }

    private fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun scanWifiNetworks() {
        if (wifiManager.startScan()) {
            displayWifiNetworks() // Call immediately after starting the scan
        }
    }

    private fun displayWifiNetworks() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val wifiList: List<ScanResult> = wifiManager.scanResults

            if (wifiList.isNotEmpty()) {
                val allNetworks = mutableListOf<String>()
                val uniqueWifiNetworks = mutableMapOf<String, String>()

                for (scanResult in wifiList) {
                    if (scanResult.SSID.isNotEmpty() && scanResult.BSSID.isNotEmpty()) {
                        allNetworks.add("SSID: ${scanResult.SSID} \nBSSID: ${scanResult.BSSID}")
                        val prefix = ssidPrefixInput.text.toString().trim()
                        if (scanResult.SSID.startsWith(prefix, ignoreCase = true)) {
                            uniqueWifiNetworks[scanResult.BSSID] = "SSID: ${scanResult.SSID} \nBSSID: ${scanResult.BSSID}"
                        }
                    }
                }

                // Use the custom layout for the ArrayAdapter
                val adapterAllNetworks = ArrayAdapter(this, R.layout.list_item_custom, allNetworks)
                allNetworksListView.adapter = adapterAllNetworks

                saveSsidsToCache(uniqueWifiNetworks.values.toList())
                updateCachedNetworksListView()
            } else {
                Toast.makeText(this, "No Wi-Fi networks found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Location permission required to scan Wi-Fi networks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCachedNetworksListView() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val savedSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()
        // Use the custom layout for the ArrayAdapter
        val adapterCachedNetworks = ArrayAdapter(this, R.layout.list_item_custom, savedSsids.toList())
        cachedNetworksListView.adapter = adapterCachedNetworks
    }

    private fun saveSsidsToCache(newSsidList: List<String>) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val existingSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()
        val updatedSsids = existingSsids.union(newSsidList.toSet())
        editor.putStringSet("saved_ssids", updatedSsids)
        editor.apply()
    }

    private fun clearWifiCache() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("saved_ssids") // Only remove the cached SSIDs
        editor.apply()
    }

    private fun saveSsidPrefix(prefix: String) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ssid_prefix", prefix)
        editor.apply()
    }

    private fun loadSsidPrefix() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val prefix = sharedPreferences.getString("ssid_prefix", "") ?: ""
        ssidPrefixInput.setText(prefix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == wifiPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifiNetworks()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
