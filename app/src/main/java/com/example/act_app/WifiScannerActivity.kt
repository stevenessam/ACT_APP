package com.example.act_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WifiScannerActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var ssidListView: ListView
    private lateinit var scanButton: Button

    private val ssidList = mutableListOf<String>()
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val wifiScanResults = wifiManager.scanResults
            ssidList.clear()
            for (scanResult in wifiScanResults) {
                ssidList.add(scanResult.SSID)
            }
            // Display the SSIDs in the ListView
            val adapter = ArrayAdapter(this@WifiScannerActivity, android.R.layout.simple_list_item_1, ssidList)
            ssidListView.adapter = adapter
            Toast.makeText(this@WifiScannerActivity, "Scan complete", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_scanner)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        ssidListView = findViewById(R.id.ssidListView)
        scanButton = findViewById(R.id.scanButton)

        // Register receiver to listen for Wi-Fi scan results
        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        scanButton.setOnClickListener {
            startWifiScan()
        }
    }

    private fun startWifiScan() {
        if (wifiManager.isWifiEnabled) {
            wifiManager.startScan()
        } else {
            Toast.makeText(this, "Wi-Fi is disabled. Enabling now...", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)  // Unregister the receiver
    }
}
