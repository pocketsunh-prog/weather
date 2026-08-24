package com.hkweather.app.ui.screen

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hkweather.app.data.repository.CurrentWeatherDisplay
import com.hkweather.app.data.repository.ForecastDayDisplay
import com.hkweather.app.data.repository.WeatherUiState
import com.hkweather.app.ui.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.getCurrentLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Auto-load weather on first composition
    LaunchedEffect(Unit) {
        viewModel.loadWeatherData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HK Weather") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadWeatherData() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map — always fills the screen
            WeatherMap(location = uiState.currentLocation)

            // Location button on map
            FloatingActionButton(
                onClick = { viewModel.refreshLocation() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Get Current Location"
                )
            }

            // Lat/Lng overlay on map
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = uiState.currentLocation.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lat: ${String.format("%.4f", uiState.currentLocation.latitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lng: ${String.format("%.4f", uiState.currentLocation.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Error
            if (uiState.error != null && !uiState.isLoading) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadWeatherData() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Weather panel at the bottom
            if (uiState.currentWeather != null && !uiState.isLoading) {
                WeatherPanel(
                    uiState = uiState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun WeatherMap(location: com.hkweather.app.data.repository.LocationData) {
    val hkLocation = LatLng(location.latitude, location.longitude)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var googleMapInstance by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    var currentMarker by remember { mutableStateOf<com.google.android.gms.maps.model.Marker?>(null) }

    // Observe lifecycle to forward to MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewInstance?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewInstance?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewInstance?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Update marker when location changes
    LaunchedEffect(hkLocation) {
        googleMapInstance?.let { map ->
            currentMarker?.remove()
            currentMarker = map.addMarker(
                MarkerOptions().position(hkLocation).title("Your Location")
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(hkLocation, 14f))
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                mapViewInstance = this
                onCreate(null)
                onResume()
                getMapAsync { googleMap ->
                    googleMapInstance = googleMap
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    googleMap.uiSettings.isCompassEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    currentMarker = googleMap.addMarker(
                        MarkerOptions().position(hkLocation).title("Your Location")
                    )
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hkLocation, 14f))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun WeatherPanel(
    uiState: WeatherUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            CurrentWeatherCard(weather = uiState.currentWeather!!)
        }
        item {
            WeatherDetailsGrid(weather = uiState.currentWeather!!)
        }
        if (uiState.forecast.isNotEmpty()) {
            items(uiState.forecast.take(3)) { day ->
                ForecastItem(day = day)
            }
        }
    }
}

@Composable
fun CurrentWeatherCard(weather: CurrentWeatherDisplay) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    WeatherIcon(iconCode = weather.iconCode, size = 48.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weather.forecastWeather,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${weather.temperature}${weather.temperatureUnit}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin,
                        color = Color.White
                    )
                    Text(
                        text = "H: ${weather.maxTemp}°  L: ${weather.minTemp}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherDetailsGrid(weather: CurrentWeatherDisplay) {
    // Row 1: Humidity + Wind Speed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherDetailItem(
            icon = Icons.Default.WaterDrop,
            label = "Humidity",
            value = "${weather.humidity}${weather.humidityUnit}",
            modifier = Modifier.weight(1f)
        )
        WeatherDetailItem(
            icon = Icons.Default.Air,
            label = "Wind Speed",
            value = "${weather.windSpeed} ${weather.windSpeedUnit}",
            modifier = Modifier.weight(1f)
        )
    }
    // Row 2: Wind Direction + Rainfall
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherDetailItem(
            icon = Icons.Default.Navigation,
            label = "Wind Dir",
            value = weather.windDirection,
            modifier = Modifier.weight(1f)
        )
        WeatherDetailItem(
            icon = Icons.Default.Cloud,
            label = "Rainfall",
            value = "${weather.rainfall} ${weather.rainfallUnit}",
            modifier = Modifier.weight(1f)
        )
    }
    // Row 3: UV Index + Rain Prediction
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WeatherDetailItem(
            icon = Icons.Default.WbSunny,
            label = "UV Index",
            value = if (weather.uvIndex != "--") "${weather.uvIndex} ${weather.uvDescription}" else "--",
            modifier = Modifier.weight(1f)
        )
        WeatherDetailItem(
            icon = Icons.Default.WaterDrop,
            label = "Rain Predict",
            value = weather.upcomingRain,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ForecastItem(day: ForecastDayDisplay) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(50.dp)) {
                Text(text = day.date.takeLast(5), style = MaterialTheme.typography.labelSmall)
                Text(text = day.week, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            WeatherIcon(iconCode = day.iconCode, size = 24.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = day.weather, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text(
                text = "${day.maxTemp}° / ${day.minTemp}°",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun WeatherIcon(iconCode: Int, size: androidx.compose.ui.unit.Dp) {
    val icon = when (iconCode) {
        in 50..53 -> Icons.Default.WbSunny
        in 54..58 -> Icons.Default.Cloud
        in 59..64 -> Icons.Default.WaterDrop
        else -> Icons.Default.WbSunny
    }
    Icon(icon, contentDescription = "Weather", tint = Color.White, modifier = Modifier.size(size))
}
