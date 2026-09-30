package com.example.data.repository

import android.content.Context

/**
 * Deprecated in Architecture v2.
 * Server (`main.py`) evaluates bus positions and dispatches FCM messages directly.
 */
object FirebaseAlertEngine {
    fun startFirebaseRealtimeListener(context: Context) {}
    fun stopFirebaseRealtimeListener() {}
}
