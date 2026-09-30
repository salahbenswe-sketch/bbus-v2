package com.example.util

import com.example.data.model.BusTrackingData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Bus3Simulator {
    // النقاط الرئيسية لمسار الذهاب والعودة
    private val keyWaypoints = listOf(
        // === رحلة الذهاب (صعوداً نحو الحانوت) ===
        Pair(36.287191, 2.753279), // نقطة الانطلاق
        Pair(36.288847, 2.749923), // 📍 نقطة الدورة
        Pair(36.291200, 2.746500),
        Pair(36.293200, 2.743500),
        Pair(36.295291, 2.740911), // 📍 محطة بني عطلي
        Pair(36.296500, 2.738000),
        Pair(36.297794, 2.735820), // 📍 محطة الدهمي
        Pair(36.299400, 2.733500),
        Pair(36.300976, 2.731803), // 📍 محطة بوعامر
        Pair(36.302800, 2.730500),
        Pair(36.304553, 2.729345), // 📍 محطة الحانوت (المحطة الأخيرة)

        // === رحلة العودة (نزولاً نحو نقطة البداية) ===
        Pair(36.302800, 2.730500),
        Pair(36.300976, 2.731803), // 📍 محطة بوعامر (نزول)
        Pair(36.299400, 2.733500),
        Pair(36.297794, 2.735820), // 📍 محطة الدهمي (نزول)
        Pair(36.296500, 2.738000),
        Pair(36.295291, 2.740911), // 📍 محطة بني عطلي (نزول)
        Pair(36.293200, 2.743500),
        Pair(36.291200, 2.746500),
        Pair(36.288847, 2.749923), // 📍 نقطة الدورة (نزول)
        Pair(36.287191, 2.753279)  // العودة لنقطة الانطلاق
    )

    // تقطيع المسار إلى خطوات صغيرة بمسافة 8.33 متراً (سرعة 15 كم/س عند تحديث كل ثانيتين)
    private val fineWaypoints: List<Pair<Double, Double>> by lazy {
        generateFinePath(keyWaypoints, stepMeters = 8.33)
    }

    private var currentIndex = 0

    @Synchronized
    fun resetSimulation() {
        currentIndex = 0
    }

    @Synchronized
    fun getNextPosition(): BusTrackingData {
        val points = fineWaypoints
        val currentPoint = points[currentIndex]
        val nextIndex = (currentIndex + 1) % points.size
        val nextPoint = points[nextIndex]

        val dLon = Math.toRadians(nextPoint.second - currentPoint.second)
        val lat1 = Math.toRadians(currentPoint.first)
        val lat2 = Math.toRadians(nextPoint.first)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearingRad = atan2(y, x)
        val courseDegrees = (Math.toDegrees(bearingRad) + 360) % 360

        val formattedTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale("ar", "DZ")).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date())

        val busData = BusTrackingData(
            busIndex = 3,
            deviceId = 333333L,
            busName = "حافلة 3 (محاكاة)",
            uniqueId = "SIM-BUS-3",
            phone = "+213 550 000 003",
            isOnline = true,
            rawStatus = "online",
            latitude = currentPoint.first,
            longitude = currentPoint.second,
            speedKmh = 15.0,
            courseDegrees = courseDegrees,
            altitudeMeters = 720.0,
            fixTimeIso = null,
            formattedFixTime = formattedTime,
            fuelPercent = 90.0,
            isIgnitionOn = true,
            distanceTodayKm = 8.4,
            totalDistanceKm = 1250.0,
            isMotion = true
        )

        currentIndex = nextIndex
        return busData
    }

    private fun generateFinePath(
        waypoints: List<Pair<Double, Double>>,
        stepMeters: Double
    ): List<Pair<Double, Double>> {
        val fineList = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]
            val dist = calculateDistance(start.first, start.second, end.first, end.second)
            val steps = (dist / stepMeters).toInt().coerceAtLeast(1)

            for (s in 0 until steps) {
                val fraction = s.toDouble() / steps
                val lat = start.first + (end.first - start.first) * fraction
                val lon = start.second + (end.second - start.second) * fraction
                fineList.add(Pair(lat, lon))
            }
        }
        fineList.add(waypoints.last())
        return fineList
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
