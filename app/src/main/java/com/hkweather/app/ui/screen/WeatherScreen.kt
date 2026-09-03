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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.hkweather.app.data.repository.StationReading
import com.hkweather.app.data.repository.TyphoonLocation
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

    var showLocationCard by remember { mutableStateOf(true) }
    var showWarningCards by remember { mutableStateOf(true) }

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
            WeatherMap(
                location = uiState.currentLocation,
                typhoonLocation = uiState.currentWeather?.typhoonLocation,
                typhoonTrack = uiState.typhoonTrack
            )

            // Thunder & Typhoon Warnings
            uiState.currentWeather?.let { weather ->
                if (showWarningCards && (weather.hasThunder || weather.hasTyphoon)) {
                    WarningCards(weather = weather, onShowTrack = { viewModel.showTyphoonTrack() }, onClose = { showWarningCards = false })
                }
            }

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
            if (showLocationCard) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        IconButton(
                            onClick = { showLocationCard = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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

    // Full-screen typhoon track overlay
    if (uiState.showTyphoonScreen) {
        TyphoonTrackScreen(
            track = uiState.typhoonTrack,
            userLocation = uiState.currentLocation,
            typhoonSignal = uiState.typhoonSignal,
            onBack = { viewModel.hideTyphoonScreen() }
        )
    }
}

@Composable
fun WeatherMap(
    location: com.hkweather.app.data.repository.LocationData,
    typhoonLocation: com.hkweather.app.data.repository.TyphoonLocation? = null,
    typhoonTrack: com.hkweather.app.data.repository.TyphoonTrack? = null
) {
    val hkLocation = LatLng(location.latitude, location.longitude)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var googleMapInstance by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    var currentMarker by remember { mutableStateOf<com.google.android.gms.maps.model.Marker?>(null) }
    var typhoonMarker by remember { mutableStateOf<com.google.android.gms.maps.model.Marker?>(null) }
    var trackPolyline by remember { mutableStateOf<com.google.android.gms.maps.model.Polyline?>(null) }
    var trackMarkers = remember { mutableStateOf<List<com.google.android.gms.maps.model.Marker>>(emptyList()) }

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

    // Update markers when location changes
    LaunchedEffect(hkLocation) {
        googleMapInstance?.let { map ->
            currentMarker?.remove()
            currentMarker = map.addMarker(
                MarkerOptions().position(hkLocation).title("Your Location")
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE))
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(hkLocation, 14f))
        }
    }

    // Show typhoon on map when available
    LaunchedEffect(typhoonLocation) {
        googleMapInstance?.let { map ->
            typhoonMarker?.remove()
            typhoonLocation?.let { tc ->
                val tcPos = LatLng(tc.latitude, tc.longitude)
                typhoonMarker = map.addMarker(
                    MarkerOptions().position(tcPos).title("🌀 ${tc.name}")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                )
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(hkLocation).include(tcPos).build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }

    // Draw typhoon track when available
    LaunchedEffect(typhoonTrack) {
        googleMapInstance?.let { map ->
            // Clear previous track
            trackPolyline?.remove()
            trackMarkers.value.forEach { it.remove() }
            trackMarkers.value = emptyList()

            typhoonTrack?.let { track ->
                if (track.points.isNotEmpty()) {
                    val trackPoints = track.points.map { LatLng(it.latitude, it.longitude) }
                    // Draw track line
                    val polylineOptions = com.google.android.gms.maps.model.PolylineOptions()
                        .addAll(trackPoints)
                        .color(android.graphics.Color.RED)
                        .width(5f)
                        .geodesic(true)
                    trackPolyline = map.addPolyline(polylineOptions)

                    // Add markers for each track point
                    val newMarkers = track.points.mapIndexed { index, point ->
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(point.latitude, point.longitude))
                                .title("${track.name} - ${point.timestamp}")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    if (index == track.points.size - 1) com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                                    else com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                                ))
                        )
                    }
                    trackMarkers.value = newMarkers.filterNotNull()

                    // Zoom to show entire track
                    val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    trackPoints.forEach { boundsBuilder.include(it) }
                    boundsBuilder.include(hkLocation)
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                }
            }
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
                    // Show typhoon if available
                    typhoonLocation?.let { tc ->
                        val tcPos = LatLng(tc.latitude, tc.longitude)
                        typhoonMarker = googleMap.addMarker(
                            MarkerOptions().position(tcPos).title("🌀 ${tc.name}")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED))
                        )
                    }
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
    // Nearby Stations Section
    if (weather.nearbyHumidity.isNotEmpty() || weather.nearbyWind.isNotEmpty() || weather.nearbyRainfall.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nearby Stations",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
    // Nearby Humidity
    if (weather.nearbyHumidity.isNotEmpty()) {
        NearbyReadingsCard(title = "Humidity", readings = weather.nearbyHumidity)
    }
    // Nearby Wind
    if (weather.nearbyWind.isNotEmpty()) {
        NearbyReadingsCard(title = "Wind", readings = weather.nearbyWind)
    }
    // Nearby Rainfall
    if (weather.nearbyRainfall.isNotEmpty()) {
        NearbyReadingsCard(title = "Rainfall", readings = weather.nearbyRainfall)
    }
}

@Composable
fun WarningCards(weather: CurrentWeatherDisplay, onShowTrack: () -> Unit = {}, onClose: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Close button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF795548)
                    )
                }
            }
            if (weather.hasThunder) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("⚡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Thunderstorm Warning",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Thunder expected in your area. Seek shelter indoors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E342E)
                        )
                    }
                }
            }
            if (weather.hasTyphoon) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("🌀", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Typhoon Warning",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                        Text(
                            text = weather.tcMessage.ifBlank { "Typhoon detected. Tap map to view location." },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E342E)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onShowTrack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Tropical Cyclone Track (GIS)", color = Color.White)
                }
            }
            // Debug: show raw warning message
            if (weather.tcMessage.isNotBlank() || weather.rawWarningMessage.isNotBlank()) {
                Text(
                    text = "API: ${weather.rawWarningMessage.ifBlank { "No warning" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF795548),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun NearbyReadingsCard(title: String, readings: List<StationReading>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            readings.take(4).forEach { reading ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = reading.place, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(text = "${reading.value} ${reading.unit}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyphoonTrackScreen(
    track: com.hkweather.app.data.repository.TyphoonTrack?,
    userLocation: com.hkweather.app.data.repository.LocationData,
    typhoonSignal: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = track?.name ?: "Tropical Cyclone Track",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Signal info
            if (typhoonSignal.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🌀", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = typhoonSignal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            // HKO Typhoon GIS Image
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Typhoon Track (HKO GIS)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { /* WebView reload handled below */ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.domStorageEnabled = true
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                            view?.scrollTo(0, 200)
                                        }
                                    }
                                    loadUrl("https://www.hko.gov.hk/tc/wxinfo/currwx/tc_gis.htm")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        )
                    }
                }
            }

            // Track points
            track?.let { tc ->
                item {
                    Text(
                        text = "Track Points (" + tc.points.size + ")",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            tc.points.forEachIndexed { index, point ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (index == tc.points.size - 1) "🔴" else "🟠",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Lat: " + point.latitude + "\u00b0N, Lng: " + point.longitude + "\u00b0E",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (point.timestamp.isNotBlank()) {
                                            Text(
                                                text = point.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${point.windSpeed} km/h",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                        if (point.category.isNotBlank()) {
                                            Text(
                                                text = point.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (index < tc.points.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                // Distance from user
                if (tc.points.isNotEmpty()) {
                    val latest = tc.points.last()
                    val dist = calculateDistance(userLocation.latitude, userLocation.longitude, latest.latitude, latest.longitude)
                    val bearing = calculateBearing(userLocation.latitude, userLocation.longitude, latest.latitude, latest.longitude)
                    val direction = getDirectionFromBearing(bearing)

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📍 Distance from You",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$dist km $direction",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your location: " + String.format("%.4f", userLocation.latitude) + "\u00b0N, " + String.format("%.4f", userLocation.longitude) + "\u00b0E",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // No track data
            if (track == null && typhoonSignal.isBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Typhoon Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active tropical cyclone track available.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Back button
            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Weather")
                }
            }
        }
    }
}
/**
 * Calculate distance between two coordinates in km using Haversine formula.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return (r * c).toInt()
}

/**
 * Calculate bearing from point 1 to point 2 in degrees.
 */
fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2))
    val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
            Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon)
    val bearing = Math.toDegrees(Math.atan2(y, x))
    return ((bearing + 360) % 360).toInt()
}

/**
 * Convert bearing to compass direction.
 */
fun getDirectionFromBearing(bearing: Int): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((bearing + 22.5) / 45).toInt() % 8
    return directions[index]
}
