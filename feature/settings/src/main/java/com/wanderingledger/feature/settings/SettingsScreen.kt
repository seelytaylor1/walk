package com.wanderingledger.feature.settings

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.wanderingledger.core.designsystem.accessibility.ContrastMode

// ── State ────────────────────────────────────────────────────────────────────

data class SettingsScreenState(
    // Audio
    val sfxEnabled: Boolean = true,
    val ambientEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    /** 0–100 */
    val sfxVolume: Int = 80,
    /** 0–100 */
    val ambientVolume: Int = 50,
    // Accessibility
    val reduceMotion: Boolean = false,
    val contrastMode: ContrastMode = ContrastMode.STANDARD,
)

// ── Actions ──────────────────────────────────────────────────────────────────

fun interface SettingsNavigationCallback {
    fun onNavigateBack()
}

data class SettingsActions(
    val onNavigateBack: SettingsNavigationCallback,
    // Audio
    val onSfxEnabledChanged: (Boolean) -> Unit,
    val onAmbientEnabledChanged: (Boolean) -> Unit,
    val onHapticsEnabledChanged: (Boolean) -> Unit,
    val onSfxVolumeChanged: (Int) -> Unit,
    val onAmbientVolumeChanged: (Int) -> Unit,
    // Accessibility
    val onReduceMotionChanged: (Boolean) -> Unit,
    val onContrastModeChanged: (ContrastMode) -> Unit,
)

// ── View ─────────────────────────────────────────────────────────────────────

/**
 * Settings screen — View-based, matching the project's existing screen pattern.
 *
 * Covers audio preferences (Slice 19) and accessibility preferences (Slice 20).
 * All changes apply immediately via [SettingsActions] callbacks; no restart required.
 */
class SettingsScreenView(
    context: Context,
) : ScrollView(context) {
    private val container =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

    // ── Section: Sound Effects ────────────────────────────────────────────

    private val sfxHeader = sectionHeader(context, "Sound Effects")

    private val sfxToggle =
        CheckBox(context).apply {
            text = "Sound effects"
        }

    private val sfxVolumeLabel = bodyLabel(context, "SFX volume")

    private val sfxVolumeBar =
        SeekBar(context).apply {
            max = 100
            contentDescription = "SFX volume slider"
        }

    // ── Section: Ambient Sounds ───────────────────────────────────────────

    private val ambientHeader = sectionHeader(context, "Ambient Sounds")

    private val ambientToggle =
        CheckBox(context).apply {
            text = "Ambient sounds"
        }

    private val ambientVolumeLabel = bodyLabel(context, "Ambient volume")

    private val ambientVolumeBar =
        SeekBar(context).apply {
            max = 100
            contentDescription = "Ambient volume slider"
        }

    // ── Section: Haptics ─────────────────────────────────────────────────

    private val hapticsHeader = sectionHeader(context, "Haptic Feedback")

    private val hapticsToggle =
        CheckBox(context).apply {
            text = "Haptic feedback"
        }

    // ── Section: Accessibility ────────────────────────────────────────────

    private val accessibilityHeader = sectionHeader(context, "Accessibility")

    private val reduceMotionToggle =
        CheckBox(context).apply {
            text = "Reduce motion"
        }

    private val contrastModeLabel = bodyLabel(context, "Contrast mode")

    private val contrastModeGroup =
        RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
        }

    private val contrastStandard =
        RadioButton(context).apply {
            text = "Standard"
            id = ContrastMode.STANDARD.ordinal + 1
        }
    private val contrastHigh =
        RadioButton(context).apply {
            text = "High"
            id = ContrastMode.HIGH.ordinal + 1
        }
    private val contrastNight =
        RadioButton(context).apply {
            text = "Night"
            id = ContrastMode.NIGHT.ordinal + 1
        }

    // ── Navigation ────────────────────────────────────────────────────────

    private val backButton =
        Button(context).apply {
            text = "Back"
        }

    init {
        addView(container)

        contrastModeGroup.addView(contrastStandard)
        contrastModeGroup.addView(contrastHigh)
        contrastModeGroup.addView(contrastNight)

        container.addView(sfxHeader, matchWidth())
        container.addView(sfxToggle, matchWidth())
        container.addView(sfxVolumeLabel, matchWidth().apply { topMargin = 16 })
        container.addView(sfxVolumeBar, matchWidth().apply { topMargin = 4 })

        container.addView(ambientHeader, matchWidth().apply { topMargin = 32 })
        container.addView(ambientToggle, matchWidth())
        container.addView(ambientVolumeLabel, matchWidth().apply { topMargin = 16 })
        container.addView(ambientVolumeBar, matchWidth().apply { topMargin = 4 })

        container.addView(hapticsHeader, matchWidth().apply { topMargin = 32 })
        container.addView(hapticsToggle, matchWidth())

        container.addView(accessibilityHeader, matchWidth().apply { topMargin = 32 })
        container.addView(reduceMotionToggle, matchWidth())
        container.addView(contrastModeLabel, matchWidth().apply { topMargin = 16 })
        container.addView(contrastModeGroup, matchWidth().apply { topMargin = 4 })

        container.addView(backButton, matchWidth().apply { topMargin = 48 })
    }

    fun render(
        state: SettingsScreenState,
        actions: SettingsActions,
    ) {
        // ── SFX ──────────────────────────────────────────────────────────
        sfxToggle.isChecked = state.sfxEnabled
        sfxVolumeBar.progress = state.sfxVolume
        sfxVolumeBar.isEnabled = state.sfxEnabled
        sfxVolumeLabel.alpha = if (state.sfxEnabled) 1f else 0.4f

        sfxToggle.setOnCheckedChangeListener { _, checked ->
            sfxVolumeBar.isEnabled = checked
            sfxVolumeLabel.alpha = if (checked) 1f else 0.4f
            actions.onSfxEnabledChanged(checked)
        }

        sfxVolumeBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    bar: SeekBar,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) actions.onSfxVolumeChanged(progress)
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit

                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            },
        )

        // ── Ambient ───────────────────────────────────────────────────────
        ambientToggle.isChecked = state.ambientEnabled
        ambientVolumeBar.progress = state.ambientVolume

        ambientToggle.setOnCheckedChangeListener { _, checked ->
            actions.onAmbientEnabledChanged(checked)
        }

        ambientVolumeBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    bar: SeekBar,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) actions.onAmbientVolumeChanged(progress)
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit

                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            },
        )

        // ── Haptics ───────────────────────────────────────────────────────
        hapticsToggle.isChecked = state.hapticsEnabled
        hapticsToggle.setOnCheckedChangeListener { _, checked ->
            actions.onHapticsEnabledChanged(checked)
        }

        // ── Accessibility ─────────────────────────────────────────────────
        reduceMotionToggle.isChecked = state.reduceMotion
        reduceMotionToggle.setOnCheckedChangeListener { _, checked ->
            actions.onReduceMotionChanged(checked)
        }

        val checkedId = state.contrastMode.ordinal + 1
        contrastModeGroup.check(checkedId)
        contrastModeGroup.setOnCheckedChangeListener { _, id ->
            val mode =
                when (id) {
                    ContrastMode.HIGH.ordinal + 1 -> ContrastMode.HIGH
                    ContrastMode.NIGHT.ordinal + 1 -> ContrastMode.NIGHT
                    else -> ContrastMode.STANDARD
                }
            actions.onContrastModeChanged(mode)
        }

        // ── Navigation ────────────────────────────────────────────────────
        backButton.setOnClickListener { actions.onNavigateBack.onNavigateBack() }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sectionHeader(
        context: Context,
        title: String,
    ) = TextView(context).apply {
        text = title
        textSize = 18f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun bodyLabel(
        context: Context,
        label: String,
    ) = TextView(context).apply {
        text = label
        textSize = 14f
    }

    private fun matchWidth() =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
}
