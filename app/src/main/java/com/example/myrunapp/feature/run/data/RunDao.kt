package com.example.myrunapp.feature.run.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM run_sessions WHERE exerciseRecordId = :exerciseRecordId LIMIT 1")
    suspend fun getSessionByExerciseRecordId(exerciseRecordId: Long): RunSessionEntity?

    @Update
    suspend fun updateSession(session: RunSessionEntity)

    @Query("SELECT * FROM run_track_points WHERE sessionId = :sessionId ORDER BY recordedAt ASC")
    fun observeTrackPoints(sessionId: Long): Flow<List<RunTrackPointEntity>>

    @Query("SELECT * FROM run_track_points WHERE id % :stride = 0 ORDER BY sessionId ASC, recordedAt ASC")
    fun observeSampledTrackPoints(stride: Int): Flow<List<RunTrackPointEntity>>

    @Query(
        """
        SELECT * FROM run_track_points
        WHERE id IN (
            SELECT p.id FROM run_track_points p
            JOIN (SELECT sessionId, MIN(recordedAt) AS edgeTime FROM run_track_points GROUP BY sessionId) m
                ON m.sessionId = p.sessionId AND m.edgeTime = p.recordedAt
            UNION
            SELECT p.id FROM run_track_points p
            JOIN (SELECT sessionId, MAX(recordedAt) AS edgeTime FROM run_track_points GROUP BY sessionId) m
                ON m.sessionId = p.sessionId AND m.edgeTime = p.recordedAt
        )
        ORDER BY sessionId ASC, recordedAt ASC
        """
    )
    fun observeTrackPointEdges(): Flow<List<RunTrackPointEntity>>

    @Query("DELETE FROM run_track_points WHERE sessionId IN (SELECT id FROM run_sessions WHERE exerciseRecordId = :exerciseRecordId)")
    suspend fun deleteTrackPointsByExerciseRecordId(exerciseRecordId: Long)

    @Query("DELETE FROM run_sessions WHERE exerciseRecordId = :exerciseRecordId")
    suspend fun deleteSessionsByExerciseRecordId(exerciseRecordId: Long)
}
