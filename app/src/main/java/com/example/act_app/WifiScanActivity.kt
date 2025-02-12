package com.example.act_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiScanActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private val wifiPermissionCode = 100
    private lateinit var handler: Handler
    private var scanningToast: Toast? = null
    private lateinit var ssidPrefixInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_scan)

        // Initialize Wi-Fi Manager and Handler for recurring tasks
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        handler = Handler()

        // Initialize the EditText for SSID prefix
        ssidPrefixInput = findViewById(R.id.ssidPrefixInput)

        // Request Wi-Fi permissions and start scanning
        requestWifiPermissionsAndScan()

        // Repeated task to scan every 30 seconds and clear cache and list before scanning
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Show the "Scanning for Wi-Fi" message every time before scanning
                showScanningMessage()

                // Clear the previous cache and list before starting a new scan
                clearWifiCache()
                clearListView()

                // Start scanning for Wi-Fi networks again
                requestWifiPermissionsAndScan()

                // Continue scanning every 30 seconds
                handler.postDelayed(this, 30000)  // 30 seconds delay
            }
        }, 30000)  // Initial delay of 30 seconds
    }

    private fun showScanningMessage() {
        // Show the "Scanning for Wi-Fi" message continuously
        scanningToast?.cancel() // Cancel the previous toast if it exists
        scanningToast = Toast.makeText(this, "Scanning for Wi-Fi...", Toast.LENGTH_SHORT)
        scanningToast?.show()
    }

    private fun requestWifiPermissionsAndScan() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        // Ensure both permissions are granted for proper Wi-Fi scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanWifiNetworks()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions, wifiPermissionCode)
        }
    }

    private fun scanWifiNetworks() {
        // Directly start the Wi-Fi scan, without checking Wi-Fi state or showing any Toasts.
        if (wifiManager.startScan()) {
            // Wait for the scan results to be available (increase waiting time to 3-5 seconds)
            handler.postDelayed({
                displayWifiNetworks()
            }, 3000) // Increased delay to 3 seconds before displaying results
        }
    }

    private fun displayWifiNetworks() {
        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val wifiList: List<ScanResult> = wifiManager.scanResults

            if (wifiList.isNotEmpty()) {
                // Get the prefix entered by the user
                val prefix = ssidPrefixInput.text.toString().trim()

                // Use a map to ensure unique BSSID and remove duplicates based on BSSID
                val uniqueWifiNetworks = mutableMapOf<String, String>()

                // Loop through the scan results and add each network to the map using BSSID as the key
                for (scanResult in wifiList) {
                    if (scanResult.SSID.isNotEmpty() && scanResult.BSSID.isNotEmpty()) {
                        // Check if the SSID starts with the entered prefix
                        if (scanResult.SSID.startsWith(prefix, ignoreCase = true)) {
                            // Store SSID and BSSID in the map
                            uniqueWifiNetworks[scanResult.BSSID] = "SSID: ${scanResult.SSID} \nBSSID: ${scanResult.BSSID}"

                            // Save the network in cache if it matches the prefix
                            saveSsidsToCache(uniqueWifiNetworks.values.toList())
                        }
                    }
                }

                // Display the unique Wi-Fi list in ListView
                val listView = findViewById<ListView>(R.id.listView)
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, uniqueWifiNetworks.values.toList())
                listView.adapter = adapter

                // Show confirmation for saved networks
                showSavedNetworksConfirmation(uniqueWifiNetworks.values.toList())
            } else {
                Toast.makeText(this, "No Wi-Fi networks found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Location permission required to scan Wi-Fi networks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSsidsToCache(ssidList: List<String>) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Use a Set to automatically prevent duplicates
        editor.putStringSet("saved_ssids", ssidList.toSet())
        editor.apply()
    }

    private fun clearWifiCache() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()  // Clear the saved SSIDs
        editor.apply()
    }

    private fun clearListView() {
        // Clear the ListView by setting its adapter to null
        val listView = findViewById<ListView>(R.id.listView)
        listView.adapter = null
    }

    private fun showSavedNetworksConfirmation(savedNetworks: List<String>) {
        if (savedNetworks.isNotEmpty()) {
            Toast.makeText(this, "Networks Saved In Cache: ${savedNetworks.size}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No networks match the prefix", Toast.LENGTH_SHORT).show()
        }
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
