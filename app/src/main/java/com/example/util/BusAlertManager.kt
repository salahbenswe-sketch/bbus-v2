package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class AlertStation(
    val title: String,
    val lat: Double,
    val lon: Double,
    val soundResId: Int,
    val channelId: String,
    val channelName: String,
    val notificationId: Int,
    val isReturnDirection: Boolean = false
) {
    EL_FORKANE(
        title = "مسجد الفرقان",
        lat = 36.264338,
        lon = 2.758888,
        soundResId = R.raw.alforqan,
        channelId = "bus_alert_el_forkane_v8",
        channelName = "تنبيه محطة مسجد الفرقان",
        notificationId = 1013
    ),
    BELHADRI(
        title = "بلحضري",
        lat = 36.273830,
        lon = 2.766270,
        soundResId = R.raw.belhadri,
        channelId = "bus_alert_belhadri_v8",
        channelName = "تنبيه محطة بلحضري",
        notificationId = 1011
    ),
    BELHADRI_RETURN(
        title = "بلحضري (عودة نحو المدية)",
        lat = 36.273830,
        lon = 2.766270,
        soundResId = R.raw.belhadri2,
        channelId = "bus_alert_belhadri_return_v8",
        channelName = "تنبيه محطة بلحضري (اتجاه المدية)",
        notificationId = 1012,
        isReturnDirection = true
    ),
    CHRACHRIA(
        title = "الشراشرية",
        lat = 36.287951,
        lon = 2.751746,
        soundResId = R.raw.chrachria,
        channelId = "bus_alert_chrachria_v8",
        channelName = "تنبيه محطة الشراشرية",
        notificationId = 1009
    ),
    CHRACHRIA_RETURN(
        title = "الشراشرية (عودة نحو المدية)",
        lat = 36.287951,
        lon = 2.751746,
        soundResId = R.raw.chrachria2,
        channelId = "bus_alert_chrachria_return_v8",
        channelName = "تنبيه محطة الشراشرية (اتجاه المدية)",
        notificationId = 1010,
        isReturnDirection = true
    ),
    DOURA(
        title = "الدورة",
        lat = 36.288847,
        lon = 2.749923,
        soundResId = R.raw.doura,
        channelId = "bus_alert_doura_v8",
        channelName = "تنبيه نقطة الدورة",
        notificationId = 1001
    ),
    BENI_ATLI(
        title = "بني عطلي",
        lat = 36.295291,
        lon = 2.740911,
        soundResId = R.raw.bni_attali,
        channelId = "bus_alert_bni_attali_v8",
        channelName = "تنبيه محطة بني عطلي",
        notificationId = 1002
    ),
    BENI_ATLI_RETURN(
        title = "بني عطلي (عودة نحو المدية)",
        lat = 36.295291,
        lon = 2.740911,
        soundResId = R.raw.bni_attali2,
        channelId = "bus_alert_bni_attali_return_v8",
        channelName = "تنبيه محطة بني عطلي (اتجاه المدية)",
        notificationId = 1007,
        isReturnDirection = true
    ),
    ADAHMI(
        title = "الدهمي",
        lat = 36.297794,
        lon = 2.735820,
        soundResId = R.raw.adahmi,
        channelId = "bus_alert_adahmi_v8",
        channelName = "تنبيه محطة الدهمي",
        notificationId = 1003
    ),
    ADAHMI_RETURN(
        title = "الدهمي (عودة نحو المدية)",
        lat = 36.297794,
        lon = 2.735820,
        soundResId = R.raw.adahmi2,
        channelId = "bus_alert_adahmi_return_v8",
        channelName = "تنبيه محطة الدهمي (اتجاه المدية)",
        notificationId = 1008,
        isReturnDirection = true
    ),
    BOUAMER(
        title = "بوعامر",
        lat = 36.300976,
        lon = 2.731803,
        soundResId = R.raw.bouamer,
        channelId = "bus_alert_bouamer_v8",
        channelName = "تنبيه محطة بوعامر (اتجاه بني عطلي)",
        notificationId = 1004
    ),
    BOUAMER_RETURN(
        title = "بوعامر (عودة نحو المدية)",
        lat = 36.300976,
        lon = 2.731803,
        soundResId = R.raw.bouamer2,
        channelId = "bus_alert_bouamer_return_v8",
        channelName = "تنبيه محطة بوعامر (اتجاه المدية)",
        notificationId = 1006,
        isReturnDirection = true
    ),
    EL_HANOUT(
        title = "الحانوت",
        lat = 36.304553,
        lon = 2.729345,
        soundResId = R.raw.elhanout,
        channelId = "bus_alert_el_hanout_v8",
        channelName = "تنبيه محطة الحانوت",
        notificationId = 1005
    )
}

object BusAlertManager {

    const val ALERT_RADIUS_METERS = 100.0
    private var mediaPlayer: MediaPlayer? = null

    /**
     * إنشاء وتسجيل جميع قنوات الإشعارات بالمأوى الصوتي مسبقاً في نظام أندرويد
     * حتى يتعرف عليها نظام الأندرويد ويعزف الصوت المخصص تلقائياً والتطبيق مغلق
     */
    fun createAllNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            for (station in AlertStation.entries) {
                val soundUri = Uri.parse("android.resource://${context.packageName}/${station.soundResId}")
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val channel = NotificationChannel(
                    station.channelId,
                    station.channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعار الوصول إلى محطة ${station.title}"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 600)
                    setSound(soundUri, audioAttributes)
                    setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                }
                notificationManager.createNotificationChannel(channel)
            }
            Log.d("BusAlertManager", "All station notification channels successfully pre-registered.")
        }
    }

    /**
     * حساب المسافة الدقيقة بين نقطتين بالأمتار (Haversine formula)
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // نصف قطر الأرض بالمتر
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * التحقق هل الحافلة تسير في الاتجاه من المدية نحو بني عطلي
     */
    fun isMovingTowardsBeniAtli(course: Double, speed: Double): Boolean {
        if (speed < 1.0 || course == 0.0) return true
        return resolveTripDirection(course, speed) != BusTripDirection.TO_MEDEA
    }

    /**
     * التحقق هل الحافلة تسير في الاتجاه من بني عطلي نحو المدية (اتجاه العودة / النزول)
     */
    fun isMovingTowardsMedea(course: Double, speed: Double): Boolean {
        if (speed < 1.0 || course == 0.0) return false
        return resolveTripDirection(course, speed) == BusTripDirection.TO_MEDEA
    }

    /**
     * تحديد اتجاه رحلة الحافلة بناءً على موقعها الجغرافي الحالي على الخط
     *
     * منطق الخط: الفرقان (بداية عند المدية) ← → الحانوت (نهاية عند بني عطلي)
     * - عند الفرقان: الحافلة تبدأ رحلتها نحو بني عطلي → TO_BENI_ATLI
     * - عند الحانوت: الحافلة تعود نحو المدية → TO_MEDEA
     * نستخدم إحداثيات الحافلة لمعرفة أين هي على الخط
     */
    fun resolveTripDirectionByLocation(
        lat: Double,
        lon: Double,
        course: Double,
        speedKmh: Double
    ): BusTripDirection {
        if (speedKmh < 0.8) return BusTripDirection.UNKNOWN

        // نقطة الفرقان (بداية الخط، جهة المدية)
        val forqanLat = 36.264338
        val forqanLon = 2.758888
        // نقطة الحانوت (نهاية الخط، جهة بني عطلي)
        val hanoutLat = 36.304553
        val hanoutLon = 2.729345
        // المنتصف التقريبي للخط (الشراشرية)
        val midLat = 36.287951
        val midLon = 2.751746

        val distToForqan = calculateDistanceMeters(lat, lon, forqanLat, forqanLon)
        val distToHanout = calculateDistanceMeters(lat, lon, hanoutLat, hanoutLon)

        // المنطقة عند الفرقان: الحافلة تبدأ الرحلة نحو بني عطلي
        if (distToForqan < 300) return BusTripDirection.TO_BENI_ATLI
        // المنطقة عند الحانوت: الحافلة ستعود نحو المدية
        if (distToHanout < 300) return BusTripDirection.TO_MEDEA

        // في باقي المناطق: نستخدم زاوية الدرجات للتحقق من الاتجاه
        // الطريق يمتد من الجنوب الشرقي (الفرقان) نحو الغرب الشمالي (الحانوت)
        // اتجاه بني عطلي: من الفرقان نحو الحانوت ≈ 270-330° (غرب-شمال غربي)
        // اتجاه المدية: من الحانوت نحو الفرقان ≈ 90-150° (شرق-جنوب شرقي)
        val normalizedCourse = (course % 360 + 360) % 360
        return if (normalizedCourse in 80.0..220.0) {
            BusTripDirection.TO_MEDEA
        } else {
            BusTripDirection.TO_BENI_ATLI
        }
    }

    fun resolveTripDirection(course: Double, speedKmh: Double): BusTripDirection {
        val normalizedCourse = (course % 360 + 360) % 360
        val headingUnknown = course == 0.0 || normalizedCourse < 1.0
        if (speedKmh < 0.8 && headingUnknown) {
            return BusTripDirection.UNKNOWN
        }
        return if (normalizedCourse in 80.0..220.0) {
            BusTripDirection.TO_MEDEA
        } else {
            BusTripDirection.TO_BENI_ATLI
        }
    }

    /**
     * إطلاق إشعار نظام حقيقي مع الصوت العالي والعرض المباشر
     */
    fun triggerTestSystemNotification(
        context: Context,
        station: AlertStation,
        title: String,
        body: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
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
                description = "إشعار وصول الحافلة إلى محطة ${station.title}"
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

    /**
     * تشغيل الصوت المسجل للتجربة اليدوية من الواجهة
     */
    @Synchronized
    fun triggerSoundAlert(context: Context, station: AlertStation) {
        try {
            stopSoundAlert()

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val afd = try {
                context.resources.openRawResourceFd(station.soundResId)
            } catch (_: Exception) {
                null
            }

            if (afd != null) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    setOnCompletionListener {
                        it.release()
                        if (mediaPlayer == it) {
                            mediaPlayer = null
                        }
                    }
                    setOnErrorListener { mp, _, _ ->
                        mp.release()
                        if (mediaPlayer == mp) {
                            mediaPlayer = null
                        }
                        true
                    }
                    start()
                }
                mediaPlayer = player
            } else {
                val player = MediaPlayer.create(context, station.soundResId)
                player?.let { p ->
                    p.setOnCompletionListener {
                        it.release()
                        if (mediaPlayer == it) {
                            mediaPlayer = null
                        }
                    }
                    p.start()
                    mediaPlayer = p
                }
            }

            triggerVibration(context)
        } catch (e: Exception) {
            Log.e("BusAlertManager", "Error playing sound alert for ${station.title}", e)
        }
    }

    @Synchronized
    fun stopSoundAlert() {
        try {
            mediaPlayer?.run {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 600), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 600), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 600), -1)
                }
            }
        } catch (_: Exception) {}
    }
}

enum class BusTripDirection(val label: String) {
    TO_BENI_ATLI("من المدية إلى بني عطلي"),
    TO_MEDEA("من بني عطلي إلى المدية"),
    UNKNOWN("الاتجاه غير محدد")
}
