package com.wanderingledger.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.wanderingledger.core.audio.AudioPreferences
import com.wanderingledger.core.haptics.HapticEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Delivers haptic feedback for game events.
 *
 * Predefined [VibrationEffect] constants require API 29+. On API 26–28 the manager silently
 * no-ops for predefined effects; the REWARD waveform works on API 26+ and is used as-is.
 *
 * Instantiate once in MainActivity and pass the shared [AudioPreferences] so haptics respect the
 * same preferences object as audio.
 */
class HapticManager(
    context: Context,
    private val prefs: AudioPreferences,
    private val scope: CoroutineScope,
) {
    private val vibrator: Vibrator? =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            }
            else -> {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }

    fun perform(effect: HapticEffect) {
        scope.launch {
            val enabled = prefs.hapticsEnabled.first()
            if (!enabled) return@launch
            if (vibrator == null || !vibrator.hasVibrator()) return@launch
            vibrate(effect)
        }
    }

    private fun vibrate(effect: HapticEffect) {
        val ve: VibrationEffect =
            when (effect) {
                HapticEffect.SOFT_TAP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    } else {
                        return // silently skip on API 26–28
                    }
                }
                HapticEffect.CONFIRM -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    } else {
                        return
                    }
                }
                HapticEffect.REWARD -> {
                    // Waveform works on API 26+
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 60, 80),
                        intArrayOf(0, 120, 0, 200),
                        -1,
                    )
                }
                HapticEffect.ERROR -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    } else {
                        return
                    }
                }
            }
        vibrator?.vibrate(ve)
    }
}
