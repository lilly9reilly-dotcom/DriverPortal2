package com.driver.portal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            val prefs = context.getSharedPreferences("driver", Context.MODE_PRIVATE)

            val driverName = prefs.getString("driverName", "") ?: ""
            val carNumber = prefs.getString("carNumber", "") ?: ""

            if (driverName.isNotEmpty()) {

                val serviceIntent = Intent(context, LocationForegroundService::class.java)
                serviceIntent.putExtra("driverName", driverName)
                serviceIntent.putExtra("carNumber", carNumber)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}