package com.example.myrunapp.feature.run.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RunDraftDao {
    @Query("SELECT * FROM run_draft WHERE id = 1")
    suspend fun getDraft(): RunDraftEntity?

    @Query("SELECT * FROM run_draft_points ORDER BY recordedAt ASC, id ASC")
    suspend fun getPoints(): List<RunDraftPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: RunDraftEntity)

    @Insert
    suspend fun insertPoint(point: RunDraftPointEntity)

    @Query("DELETE FROM run_draft_points")
    suspend fun clearPoints()

    @Query("DELETE FROM run_draft")
    suspend fun clearDraft()
}
