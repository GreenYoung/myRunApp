package com.example.myrunapp.feature.exercise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insertExercise(record: ExerciseRecordEntity): Long

    @Query("SELECT * FROM exercise_records ORDER BY startTime DESC")
    fun observeAllExercises(): Flow<List<ExerciseRecordEntity>>

    @Query("SELECT * FROM exercise_records WHERE id = :recordId")
    fun observeExerciseById(recordId: Long): Flow<ExerciseRecordEntity?>

    @Query("SELECT * FROM exercise_records ORDER BY startTime DESC")
    suspend fun getAllExercises(): List<ExerciseRecordEntity>

    @Query("DELETE FROM exercise_records WHERE id = :recordId")
    suspend fun deleteExerciseById(recordId: Long)

    @Update
    suspend fun updateExercise(record: ExerciseRecordEntity)
}
