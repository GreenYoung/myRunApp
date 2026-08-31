package com.example.myrunapp.feature.run.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Insert
    suspend fun insertSession(session: RunSessionEntity): Long

    @Insert
    suspend fun insertTrackPoints(points: List<RunTrackPointEntity>)

    @Query("SELECT * FROM run_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<RunSessionEntity?>

    @Query("SELECT * FROM run_sessions")
    fun observeAllSessions(): Flow<List<RunSessionEntity>>

    @Query("SELECT * FROM run_sessions WHERE exerciseRecordId = :exerciseRecordId LIMIT 1")
    fun observeSessionByExerciseRecordId(exerciseRecordId: Long): Flow<RunSessionEntity?>

    @Query("SELECT * FROM run_track_points WHERE sessionId = :sessionId ORDER BY recordedAt ASC")
    fun observeTrackPoints(sessionId: Long): Flow<List<RunTrackPointEntity>>

    @Query("SELECT * FROM run_track_points ORDER BY recordedAt ASC")
    fun observeAllTrackPoints(): Flow<List<RunTrackPointEntity>>

    @Query("DELETE FROM run_track_points WHERE sessionId IN (SELECT id FROM run_sessions WHERE exerciseRecordId = :exerciseRecordId)")
    suspend fun deleteTrackPointsByExerciseRecordId(exerciseRecordId: Long)

    @Query("DELETE FROM run_sessions WHERE exerciseRecordId = :exerciseRecordId")
    suspend fun deleteSessionsByExerciseRecordId(exerciseRecordId: Long)
}
