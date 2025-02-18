package com.example.act_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class WifiScanService : Service() {

    private lateinit var wifiManager: WifiManager
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private val TAG = "WifiScanService"
    private val NOTIFICATION_ID = 1

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Create a notification channel for Android 8.0 and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wifi_scan_channel",
                "Wi-Fi Scan Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Start the service with a notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        Log.d(TAG, "WifiScanService: Service created and started in foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startScanning()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
        Log.d(TAG, "WifiScanService: Service destroyed")
    }

    private fun startScanning() {
        isScanning = true
        handler.post(object : Runnable {
            override fun run() {
                if (isScanning) {
                    Log.d(TAG, "WifiScanService: Starting Wi-Fi scan")
                    requestWifiPermissionsAndScan()
                    handler.postDelayed(this, 30000) // Scan every 30 seconds
                }
            }
        })
    }

    private fun stopScanning() {
        isScanning = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "WifiScanService: Stopped Wi-Fi scan")
    }

    private fun requestWifiPermissionsAndScan() {
        if (!isWifiEnabled()) {
            Toast.makeText(this, "Please enable Wi-Fi", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable Location", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanWifiNetworks()
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
            Log.d(TAG, "WifiScanService: Wi-Fi scan started")
            displayWifiNetworks()
        } else {
            Log.e(TAG, "WifiScanService: Failed to start Wi-Fi scan")
        }
    }

    private fun displayWifiNetworks() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val wifiList: List<ScanResult> = wifiManager.scanResults
            val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
            val prefix = sharedPreferences.getString("ssid_prefix", "") ?: ""
            val signalStrengthThreshold = sharedPreferences.getInt("signal_strength_threshold", -100)

            if (wifiList.isNotEmpty()) {
                val uniqueWifiNetworks = mutableMapOf<String, String>()
                val allNetworks = mutableListOf<String>()

                for (scanResult in wifiList) {
                    if (scanResult.SSID.isNotEmpty() && scanResult.BSSID.isNotEmpty() && scanResult.level >= signalStrengthThreshold) {
                        Log.d(TAG, "WifiScanService: Scanned Network - SSID=${scanResult.SSID}, BSSID=${scanResult.BSSID}, Level=${scanResult.level} dBm")
                        allNetworks.add("SSID: ${scanResult.SSID} \nBSSID: ${scanResult.BSSID}")
                        if (scanResult.SSID.startsWith(prefix, ignoreCase = true)) {
                            uniqueWifiNetworks[scanResult.BSSID] = "SSID: ${scanResult.SSID} \nBSSID: ${scanResult.BSSID}"
                        }
                    }
                }

                // Sort the allNetworks list alphabetically by SSID (case-insensitive)
                val sortedAllNetworks = allNetworks.sortedBy { it.substringAfter("SSID: ").substringBefore("\n").lowercase() }

                // Save all scanned networks to shared preferences
                val editor = sharedPreferences.edit()
                editor.putStringSet("scanned_networks", sortedAllNetworks.toSet())
                editor.apply()

                val sortedUniqueWifiNetworks = uniqueWifiNetworks.values.sortedBy {
                    it.substringAfter("SSID: ").substringBefore("\n").lowercase()
                }

                saveSsidsToCache(sortedUniqueWifiNetworks)

                // Send a broadcast to notify the activity of the update
                val intent = Intent("com.example.act_app.WIFI_UPDATE")
                sendBroadcast(intent)
            } else {
                Log.d(TAG, "WifiScanService: No Wi-Fi networks found")
            }
        }
    }



    private fun saveSsidsToCache(newSsidList: List<String>) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val existingSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()
        val updatedSsids = existingSsids.union(newSsidList.toSet())

        if (updatedSsids.size > existingSsids.size) {
            // New SSID added, send a notification, vibrate, and play sound
            showCachedNetworkNotification()
            vibrate()
            playNotificationSound()
        }

        editor.putStringSet("saved_ssids", updatedSsids)
        editor.apply()

        Log.d(TAG, "WifiScanService: Cached Networks: $updatedSsids")
    }

    private fun showCachedNetworkNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationBuilder = NotificationCompat.Builder(this, "wifi_scan_channel")
            .setContentTitle("New Network Cached")
            .setContentText("A new Wi-Fi network has been cached.")
            .setSmallIcon(R.drawable.act)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID + 1, notificationBuilder.build())
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use VibratorManager for API level 31 and above
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            // Use Vibrator for API levels below 31
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(500)
            }
        }
    }


    private fun playNotificationSound() {
        val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone: Ringtone = RingtoneManager.getRingtone(this, notificationUri)
        ringtone.play()
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, "wifi_scan_channel")
            .setContentTitle("Wi-Fi Scan Service")
            .setContentText("Scanning for Wi-Fi networks...")
            .setSmallIcon(R.drawable.act)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return notificationBuilder.build()
    }
}