package com.hkweather.app.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ==================== API Response Wrappers (use JsonElement for safety) ====================

data class HKOWeatherResponse(
    @SerializedName("icon") val icon: JsonElement? = null,
    @SerializedName("uvindex") val uvIndex: JsonElement? = null,
    @SerializedName("rainfall") val rainfall: JsonElement? = null,
    @SerializedName("warningMessage") val warningMessage: String? = null,
    @SerializedName("tcmessage") val tcMessage: String? = null,
    @SerializedName("mintempFrom00To09") val minTempFrom00To09: String? = null,
    @SerializedName("rainfallFrom00To14") val rainfallFrom00To14: String? = null,
    @SerializedName("rainfallLastMonth") val rainfallLastMonth: String? = null,
    @SerializedName("rainfallJanuary") val rainfallJanuary: String? = null,
    @SerializedName("normalFrom01To14") val normalFrom01To14: String? = null,
    @SerializedName("temperature") val temperature: JsonElement? = null,
    @SerializedName("humidity") val humidity: JsonElement? = null,
    @SerializedName("wind") val wind: JsonElement? = null,
    @SerializedName("airTemp") val airTemp: JsonElement? = null,
    @SerializedName("forecastInfo") val forecastInfo: JsonElement? = null
)

data class HKOForecastResponse(
    @SerializedName("generalSituation") val generalSituation: String = "",
    @SerializedName("weatherForecast") val weatherForecast: JsonElement? = null,
    @SerializedName("updateTime") val updateTime: String = "",
    @SerializedName("seaSurfaceTemperature") val seaSurfaceTemperature: JsonElement? = null,
    @SerializedName("soilTemperature") val soilTemperature: JsonElement? = null
)

data class HKOWarningResponse(
    @SerializedName("details") val details: JsonElement? = null,
    @SerializedName("contents") val contents: JsonElement? = null,
    @SerializedName("subtype") val subtype: String? = null,
    @SerializedName("warningStatementCode") val warningStatementCode: String? = null
)

// ==================== Data Models (parsed manually from JsonElement) ====================

data class UVIndex(
    val value: Int = 0,
    val desc: String = ""
)

data class Rainfall(
    val data: List<RainfallData> = emptyList(),
    val max: Double = 0.0,
    val min: Double = 0.0,
    val main: String = ""
)

data class RainfallData(
    val unit: String = "mm",
    val place: String = "",
    val max: Double = 0.0,
    val main: String = "",
    val icon: List<Int> = emptyList()
)

data class Temperature(
    val data: List<TemperatureData> = emptyList(),
    val recordTime: String = ""
)

data class TemperatureData(
    val unit: String = "°C",
    val value: Double = 0.0,
    val place: String = ""
)

data class Humidity(
    val data: List<HumidityData> = emptyList(),
    val recordTime: String = ""
)

data class HumidityData(
    val unit: String = "%",
    val value: Double = 0.0,
    val place: String = ""
)

data class Wind(
    val data: List<WindData> = emptyList(),
    val recordTime: String = ""
)

data class WindData(
    val speed: Double? = null,
    val direction: String? = null,
    val place: String? = null,
    val unit: String? = "km/h"
)

data class AirTemp(
    val data: List<AirTempData> = emptyList(),
    val recordTime: String = ""
)

data class AirTempData(
    val unit: String = "°C",
    val value: Double = 0.0,
    val place: String = "",
    val recordTime: String = ""
)

data class ForecastInfo(
    val forecastDate: String = "",
    val forecastWeather: String = "",
    val forecastWind: String = "",
    val forecastMaxTemp: ForecastTemp? = null,
    val forecastMinTemp: ForecastTemp? = null,
    val forecastMaxRh: ForecastHumidity? = null,
    val forecastMinRh: ForecastHumidity? = null,
    val forecastIcon: List<Int> = emptyList(),
    val psr: String = "",
    val weekDay: Int = 0
)

data class ForecastTemp(
    val value: Double = 0.0,
    val unit: String = "°C"
)

data class ForecastHumidity(
    val value: Double = 0.0,
    val unit: String = "%"
)

data class WeatherForecastItem(
    val forecastDate: String = "",
    val week: String = "",
    val forecastWind: String = "",
    val forecastWeather: String = "",
    val forecastMaxTemp: ForecastTemp? = null,
    val forecastMinTemp: ForecastTemp? = null,
    val forecastMaxRh: ForecastHumidity? = null,
    val forecastMinRh: ForecastHumidity? = null,
    val forecastIcon: List<Int> = emptyList(),
    val psr: String = ""
)

data class SeaTemp(
    val place: String = "",
    val value: Double = 0.0,
    val unit: String = "°C",
    val recordTime: String = ""
)

data class SoilTemp(
    val place: String = "",
    val value: Double = 0.0,
    val unit: String = "°C",
    val recordTime: String = "",
    val depth: SoilDepth? = null
)

data class SoilDepth(
    val unit: String = "m",
    val value: Double = 0.0
)

data class WarningDetail(
    val warningStatementCode: String? = null,
    val subtype: String? = null,
    val contents: List<String> = emptyList(),
    val issueTime: String? = null,
    val updateTime: String? = null,
    val expiryTime: String? = null
)
