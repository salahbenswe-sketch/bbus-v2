package com.example.data.api

import com.example.data.model.DeviceDto
import com.example.data.model.PositionDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BusApiService {
    @GET("devices")
    suspend fun getDevices(): List<DeviceDto>

    @GET("positions")
    suspend fun getPositions(
        @Query("deviceId") deviceId: Long
    ): List<PositionDto>

    companion object {
        const val BASE_URL = "https://bus-medea.malimspotter.dz/api/"

        // Bus 1: 26/31 (New Valid Token until March 2027)
        const val TOKEN_BUS_1 = "RzBFAiEA6YxQHoCVbXtAAjL76JYNhuRoZkQiDuA7Fzc4G6Bs7ooCIAx6oyBDnmKPgkMwZxGJ3181eLSWmHPJfjO3Eqksly5zeyJ1Ijo2MzYsImUiOiIyMDI3LTAzLTAxVDAwOjAwOjAwLjAwMCswMDowMCJ9"
        const val TARGET_DEVICE_BUS_1 = "26/31"
        const val BUS_NAME_1 = "حافلة 1 (26/31)"

        // Bus 2: 26/26
        const val TOKEN_BUS_2 = "SDBGAiEAuI118lPLGYFAdcAKOdFW4amwkILR3dEde7nS8lAF2IoCIQD17hxsknJyX006Tr06pSGG9izBKtB949QFAdyePFKQOnsidSI6NjIwLCJlIjoiMjAyNy0wMy0wMVQwMDowMDowMC4wMDArMDA6MDAifQ"
        const val TARGET_DEVICE_BUS_2 = "26/26"
        const val BUS_NAME_2 = "حافلة 2 (26/26)"

        // Default Token set to TOKEN_BUS_1
        const val DEFAULT_TOKEN = TOKEN_BUS_1
        const val TARGET_DEVICE_NAME = TARGET_DEVICE_BUS_1

        fun create(token: String = DEFAULT_TOKEN): BusApiService {
            val authInterceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(newRequest)
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(BusApiService::class.java)
        }
    }
}
