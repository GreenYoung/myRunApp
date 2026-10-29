package com.example.myrunapp.feature.run.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 进行中的跑步草稿。固定使用一行（id = 1），进程被杀后可以在下次启动时恢复。
 */
@Entity(tableName = "run_draft")
data class RunDraftEntity(
    @PrimaryKey val id: Long = DRAFT_ID,
    val startTime: Long,
    val pausedAtMs: Long,
    val totalPausedMs: Long,
    val durationSeconds: Long,
    val totalDistanceMeters: Double,
    val caloriesKcal: Int,
    val weightKg: Double,
    val updatedAt: Long
) {
    companion object {
        const val DRAFT_ID = 1L
    }
}

@Entity(tableName = "run_draft_points")
data class RunDraftPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val coordinateSystem: String = "WGS84",
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val recordedAt: Long
)
