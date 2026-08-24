package com.hkweather.app.data.repository

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hkweather.app.data.api.HkoApiClient
import com.hkweather.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val apiClient: HkoApiClient
) {
    suspend fun getCurrentWeather(): Result<HKOWeatherResponse> = withContext(Dispatchers.IO) {
        runCatching { apiClient.getCurrentWeather().parseWeather() }
    }

    suspend fun getForecast(): Result<HKOForecastResponse> = withContext(Dispatchers.IO) {
        runCatching { apiClient.getForecast().parseForecast() }
    }

    suspend fun getWarningInfo(): Result<HKOWarningResponse> = withContext(Dispatchers.IO) {
        runCatching { apiClient.getWarningInfo().parseWarning() }
    }

    suspend fun getRainfall(): Result<HKOWeatherResponse> = withContext(Dispatchers.IO) {
        runCatching { apiClient.getRainfall().parseWeather() }
    }

    suspend fun getAllWeatherData(): Result<Triple<HKOWeatherResponse, HKOForecastResponse, HKOWarningResponse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                coroutineScope {
                    val weatherDeferred = async { apiClient.getCurrentWeather().parseWeather() }
                    val forecastDeferred = async { apiClient.getForecast().parseForecast() }
                    val warningDeferred = async { apiClient.getWarningInfo().parseWarning() }

                    Triple(
                        weatherDeferred.await(),
                        forecastDeferred.await(),
                        warningDeferred.await()
                    )
                }
            }
        }

    // ==================== Raw JSON Parsing ====================

    private fun String.parseWeather(): HKOWeatherResponse {
        val json = JsonParser.parseString(this).asJsonObject
        return HKOWeatherResponse(
            icon = json.get("icon"),
            uvIndex = json.get("uvindex"),
            rainfall = json.get("rainfall"),
            warningMessage = json.get("warningMessage")?.asString,
            temperature = json.get("temperature"),
            humidity = json.get("humidity"),
            wind = json.get("wind"),
            airTemp = json.get("airTemp"),
            forecastInfo = json.get("forecastInfo")
        )
    }

    private fun String.parseForecast(): HKOForecastResponse {
        val json = JsonParser.parseString(this).asJsonObject
        return HKOForecastResponse(
            generalSituation = json.get("generalSituation")?.asString ?: "",
            weatherForecast = json.get("weatherForecast"),
            updateTime = json.get("updateTime")?.asString ?: ""
        )
    }

    private fun String.parseWarning(): HKOWarningResponse {
        val json = JsonParser.parseString(this).asJsonObject
        return HKOWarningResponse(
            details = json.get("details"),
            contents = json.get("contents"),
            subtype = json.get("subtype")?.asString,
            warningStatementCode = json.get("warningStatementCode")?.asString
        )
    }

    // ==================== Safe Field Parsing ====================

    private fun parseIntList(element: JsonElement?): List<Int> {
        if (element == null || element.isJsonNull || !element.isJsonArray) return emptyList()
        return try { element.asJsonArray.mapNotNull { it.asInt } } catch (e: Exception) { emptyList() }
    }

    private fun parseUVIndex(element: JsonElement?): UVIndex {
        if (element == null || element.isJsonNull || !element.isJsonObject) return UVIndex()
        return try {
            val o = element.asJsonObject
            UVIndex(value = o.get("value")?.asInt ?: 0, desc = o.get("desc")?.asString ?: "")
        } catch (e: Exception) { UVIndex() }
    }

    private fun parseTemperature(element: JsonElement?): Temperature? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            val arr = o.getAsJsonArray("data") ?: return null
            Temperature(
                data = arr.mapNotNull { el ->
                    if (el.isJsonObject) {
                        val d = el.asJsonObject
                        TemperatureData(unit = d.get("unit")?.asString ?: "°C", value = d.get("value")?.asDouble ?: 0.0, place = d.get("place")?.asString ?: "")
                    } else null
                },
                recordTime = o.get("recordTime")?.asString ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun parseHumidity(element: JsonElement?): Humidity? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            val arr = o.getAsJsonArray("data") ?: return null
            Humidity(
                data = arr.mapNotNull { el ->
                    if (el.isJsonObject) {
                        val d = el.asJsonObject
                        HumidityData(unit = d.get("unit")?.asString ?: "%", value = d.get("value")?.asDouble ?: 0.0, place = d.get("place")?.asString ?: "")
                    } else null
                },
                recordTime = o.get("recordTime")?.asString ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun parseWind(element: JsonElement?): Wind? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            val arr = o.getAsJsonArray("data") ?: return null
            Wind(
                data = arr.mapNotNull { el ->
                    if (el.isJsonObject) {
                        val d = el.asJsonObject
                        WindData(speed = d.get("speed")?.asDouble, direction = d.get("direction")?.asString, place = d.get("place")?.asString, unit = d.get("unit")?.asString ?: "km/h")
                    } else null
                },
                recordTime = o.get("recordTime")?.asString ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun parseRainfall(element: JsonElement?): Rainfall? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            val arr = o.getAsJsonArray("data") ?: return null
            Rainfall(
                data = arr.mapNotNull { el ->
                    if (el.isJsonObject) {
                        val d = el.asJsonObject
                        RainfallData(unit = d.get("unit")?.asString ?: "mm", place = d.get("place")?.asString ?: "", max = d.get("max")?.asDouble ?: 0.0, main = d.get("main")?.asString ?: "", icon = parseIntList(d.get("icon")))
                    } else null
                },
                max = o.get("max")?.asDouble ?: 0.0, min = o.get("min")?.asDouble ?: 0.0, main = o.get("main")?.asString ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun parseAirTemp(element: JsonElement?): AirTemp? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            val arr = o.getAsJsonArray("data") ?: return null
            AirTemp(
                data = arr.mapNotNull { el ->
                    if (el.isJsonObject) {
                        val d = el.asJsonObject
                        AirTempData(unit = d.get("unit")?.asString ?: "°C", value = d.get("value")?.asDouble ?: 0.0, place = d.get("place")?.asString ?: "", recordTime = d.get("recordTime")?.asString ?: "")
                    } else null
                },
                recordTime = o.get("recordTime")?.asString ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun parseForecastInfo(element: JsonElement?): List<ForecastInfo> {
        if (element == null || element.isJsonNull || !element.isJsonArray) return emptyList()
        return try {
            element.asJsonArray.mapNotNull { el ->
                if (el.isJsonObject) {
                    val o = el.asJsonObject
                    ForecastInfo(
                        forecastDate = o.get("forecastDate")?.asString ?: "",
                        forecastWeather = o.get("forecastWeather")?.asString ?: "",
                        forecastWind = o.get("forecastWind")?.asString ?: "",
                        forecastMaxTemp = parseForecastTemp(o.get("forecastMaxtemp")),
                        forecastMinTemp = parseForecastTemp(o.get("forecastMintemp")),
                        forecastMaxRh = parseForecastHumidity(o.get("forecastMaxrh")),
                        forecastMinRh = parseForecastHumidity(o.get("forecastMinrh")),
                        forecastIcon = parseIntList(o.get("forecastIcon")),
                        psr = o.get("psr")?.asString ?: "",
                        weekDay = o.get("weekDay")?.asInt ?: 0
                    )
                } else null
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseForecastTemp(element: JsonElement?): ForecastTemp? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            ForecastTemp(value = o.get("value")?.asDouble ?: 0.0, unit = o.get("unit")?.asString ?: "°C")
        } catch (e: Exception) { null }
    }

    private fun parseForecastHumidity(element: JsonElement?): ForecastHumidity? {
        if (element == null || element.isJsonNull || !element.isJsonObject) return null
        return try {
            val o = element.asJsonObject
            ForecastHumidity(value = o.get("value")?.asDouble ?: 0.0, unit = o.get("unit")?.asString ?: "%")
        } catch (e: Exception) { null }
    }

    private fun parseWeatherForecast(element: JsonElement?): List<WeatherForecastItem> {
        if (element == null || element.isJsonNull || !element.isJsonArray) return emptyList()
        return try {
            element.asJsonArray.mapNotNull { el ->
                if (el.isJsonObject) {
                    val o = el.asJsonObject
                    WeatherForecastItem(
                        forecastDate = o.get("forecastDate")?.asString ?: "",
                        week = o.get("week")?.asString ?: "",
                        forecastWind = o.get("forecastWind")?.asString ?: "",
                        forecastWeather = o.get("forecastWeather")?.asString ?: "",
                        forecastMaxTemp = parseForecastTemp(o.get("forecastMaxtemp")),
                        forecastMinTemp = parseForecastTemp(o.get("forecastMintemp")),
                        forecastMaxRh = parseForecastHumidity(o.get("forecastMaxrh")),
                        forecastMinRh = parseForecastHumidity(o.get("forecastMinrh")),
                        forecastIcon = parseIntList(o.get("forecastIcon")),
                        psr = o.get("PSR")?.asString ?: ""
                    )
                } else null
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseWarningContents(element: JsonElement?): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        return try { if (element.isJsonArray) element.asJsonArray.mapNotNull { it.asString } else emptyList() } catch (e: Exception) { emptyList() }
    }

    private fun parseWarningDetails(element: JsonElement?): List<WarningDetail> {
        if (element == null || element.isJsonNull || !element.isJsonArray) return emptyList()
        return try {
            element.asJsonArray.mapNotNull { el ->
                if (el.isJsonObject) {
                    val o = el.asJsonObject
                    WarningDetail(
                        warningStatementCode = o.get("warningStatementCode")?.asString,
                        subtype = o.get("subtype")?.asString,
                        contents = parseWarningContents(o.get("contents")),
                        issueTime = o.get("issueTime")?.asString,
                        updateTime = o.get("updateTime")?.asString,
                        expiryTime = o.get("expiryTime")?.asString
                    )
                } else null
            }
        } catch (e: Exception) { emptyList() }
    }

    // ==================== UI Mapping ====================

    fun mapToCurrentWeatherDisplay(weather: HKOWeatherResponse): CurrentWeatherDisplay {
        val iconCodes = parseIntList(weather.icon)
        val uv = parseUVIndex(weather.uvIndex)
        val temp = parseTemperature(weather.temperature)
        val hum = parseHumidity(weather.humidity)
        val wind = parseWind(weather.wind)
        val rainfall = parseRainfall(weather.rainfall)
        val airTemp = parseAirTemp(weather.airTemp)
        val forecastInfoList = parseForecastInfo(weather.forecastInfo)

        val tempData = temp?.data?.firstOrNull()
        val humData = hum?.data?.firstOrNull()
        val windData = wind?.data?.firstOrNull()

        val rainfallPlaces = rainfall?.data?.filter { it.max > 0 }?.map { RainfallPlaceDisplay(it.place, it.max, it.unit) } ?: emptyList()
        val rainfallTotal = rainfall?.max ?: 0.0
        val rainfallUnit = rainfall?.data?.firstOrNull()?.unit ?: "mm"

        val windDir = when (windData?.direction?.lowercase()) {
            "n" -> "North"; "nne" -> "NNE"; "ne" -> "NE"; "ene" -> "ENE"
            "e" -> "East"; "ese" -> "ESE"; "se" -> "SE"; "sse" -> "SSE"
            "s" -> "South"; "ssw" -> "SSW"; "sw" -> "SW"; "wsw" -> "WSW"
            "w" -> "West"; "wnw" -> "WNW"; "nw" -> "NW"; "nnw" -> "NNW"
            else -> windData?.direction ?: "--"
        }

        val upcomingRain = if (rainfallTotal > 0) "Rain detected nearby" else "No rain expected"
        val firstForecast = forecastInfoList.firstOrNull()

        return CurrentWeatherDisplay(
            temperature = tempData?.value?.let { String.format("%.1f", it) } ?: "--",
            temperatureUnit = tempData?.unit ?: "°C",
            humidity = humData?.value?.let { String.format("%.1f", it) } ?: "--",
            humidityUnit = humData?.unit ?: "%",
            windSpeed = windData?.speed?.let { String.format("%.1f", it) } ?: "--",
            windSpeedUnit = windData?.unit ?: "km/h",
            windDirection = windDir,
            uvIndex = if (uv.value > 0) uv.value.toString() else "--",
            uvDescription = uv.desc,
            maxTemp = firstForecast?.forecastMaxTemp?.value?.let { String.format("%.1f", it) } ?: "--",
            minTemp = firstForecast?.forecastMinTemp?.value?.let { String.format("%.1f", it) } ?: "--",
            iconCode = iconCodes.firstOrNull() ?: 0,
            rainfall = String.format("%.1f", rainfallTotal),
            rainfallUnit = rainfallUnit,
            rainfallPlaces = rainfallPlaces,
            recordTime = temp?.recordTime ?: "",
            forecastWeather = firstForecast?.forecastWeather ?: "",
            forecastWind = firstForecast?.forecastWind ?: "",
            generalSituation = "",
            upcomingRain = upcomingRain,
            airTempStations = airTemp?.data?.map { AirTempDisplay(it.place, it.value, it.unit) } ?: emptyList()
        )
    }

    fun mapToForecastDays(forecast: HKOForecastResponse): List<ForecastDayDisplay> {
        return parseWeatherForecast(forecast.weatherForecast).map { item ->
            ForecastDayDisplay(
                date = formatForecastDate(item.forecastDate),
                week = item.week,
                weather = item.forecastWeather,
                wind = item.forecastWind,
                maxTemp = item.forecastMaxTemp?.value?.let { String.format("%.1f", it) } ?: "--",
                minTemp = item.forecastMinTemp?.value?.let { String.format("%.1f", it) } ?: "--",
                maxRh = item.forecastMaxRh?.value?.let { String.format("%.1f", it) } ?: "--",
                minRh = item.forecastMinRh?.value?.let { String.format("%.1f", it) } ?: "--",
                iconCode = item.forecastIcon.firstOrNull() ?: 0,
                psr = item.psr
            )
        }
    }

    fun mapToWarnings(response: HKOWarningResponse): List<String> {
        return parseWarningContents(response.contents) + parseWarningDetails(response.details).flatMap { it.contents }
    }

    fun mapToRainPrediction(weather: HKOWeatherResponse): String {
        val rainfall = parseRainfall(weather.rainfall)
        val rainfallTotal = rainfall?.max ?: 0.0
        val hasRainNearby = rainfall?.data?.any { it.max > 0 } == true
        val forecastInfoList = parseForecastInfo(weather.forecastInfo)
        val nextHourForecast = forecastInfoList.firstOrNull()?.forecastWeather?.lowercase() ?: ""

        return when {
            rainfallTotal > 10 -> "Heavy rain now — seek shelter"
            rainfallTotal > 0 -> "Light rain nearby (${String.format("%.1f", rainfallTotal)} mm)"
            hasRainNearby -> "Rain in surrounding areas"
            nextHourForecast.contains("rain") || nextHourForecast.contains("shower") -> "Rain expected within 30 min"
            nextHourForecast.contains("thunderstorm") -> "Thunderstorm expected — stay indoors"
            else -> "No rain expected in next 30 min"
        }
    }

    private fun formatForecastDate(dateStr: String): String {
        return if (dateStr.length == 8) "${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}" else dateStr
    }
}

// ==================== UI Models ====================

data class WeatherUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentWeather: CurrentWeatherDisplay? = null,
    val forecast: List<ForecastDayDisplay> = emptyList(),
    val warnings: List<String> = emptyList(),
    val currentLocation: LocationData = LocationData()
)

data class CurrentWeatherDisplay(
    val temperature: String = "--",
    val temperatureUnit: String = "°C",
    val humidity: String = "--",
    val humidityUnit: String = "%",
    val windSpeed: String = "--",
    val windSpeedUnit: String = "km/h",
    val windDirection: String = "--",
    val uvIndex: String = "--",
    val uvDescription: String = "",
    val maxTemp: String = "--",
    val minTemp: String = "--",
    val iconCode: Int = 0,
    val rainfall: String = "0.0",
    val rainfallUnit: String = "mm",
    val rainfallPlaces: List<RainfallPlaceDisplay> = emptyList(),
    val recordTime: String = "",
    val forecastWeather: String = "",
    val forecastWind: String = "",
    val generalSituation: String = "",
    val upcomingRain: String = "No rain expected",
    val airTempStations: List<AirTempDisplay> = emptyList()
)

data class RainfallPlaceDisplay(val place: String, val amount: Double, val unit: String)
data class AirTempDisplay(val place: String, val value: Double, val unit: String)

data class ForecastDayDisplay(
    val date: String, val week: String, val weather: String, val wind: String,
    val maxTemp: String, val minTemp: String, val maxRh: String, val minRh: String,
    val iconCode: Int, val psr: String
)

data class LocationData(val latitude: Double = 22.3193, val longitude: Double = 114.1694, val name: String = "Hong Kong")
