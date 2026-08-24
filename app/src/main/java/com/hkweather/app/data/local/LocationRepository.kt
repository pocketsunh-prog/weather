package com.hkweather.app.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao
) {
    suspend fun saveLocation(latitude: Double, longitude: Double, name: String) {
        locationDao.saveLocation(
            LocationEntity(
                latitude = latitude,
                longitude = longitude,
                name = name
            )
        )
    }

    suspend fun getLastLocation(): LocationEntity? {
        return locationDao.getLastLocation()
    }

    fun getLastLocationFlow(): Flow<LocationEntity?> {
        return locationDao.getLastLocationFlow()
    }
}
