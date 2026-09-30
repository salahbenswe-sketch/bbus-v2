package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String?,
    @Json(name = "uniqueId") val uniqueId: String?,
    @Json(name = "status") val status: String? = "unknown",
    @Json(name = "lastUpdate") val lastUpdate: String? = null,
    @Json(name = "positionId") val positionId: Long? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "disabled") val disabled: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class PositionDto(
    @Json(name = "id") val id: Long,
    @Json(name = "deviceId") val deviceId: Long,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "speed") val speed: Double = 0.0,
    @Json(name = "course") val course: Double = 0.0,
    @Json(name = "altitude") val altitude: Double = 0.0,
    @Json(name = "fixTime") val fixTime: String? = null,
    @Json(name = "serverTime") val serverTime: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "valid") val valid: Boolean = true,
    @Json(name = "attributes") val attributes: Map<String, Any?>? = null
)

/**
 * Clean UI representation of the Bus State
 */
data class BusTrackingData(
    val busIndex: Int = 1,
    val deviceId: Long,
    val busName: String,
    val uniqueId: String,
    val phone: String?,
    val isOnline: Boolean,
    val rawStatus: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val courseDegrees: Double,
    val altitudeMeters: Double,
    val fixTimeIso: String?,
    val formattedFixTime: String,
    val fuelPercent: Double?,
    val isIgnitionOn: Boolean,
    val distanceTodayKm: Double,
    val totalDistanceKm: Double,
    val isMotion: Boolean
)
