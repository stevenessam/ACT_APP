package com.example.act_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.d("ScreenReceiver", "Screen turned off")
            context.startForegroundService(Intent(context, WifiScanService::class.java))
        } else if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("ScreenReceiver", "Screen turned on")
            context.stopService(Intent(context, WifiScanService::class.java))
        }
    }
}
