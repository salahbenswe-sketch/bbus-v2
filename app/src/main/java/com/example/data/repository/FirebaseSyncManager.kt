package com.example.data.repository

import android.content.Context
import com.example.data.model.BusTrackingData

/**
 * Deprecated in Architecture v2.
 * Server (`main.py`) directly polls Traccar API and evaluates distance.
 */
object FirebaseSyncManager {
    fun syncBusToFirebase(context: Context, busData: BusTrackingData) {}
}
