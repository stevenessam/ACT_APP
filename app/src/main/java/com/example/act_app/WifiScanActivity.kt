package com.example.act_app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog

class WifiScanActivity : AppCompatActivity() {

    private lateinit var allNetworksListView: ListView
    private lateinit var cachedNetworksListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var scanningMessage: TextView
    private lateinit var wifiUpdateReceiver: BroadcastReceiver

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )

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
                R.id.nav_act_map -> {
                    startActivity(Intent(this, ACTMapActivity::class.java))
                }
                R.id.nav_wifi_scan -> {
                    // Already in WifiScanActivity, do nothing
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        allNetworksListView = findViewById(R.id.allNetworksListView)
        cachedNetworksListView = findViewById(R.id.cachedNetworksListView)
        progressBar = findViewById(R.id.progressBar)
        scanningMessage = findViewById(R.id.scanningMessage)

        // Load cached networks when the activity is created
        updateCachedNetworksListView()

        // Register the BroadcastReceiver to listen for updates from the service
        wifiUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Update the UI with the latest scanned networks
                updateScannedNetworksListView()
                updateCachedNetworksListView()
            }
        }

        val intentFilter = IntentFilter("com.example.act_app.WIFI_UPDATE")
        registerReceiver(wifiUpdateReceiver, intentFilter, RECEIVER_EXPORTED)

        // Check and request permissions
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            // Start the Wi-Fi scan service
            // startWifiScanService()
        }
    }

    override fun onResume() {
        super.onResume()
        // Update the UI with the latest scanned networks
        updateScannedNetworksListView()
        updateCachedNetworksListView()
    }

    override fun onPause() {
        super.onPause()
        // No need to stop the service here as it should run in the background
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the BroadcastReceiver
        unregisterReceiver(wifiUpdateReceiver)
        // Stop the service when the activity is destroyed
        stopWifiScanService()
    }

    private fun updateScannedNetworksListView() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val scannedNetworks = sharedPreferences.getStringSet("scanned_networks", emptySet()) ?: emptySet()
        val sortedScannedNetworks = scannedNetworks.toList().sortedByDescending {
            extractSignalStrength(it)
        }
        val adapterAllNetworks = CustomArrayAdapter(this, R.layout.list_item_custom, sortedScannedNetworks)
        allNetworksListView.adapter = adapterAllNetworks
    }


    private fun updateCachedNetworksListView() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val savedSsids = sharedPreferences.getStringSet("saved_ssids", emptySet()) ?: emptySet()
        val sortedSavedSsids = savedSsids.toList().sortedByDescending {
            extractSignalStrength(it)
        }
        val adapterCachedNetworks = CustomArrayAdapter(this, R.layout.list_item_custom, sortedSavedSsids)
        cachedNetworksListView.adapter = adapterCachedNetworks
    }

    private fun extractSignalStrength(networkName: String): Int {
        val levelPrefix = "Level: "
        val levelStart = networkName.indexOf(levelPrefix)
        if (levelStart != -1) {
            val levelEnd = networkName.indexOf(" dBm", levelStart)
            if (levelEnd != -1) {
                val levelString = networkName.substring(levelStart + levelPrefix.length, levelEnd)
                return levelString.toIntOrNull() ?: Int.MIN_VALUE
            }
        }
        return Int.MIN_VALUE
    }

    class CustomArrayAdapter(context: Context, private val resource: Int, objects: List<String>) :
        ArrayAdapter<String>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
            val networkName = getItem(position)
            val textView = view.findViewById<TextView>(R.id.networkName)
            val imageView = view.findViewById<ImageView>(R.id.wifiIcon)

            // Use the Elvis operator to provide a default value if networkName is null
            textView.text = networkName ?: "Unknown Network"

            // Assuming the signal strength is part of the networkName string
            val signalStrength = extractSignalStrength(networkName)
            val iconResId = when {
                signalStrength >= -50 -> R.drawable.wifi_solid_green
                signalStrength >= -70 -> R.drawable.wifi_solid_yellow
                else -> R.drawable.wifi_solid
            }
            imageView.setImageResource(iconResId)

            return view
        }

        private fun extractSignalStrength(networkName: String?): Int {
            // Extract the signal strength from the networkName string
            // Assuming the format is "SSID: <SSID> \nBSSID: <BSSID> \nLevel: <signalStrength> dBm"
            if (networkName == null) return Int.MIN_VALUE

            val levelPrefix = "Level: "
            val levelStart = networkName.indexOf(levelPrefix)
            if (levelStart != -1) {
                val levelEnd = networkName.indexOf(" dBm", levelStart)
                if (levelEnd != -1) {
                    val levelString = networkName.substring(levelStart + levelPrefix.length, levelEnd)
                    return levelString.toIntOrNull() ?: Int.MIN_VALUE
                }
            }
            return Int.MIN_VALUE
        }
    }


    private fun clearWifiCache() {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("saved_ssids") // Only remove the cached SSIDs
        editor.apply()
    }

    private fun startWifiScanService() {
        val intent = Intent(this, WifiScanService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopWifiScanService() {
        val intent = Intent(this, WifiScanService::class.java)
        stopService(intent)
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    private fun getScanningDelay(): Int {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("scanning_delay", 30)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, start the Wi-Fi scan service
                startWifiScanService()
            } else {
                // Permissions denied, show a message to the user
                Toast.makeText(this, "Permissions are required to scan Wi-Fi networks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val delayInput = dialogView.findViewById<EditText>(R.id.delayInput)
        val ssidPrefixInputDialog = dialogView.findViewById<EditText>(R.id.ssidPrefixInput)
        val signalStrengthSeekBar = dialogView.findViewById<SeekBar>(R.id.signalStrengthSeekBar)
        val signalStrengthValue = dialogView.findViewById<TextView>(R.id.signalStrengthValue)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val clearCacheButton = dialogView.findViewById<Button>(R.id.clearCacheButton)

        // Load the last entered delay or set default to 30
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val lastDelay = getScanningDelay()
        delayInput.setText(lastDelay.toString())

        // Load the last SSID prefix
        val prefix = sharedPreferences.getString("ssid_prefix", "") ?: ""
        ssidPrefixInputDialog.setText(prefix)

        // Load the last signal strength threshold
        val initialThreshold = getSignalStrengthThreshold()
        signalStrengthSeekBar.progress = initialThreshold + 100
        signalStrengthValue.text = "$initialThreshold dBm"

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Settings")
            .setView(dialogView)
            .create()

        signalStrengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val threshold = progress - 100
                signalStrengthValue.text = "$threshold dBm"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener {
            val delay = delayInput.text.toString().toIntOrNull() ?: 30
            if (delay >= 20) {
                saveScanningDelay(delay)
                val threshold = signalStrengthSeekBar.progress - 100
                saveSignalStrengthThreshold(threshold)
                val newPrefix = ssidPrefixInputDialog.text.toString()
                saveSsidPrefix(newPrefix)
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Delay must be at least 20 seconds", Toast.LENGTH_SHORT).show()
            }
        }

        clearCacheButton.setOnClickListener {
            clearWifiCache()
            updateCachedNetworksListView()
            Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveScanningDelay(delay: Int) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("scanning_delay", delay)
        editor.apply()
    }

    private fun getSignalStrengthThreshold(): Int {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("signal_strength_threshold", -100)
    }

    private fun saveSignalStrengthThreshold(threshold: Int) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("signal_strength_threshold", threshold)
        editor.apply()
    }

    private fun saveSsidPrefix(prefix: String) {
        val sharedPreferences = getSharedPreferences("wifi_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ssid_prefix", prefix)
        editor.apply()
    }
}
