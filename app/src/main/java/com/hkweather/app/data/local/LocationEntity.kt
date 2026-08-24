package com.hkweather.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class LocationEntity(
    @PrimaryKey val id: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
