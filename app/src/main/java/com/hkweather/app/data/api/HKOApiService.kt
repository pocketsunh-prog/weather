package com.hkweather.app.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface HKOApiService {

    @GET("weather.php")
    suspend fun getCurrentWeather(
        @Query("dataType") dataType: String = "rhrread",
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("weather.php")
    suspend fun getForecast(
        @Query("dataType") dataType: String = "fnd",
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("weather.php")
    suspend fun getWarningInfo(
        @Query("dataType") dataType: String = "warningInfo",
        @Query("lang") lang: String = "en"
    ): ResponseBody

    @GET("weather.php")
    suspend fun getRainfall(
        @Query("dataType") dataType: String = "rainfall",
        @Query("lang") lang: String = "en"
    ): ResponseBody
}
