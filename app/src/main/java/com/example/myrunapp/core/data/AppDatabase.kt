package com.example.myrunapp.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myrunapp.feature.exercise.data.ExerciseDao
import com.example.myrunapp.feature.exercise.data.ExerciseRecordEntity
import com.example.myrunapp.feature.run.data.RunDao
import com.example.myrunapp.feature.run.data.RunDraftDao
import com.example.myrunapp.feature.run.data.RunDraftEntity
import com.example.myrunapp.feature.run.data.RunDraftPointEntity
import com.example.myrunapp.feature.run.data.RunSessionEntity
import com.example.myrunapp.feature.run.data.RunTrackPointEntity
import com.example.myrunapp.feature.weight.data.WeightDao
import com.example.myrunapp.feature.weight.data.WeightRecordEntity

@Database(
    entities = [
        WeightRecordEntity::class,
        ExerciseRecordEntity::class,
        RunSessionEntity::class,
        RunTrackPointEntity::class,
        RunDraftEntity::class,
        RunDraftPointEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun runDao(): RunDao
    abstract fun runDraftDao(): RunDraftDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_run_app.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `caloriesKcal` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `run_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `exerciseRecordId` INTEGER NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `caloriesKcal` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `run_track_points` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `accuracyMeters` REAL,
                        `altitudeMeters` REAL,
                        `speedMetersPerSecond` REAL,
                        `recordedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `run_track_points` ADD COLUMN `coordinateSystem` TEXT NOT NULL DEFAULT 'WGS84'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `weight_records` ADD COLUMN `note` TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `run_draft` (
                        `id` INTEGER NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `pausedAtMs` INTEGER NOT NULL,
                        `totalPausedMs` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `totalDistanceMeters` REAL NOT NULL,
                        `caloriesKcal` INTEGER NOT NULL,
                        `weightKg` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `run_draft_points` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `coordinateSystem` TEXT NOT NULL,
                        `accuracyMeters` REAL,
                        `altitudeMeters` REAL,
                        `speedMetersPerSecond` REAL,
                        `recordedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `exercise_records` ADD COLUMN `inclinePercent` REAL")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_records_startTime` ON `exercise_records` (`startTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_sessions_exerciseRecordId` ON `run_sessions` (`exerciseRecordId`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_run_track_points_sessionId_recordedAt` " +
                        "ON `run_track_points` (`sessionId`, `recordedAt`)"
                )
            }
        }
    }
}
