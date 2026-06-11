package com.wanderingledger.core.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.audioDataStore: DataStore<Preferences> by preferencesDataStore(name = "audio")

class AudioPreferences(
    private val context: Context,
) {
    private object Keys {
        val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
        val AMBIENT_ENABLED = booleanPreferencesKey("ambient_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
    }

    val sfxEnabled: Flow<Boolean> =
        context.audioDataStore.data
            .map { it[Keys.SFX_ENABLED] ?: true }

    val ambientEnabled: Flow<Boolean> =
        context.audioDataStore.data
            .map { it[Keys.AMBIENT_ENABLED] ?: true }

    val hapticsEnabled: Flow<Boolean> =
        context.audioDataStore.data
            .map { it[Keys.HAPTICS_ENABLED] ?: true }

    /** 0.0–1.0 */
    val sfxVolume: Flow<Float> =
        context.audioDataStore.data
            .map { it[Keys.SFX_VOLUME] ?: 0.8f }

    /** 0.0–1.0 */
    val ambientVolume: Flow<Float> =
        context.audioDataStore.data
            .map { it[Keys.AMBIENT_VOLUME] ?: 0.5f }

    suspend fun setSfxEnabled(enabled: Boolean) {
        context.audioDataStore.edit { it[Keys.SFX_ENABLED] = enabled }
    }

    suspend fun setAmbientEnabled(enabled: Boolean) {
        context.audioDataStore.edit { it[Keys.AMBIENT_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.audioDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    /** @param volume 0–100 (UI scale); stored as 0.0–1.0 */
    suspend fun setSfxVolume(volume: Int) {
        context.audioDataStore.edit { it[Keys.SFX_VOLUME] = volume.coerceIn(0, 100) / 100f }
    }

    /** @param volume 0–100 (UI scale); stored as 0.0–1.0 */
    suspend fun setAmbientVolume(volume: Int) {
        context.audioDataStore.edit { it[Keys.AMBIENT_VOLUME] = volume.coerceIn(0, 100) / 100f }
    }
}
