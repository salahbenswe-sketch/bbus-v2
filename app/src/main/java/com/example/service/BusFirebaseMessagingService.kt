package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.AlertStation
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BusFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. التحقق الفوري مما إذا كان المستخدم قد أوقف التنبيهات العامة
        val prefs = getSharedPreferences("bus_tracker_prefs", MODE_PRIVATE)
        val isAlertEnabled = prefs.getBoolean("is_proximity_alert_enabled", true)
        if (!isAlertEnabled) {
            Log.d("FCM", "Alerts globally disabled by user preference. Silently ignoring push notification.")
            return
        }

        // 2. التحقق مما إذا كانت جميع المحطات معطلة
        val hasAnyStationEnabled = AlertStation.entries.any {
            prefs.getBoolean("station_alert_enabled_${it.name}", true)
        }
        if (!hasAnyStationEnabled) {
            Log.d("FCM", "All stations are disabled by user preference. Silently dropping push notification.")
            return
        }

        val data = remoteMessage.data

        // 3. استخراج المفتاح المباشر للمحطة والاتجاه من بيانات الفايربيز
        val stationKey = data["stationKey"] ?: remoteMessage.data["stationKey"]
        if (stationKey.isNullOrBlank()) {
            Log.e("FCM", "Missing or empty stationKey in FCM payload. Dropping notification.")
            return
        }

        // 4. المطابقة الدقيقة مع القائمة
        val station = try {
            AlertStation.valueOf(stationKey)
        } catch (e: Exception) {
            Log.e("FCM", "Unrecognized stationKey '$stationKey'. Dropping notification to prevent wrong-sound alerts.", e)
            return
        }

        // 5. التحقق الصارم من التفعيل الخاص بهذه المحطة تحديداً
        val isStationAlertEnabled = prefs.getBoolean("station_alert_enabled_${station.name}", true)
        if (!isStationAlertEnabled) {
            Log.d("FCM", "Alert specifically disabled for station '${station.title}' (${station.name}) by user preference. Dropping notification completely.")
            return
        }

        val busName = data["busName"] ?: "حافلة بني عطلي"
        val customTitle = data["title"]
            ?: remoteMessage.notification?.title
            ?: "🚌 الحافلة اقتربت من محطة ${station.title}!"
        val customBody = data["body"]
            ?: remoteMessage.notification?.body
            ?: "الحافلة متواجدة الآن في نطاق محطة ${station.title}"

        // 6. إطلاق الإشعار الفوري فوق شاشة القفل ومصحوباً بالصوت المخصص للمحطة والاتجاه
        triggerPushNotification(this, busName, station, customTitle, customBody)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token: $token")
    }

    private fun triggerPushNotification(
        context: Context,
        busName: String,
        station: AlertStation,
        title: String,
        body: String
    ) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // إيقاظ الشاشة فوراً عند وصول الإشعار في حالة الشاشة المغلقة
        try {
            val powerManager = context.getSystemService(POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "BusTracker::NotificationWakeLock"
            )
            wakeLock?.acquire(3000L)
        } catch (_: Exception) {}

        val soundUri = Uri.parse("android.resource://${context.packageName}/${station.soundResId}")

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                station.channelId,
                station.channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار الفايربيز الفوري لوصول الحافلة إلى محطة ${station.title}"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 600)
                setSound(soundUri, audioAttributes)
                setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            station.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, station.channelId)
            .setSmallIcon(R.drawable.ic_bus_marker)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 600))
            .build()

        notificationManager.notify(station.notificationId, notification)
    }
}
