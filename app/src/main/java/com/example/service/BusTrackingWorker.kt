package com.example.service

import android.content.Context

/**
 * Deprecated in Architecture v2.
 * Local watchdog is no longer required because FCM push delivery is handled by Google Play Services.
 */
object BusTrackingWorker {
    fun scheduleWatchdog(context: Context) {}
    fun cancelWatchdog(context: Context) {}
}
