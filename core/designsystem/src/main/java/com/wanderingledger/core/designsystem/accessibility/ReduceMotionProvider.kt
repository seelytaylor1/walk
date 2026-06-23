package com.wanderingledger.core.designsystem.accessibility

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Composition local that is `true` when any reduce-motion signal is active:
 * - System "Remove animations" (ANIMATOR_DURATION_SCALE == 0)
 * - User override stored in [AccessibilityPreferences]
 *
 * Consumers read this via `LocalReduceMotion.current`.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Reads the system animator duration scale and the user's stored preference, then provides
 * the combined value via [LocalReduceMotion].
 *
 * Place this at the root of the Compose tree (inside the ComposeView's `setContent` block).
 *
 * @param prefs An [AccessibilityPreferences] instance. Pass `null` to rely on the system
 *   signal only (useful in previews or tests).
 */
@Composable
fun ReduceMotionProvider(
    prefs: AccessibilityPreferences?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val systemReduceMotion =
        remember(context) {
            val scale =
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                )
            scale == 0f
        }

    val userOverride by (prefs?.reduceMotion ?: kotlinx.coroutines.flow.flowOf(false))
        .collectAsState(initial = false)

    val reduceMotion = systemReduceMotion || userOverride

    androidx.compose.runtime.CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        content = content,
    )
}
