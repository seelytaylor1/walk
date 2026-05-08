package com.wanderingledger.feature.calibration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.calibrationDataStore: DataStore<Preferences> by preferencesDataStore(name = "calibration")

class CalibrationRepository(private val context: Context) {

    private object Keys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val LAST_CALIBRATED_AT = longPreferencesKey("last_calibrated_at")
        val STEP_COUNTER_AVAILABLE = floatPreferencesKey("step_counter_available")
    }

    suspend fun saveCalibrationProfile(sensitivity: Float) {
        context.calibrationDataStore.edit { prefs ->
            prefs[Keys.SENSITIVITY] = sensitivity
            prefs[Keys.LAST_CALIBRATED_AT] = System.currentTimeMillis()
        }
    }

    fun observeCalibrationProfile() = context.calibrationDataStore.data

    suspend fun isStepCounterAvailable(): Boolean {
        var available = false
        context.calibrationDataStore.edit { prefs ->
            available = prefs[Keys.STEP_COUNTER_AVAILABLE]?.toInt() == 1
        }
        return available
    }

    suspend fun setStepCounterAvailable(available: Boolean) {
        context.calibrationDataStore.edit { prefs ->
            prefs[Keys.STEP_COUNTER_AVAILABLE] = if (available) 1f else 0f
        }
    }

    suspend fun resetCalibration() {
        context.calibrationDataStore.edit { prefs ->
            prefs.remove(Keys.SENSITIVITY)
            prefs.remove(Keys.LAST_CALIBRATED_AT)
        }
    }
}