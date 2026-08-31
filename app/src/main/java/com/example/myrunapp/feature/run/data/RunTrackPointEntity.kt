package com.example.myrunapp.feature.run.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_track_points")
data class RunTrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val coordinateSystem: String = "WGS84",
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val recordedAt: Long
)
