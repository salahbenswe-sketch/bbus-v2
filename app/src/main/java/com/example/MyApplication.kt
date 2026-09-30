package com.example

import android.app.Application
import android.util.Log
import com.example.util.BusAlertManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "FirebaseApp init error", e)
        }

        // تسجيل كافة قنوات الإشعارات الـ 8 بالأصوات المخصصة مسبقاً في نظام أندرويد
        try {
            BusAlertManager.createAllNotificationChannels(this)
        } catch (e: Exception) {
            Log.e("MyApplication", "Error creating notification channels", e)
        }

        // الاشتراك في القناة تلقائياً وطباعة التشخيص لمعرفة حالة الاتصال بـ Google FCM
        val prefs = getSharedPreferences("bus_tracker_prefs", MODE_PRIVATE)
        val isAlertEnabled = prefs.getBoolean("is_proximity_alert_enabled", true)
        if (isAlertEnabled) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MyApplication", "✅ FCM Device Token Generated: ${task.result}")
                    } else {
                        Log.e("MyApplication", "❌ Failed to get FCM Token.", task.exception)
                    }
                }

                FirebaseMessaging.getInstance().subscribeToTopic("bus_alerts").addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MyApplication", "✅ Successfully subscribed to FCM topic 'bus_alerts'")
                    } else {
                        Log.e("MyApplication", "❌ Failed to subscribe to 'bus_alerts' topic", task.exception)
                    }
                }
            } catch (e: Exception) {
                Log.e("MyApplication", "Error during FCM initialization", e)
            }
        }
    }
}
