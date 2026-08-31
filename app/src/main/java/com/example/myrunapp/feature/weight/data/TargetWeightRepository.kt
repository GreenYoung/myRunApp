package com.example.myrunapp.feature.weight.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weightSettingsDataStore by preferencesDataStore(name = "weight_settings")

class TargetWeightRepository(
    context: Context
) {
    private val dataStore = context.applicationContext.weightSettingsDataStore
    private val targetWeightKey = doublePreferencesKey("targetWeightKg")

    val targetWeightKg: Flow<Double?> = dataStore.data.map { preferences ->
        preferences[targetWeightKey]
    }

    suspend fun saveTargetWeight(weightKg: Double) {
        dataStore.edit { preferences ->
            preferences[targetWeightKey] = weightKg
        }
    }
}
