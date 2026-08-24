package com.hkweather.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocation(location: LocationEntity)

    @Query("SELECT * FROM saved_locations WHERE id = 1")
    suspend fun getLastLocation(): LocationEntity?

    @Query("SELECT * FROM saved_locations WHERE id = 1")
    fun getLastLocationFlow(): Flow<LocationEntity?>

    @Query("DELETE FROM saved_locations")
    suspend fun clearAll()
}
