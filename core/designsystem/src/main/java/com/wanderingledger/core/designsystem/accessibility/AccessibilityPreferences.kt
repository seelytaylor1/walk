package com.wanderingledger.core.designsystem.accessibility

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accessibilityDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "accessibility")

/**
 * Persists user-controlled accessibility preferences.
 *
 * System-level signals (animator duration scale, TalkBack) are read at runtime in
 * [ReduceMotionProvider] and combined with [reduceMotion] here.
 */
class AccessibilityPreferences(
    private val context: Context,
) {
    private object Keys {
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val CONTRAST_MODE = stringPreferencesKey("contrast_mode")
    }

    /** User-requested reduce-motion override (independent of system setting). */
    val reduceMotion: Flow<Boolean> =
        context.accessibilityDataStore.data
            .map { it[Keys.REDUCE_MOTION] ?: false }

    /** Current [ContrastMode]. Defaults to [ContrastMode.STANDARD]. */
    val contrastMode: Flow<ContrastMode> =
        context.accessibilityDataStore.data
            .map { prefs ->
                prefs[Keys.CONTRAST_MODE]
                    ?.let { runCatching { ContrastMode.valueOf(it) }.getOrNull() }
                    ?: ContrastMode.STANDARD
            }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.accessibilityDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setContrastMode(mode: ContrastMode) {
        context.accessibilityDataStore.edit { it[Keys.CONTRAST_MODE] = mode.name }
    }
}

enum class ContrastMode {
    /** Default atmospheric palette. */
    STANDARD,

    /**
     * All text raised to full black/white; alpha < 0.9 removed from text;
     * border weights increased to 1.5dp. Targets WCAG AA (4.5:1).
     */
    HIGH,

    /**
     * Blue channel reduced 30% across all theme colors.
     * Suitable for use before sleep.
     */
    NIGHT,
}
