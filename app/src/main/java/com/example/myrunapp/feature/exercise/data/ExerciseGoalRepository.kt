package com.example.myrunapp.feature.exercise.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_WEEKLY_DISTANCE_GOAL_KM = 40.0
const val DEFAULT_MONTHLY_DISTANCE_GOAL_KM = 100.0

data class ExerciseGoalSettings(
    val weeklyDistanceGoalKm: Double = DEFAULT_WEEKLY_DISTANCE_GOAL_KM,
    val monthlyDistanceGoalKm: Double = DEFAULT_MONTHLY_DISTANCE_GOAL_KM
)

private val Context.exerciseSettingsDataStore by preferencesDataStore(name = "exercise_settings")

class ExerciseGoalRepository(
    context: Context
) {
    private val dataStore = context.applicationContext.exerciseSettingsDataStore
    private val weeklyDistanceGoalKey = doublePreferencesKey("weeklyDistanceGoalKm")
    private val monthlyDistanceGoalKey = doublePreferencesKey("monthlyDistanceGoalKm")

    val goalSettings: Flow<ExerciseGoalSettings> = dataStore.data.map { preferences ->
        ExerciseGoalSettings(
            weeklyDistanceGoalKm = preferences[weeklyDistanceGoalKey] ?: DEFAULT_WEEKLY_DISTANCE_GOAL_KM,
            monthlyDistanceGoalKm = preferences[monthlyDistanceGoalKey] ?: DEFAULT_MONTHLY_DISTANCE_GOAL_KM
        )
    }

    suspend fun saveGoals(weeklyDistanceGoalKm: Double, monthlyDistanceGoalKm: Double) {
        dataStore.edit { preferences ->
            preferences[weeklyDistanceGoalKey] = weeklyDistanceGoalKm
            preferences[monthlyDistanceGoalKey] = monthlyDistanceGoalKm
        }
    }
}
