package com.example.myrunapp.feature.run.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseRecordId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val caloriesKcal: Int,
    val createdAt: Long
)
