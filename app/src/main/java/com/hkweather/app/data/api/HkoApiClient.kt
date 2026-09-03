package com.hkweather.app.data.api

import com.hkweather.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HkoApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl = BuildConfig.HKO_WEATHER_BASE_URL

    suspend fun getCurrentWeather(): String = fetch("weather.php?dataType=rhrread&lang=en")
    suspend fun getForecast(): String = fetch("weather.php?dataType=fnd&lang=en")
    suspend fun getWarningInfo(): String = fetch("weather.php?dataType=warningInfo&lang=en")
    suspend fun getRainfall(): String = fetch("weather.php?dataType=rainfall&lang=en")
    suspend fun getTyphoonTrack(): String = fetch("weather.php?dataType=tctrack&lang=en")

    private suspend fun fetch(endpoint: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .build()
        client.newCall(request).execute().use { response ->
            response.body?.string() ?: "{}"
        }
    }
}
