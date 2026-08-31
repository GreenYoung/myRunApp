package com.example.myrunapp.feature.weight.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey val date: String,
    val weightKg: Double,
    val createdAt: Long,
    val updatedAt: Long
)
