package com.example.data.repository

import com.example.data.api.BusApiService
import com.example.data.model.BusTrackingData
import com.example.data.model.DeviceDto
import com.example.data.model.PositionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BusRepository(
    private val apiServiceBus: BusApiService = BusApiService.create(BusApiService.TOKEN_BUS_1)
) {
    private var cachedDeviceId: Long? = null

    suspend fun fetchBusData(busIndex: Int = 1): Result<BusTrackingData> = withContext(Dispatchers.IO) {
        fetchBusInternal()
    }

    suspend fun fetchBus1(): Result<BusTrackingData> = withContext(Dispatchers.IO) {
        fetchBusInternal()
    }

    private suspend fun fetchBusInternal(): Result<BusTrackingData> {
        return try {
            val devices = apiServiceBus.getDevices()
            val device = if (cachedDeviceId != null) {
                devices.find { it.id == cachedDeviceId }
            } else {
                val target = devices.find { it.name == BusApiService.TARGET_DEVICE_BUS_1 } ?: devices.firstOrNull()
                if (target != null) {
                    cachedDeviceId = target.id
                }
                target
            } ?: return Result.failure(Exception("بيانات الحافلة غير متوفرة"))

            val positions = apiServiceBus.getPositions(device.id)
            val latestPosition = positions.firstOrNull()
                ?: return Result.failure(Exception("لا توجد بيانات موقع للحافلة"))

            mapToBusTrackingData(
                busIndex = 1,
                busName = BusApiService.BUS_NAME_1,
                device = device,
                latestPosition = latestPosition
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToBusTrackingData(
        busIndex: Int,
        busName: String,
        device: DeviceDto,
        latestPosition: PositionDto
    ): Result<BusTrackingData> {
        val attrs = latestPosition.attributes ?: emptyMap()

        val fuelVal = (attrs["fuel1"] as? Number)?.toDouble()
        val isIgnition = (attrs["ignition"] as? Boolean) == true
        val distTodayMeters = (attrs["distance"] as? Number)?.toDouble() ?: 0.0
        val totalDistMeters = (attrs["totalDistance"] as? Number)?.toDouble() ?: 0.0
        val isMotion = (attrs["motion"] as? Boolean) ?: (latestPosition.speed > 0.5)

        val formattedTime = formatTimestamp(latestPosition.fixTime)
        val isOnline = device.status?.lowercase(Locale.ROOT) == "online"

        val uiData = BusTrackingData(
            busIndex = busIndex,
            deviceId = device.id,
            busName = busName,
            uniqueId = device.uniqueId ?: "",
            phone = device.phone,
            isOnline = isOnline,
            rawStatus = device.status ?: "unknown",
            latitude = latestPosition.latitude,
            longitude = latestPosition.longitude,
            speedKmh = latestPosition.speed,
            courseDegrees = latestPosition.course,
            altitudeMeters = latestPosition.altitude,
            fixTimeIso = latestPosition.fixTime,
            formattedFixTime = formattedTime,
            fuelPercent = fuelVal,
            isIgnitionOn = isIgnition,
            distanceTodayKm = distTodayMeters / 1000.0,
            totalDistanceKm = totalDistMeters / 1000.0,
            isMotion = isMotion
        )

        return Result.success(uiData)
    }

    private fun formatTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "غير محدد"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString) ?: Date()
            val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale("ar", "DZ")).apply {
                timeZone = TimeZone.getDefault()
            }
            formatter.format(date)
        } catch (_: Exception) {
            try {
                val fallbackParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val date = fallbackParser.parse(isoString) ?: Date()
                val formatter = SimpleDateFormat("HH:mm:ss yyyy/MM/dd", Locale("ar", "DZ"))
                formatter.format(date)
            } catch (_: Exception) {
                isoString
            }
        }
    }
}
