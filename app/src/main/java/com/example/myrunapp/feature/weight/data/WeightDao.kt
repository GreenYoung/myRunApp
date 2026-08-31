package com.example.myrunapp.feature.weight.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Upsert
    suspend fun upsertWeight(record: WeightRecordEntity)

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT 1")
    fun observeLatestWeight(): Flow<WeightRecordEntity?>

    @Query("SELECT * FROM weight_records ORDER BY date ASC")
    fun observeAllWeights(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE date = :date LIMIT 1")
    suspend fun getWeightByDate(date: String): WeightRecordEntity?

    @Query("SELECT * FROM weight_records WHERE date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeightOnOrBefore(date: String): WeightRecordEntity?

    @Query("DELETE FROM weight_records WHERE date = :date")
    suspend fun deleteWeightByDate(date: String)
}
