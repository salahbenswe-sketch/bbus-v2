package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class BusTrackingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    companion object {
        fun startService(context: Context) {
            // Deprecated in Architecture v2 - FCM handles alerts directly from server.
        }

        fun stopService(context: Context) {
            // Deprecated in Architecture v2
        }
    }
}
