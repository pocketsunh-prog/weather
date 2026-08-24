package com.hkweather.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.hkweather.app.data.local.LocationRepository
import com.hkweather.app.data.model.*
import com.hkweather.app.data.repository.LocationData
import com.hkweather.app.data.repository.WeatherRepository
import com.hkweather.app.data.repository.WeatherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadWeatherData()
        getCurrentLocation()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getAllWeatherData()
                .onSuccess { (weather, forecast, warnings) ->
                    val display = repository.mapToCurrentWeatherDisplay(weather)
                    val forecastDays = repository.mapToForecastDays(forecast)
                    val warningList = repository.mapToWarnings(warnings)
                    val rainPrediction = repository.mapToRainPrediction(weather)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentWeather = display.copy(upcomingRain = rainPrediction),
                            forecast = forecastDays,
                            warnings = warningList
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load weather data"
                        )
                    }
                }
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()

                if (location != null) {
                    // Got GPS location — save to DB and use it
                    locationRepository.saveLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        name = "Current Location"
                    )
                    _uiState.update { state ->
                        state.copy(
                            currentLocation = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                name = "Current Location"
                            )
                        )
                    }
                } else {
                    // GPS returned null — fall back to DB
                    useSavedLocation()
                }
            } catch (e: Exception) {
                // GPS failed — fall back to DB
                useSavedLocation()
            }
        }
    }

    private suspend fun useSavedLocation() {
        val saved = locationRepository.getLastLocation()
        if (saved != null) {
            _uiState.update { state ->
                state.copy(
                    currentLocation = LocationData(
                        latitude = saved.latitude,
                        longitude = saved.longitude,
                        name = "Saved Location"
                    )
                )
            }
        } else {
            // No saved location — use Hong Kong default
            _uiState.update { state ->
                state.copy(
                    currentLocation = LocationData(
                        latitude = 22.3193,
                        longitude = 114.1694,
                        name = "Hong Kong (default)"
                    )
                )
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCurrentLocation()
            loadWeatherData()
        }
    }
}
