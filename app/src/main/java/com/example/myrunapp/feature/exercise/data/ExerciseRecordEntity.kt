package com.example.myrunapp.feature.exercise.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_records",
    indices = [Index("startTime")]
)
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startTime: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val caloriesKcal: Int,
    val inclinePercent: Double? = null
)
