package com.wanderingledger.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.util.Log
import com.wanderingledger.core.model.Biome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.AudioManager as SystemAudioManager

private const val TAG = "AudioManager"
private const val FADE_STEP_MS = 50L
private const val FADE_DURATION_MS = 800L
private const val FADE_STEPS = (FADE_DURATION_MS / FADE_STEP_MS).toInt()

/**
 * Manages all audio playback for Wandering Ledger.
 *
 * - One-shot SFX: [SoundPool] (max 4 concurrent streams)
 * - Ambient biome loop: single [MediaPlayer] with crossfade
 * - Audio focus: requests transient focus for SFX; handles phone-call interruptions
 *
 * Instantiate once in MainActivity and call [release] in onDestroy.
 */
class AudioManager(
    private val context: Context,
    private val prefs: AudioPreferences,
    private val scope: CoroutineScope,
) {
    private val systemAudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager

    // ── SoundPool ────────────────────────────────────────────────────────────

    private val audioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

    /**
     * Maps each AudioEvent to its raw resource ID.
     * Raw resources live in core/audio/src/main/res/raw/.
     * Entries with a null resource ID are silently skipped until the asset is added.
     */
    private val eventResIds: Map<AudioEvent, Int?> =
        mapOf(
            AudioEvent.StepBankTick to resIdOrNull("step_bank_tick"),
            AudioEvent.TravelBegin to resIdOrNull("travel_begin"),
            AudioEvent.TownArrival to resIdOrNull("town_arrival"),
            AudioEvent.MarketBuy to resIdOrNull("market_buy"),
            AudioEvent.MarketSell to resIdOrNull("market_sell"),
            AudioEvent.LedgerOpen to resIdOrNull("ledger_open"),
            AudioEvent.RumorReceived to resIdOrNull("rumor_received"),
            AudioEvent.EncounterStart to resIdOrNull("encounter_start"),
            AudioEvent.EncounterSuccess to resIdOrNull("encounter_success"),
            AudioEvent.EncounterFailure to resIdOrNull("encounter_failure"),
            AudioEvent.BondIncrease to resIdOrNull("bond_increase"),
        )

    /** Loaded SoundPool sound IDs, populated after load completes. */
    private val soundIds = mutableMapOf<AudioEvent, Int>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) Log.w(TAG, "SoundPool load failed for sampleId=$sampleId")
        }
        loadSounds()
    }

    private fun loadSounds() {
        eventResIds.forEach { (event, resId) ->
            if (resId != null) {
                soundIds[event] = pool.load(context, resId, 1)
            }
        }
    }

    // ── Audio focus ──────────────────────────────────────────────────────────

    private val focusChangeListener =
        SystemAudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                SystemAudioManager.AUDIOFOCUS_LOSS,
                -> {
                    // Phone call or other app took focus — pause ambient
                    ambientPlayer?.setVolume(0f, 0f)
                }
                SystemAudioManager.AUDIOFOCUS_GAIN -> {
                    // Focus returned — resume ambient at current volume
                    scope.launch {
                        val vol = prefs.ambientVolume.first()
                        ambientPlayer?.setVolume(vol, vol)
                    }
                }
            }
        }

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest
                .Builder(SystemAudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
        } else {
            null
        }

    // ── Ambient MediaPlayer ──────────────────────────────────────────────────

    private var ambientPlayer: MediaPlayer? = null
    private var currentAmbientBiome: Biome? = null
    private var fadeJob: Job? = null

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Plays a one-shot SFX for [event]. No-ops if SFX is disabled in preferences or the sound
     * asset has not been loaded yet.
     */
    fun play(event: AudioEvent) {
        scope.launch {
            val enabled = prefs.sfxEnabled.first()
            val volume = prefs.sfxVolume.first()
            if (!enabled) return@launch
            val id = soundIds[event] ?: return@launch
            pool.play(id, volume, volume, 0, 0, 1f)
        }
    }

    /**
     * Starts the ambient loop for [biome]. If the same biome is already playing, this is a no-op.
     * Crossfades out the previous loop and fades in the new one.
     */
    fun startAmbient(biome: Biome) {
        if (currentAmbientBiome == biome && ambientPlayer?.isPlaying == true) return
        val resId = biome.ambientResId ?: return

        scope.launch {
            val enabled = prefs.ambientEnabled.first()
            val targetVol = prefs.ambientVolume.first()
            if (!enabled) return@launch

            // Fade out and release the old player
            val old = ambientPlayer
            if (old != null) {
                fadeJob?.cancel()
                fadeJob =
                    launch {
                        fadeOut(old)
                        old.release()
                    }
                fadeJob?.join()
            }

            currentAmbientBiome = biome
            ambientPlayer =
                withContext(Dispatchers.IO) {
                    MediaPlayer.create(context, resId)?.apply {
                        isLooping = true
                        setVolume(0f, 0f)
                        start()
                    }
                }

            requestAudioFocus()
            fadeJob = launch { fadeIn(ambientPlayer, targetVol) }
        }
    }

    /** Fades out and releases the ambient player. */
    fun stopAmbient() {
        scope.launch {
            val player = ambientPlayer ?: return@launch
            fadeJob?.cancel()
            fadeJob =
                launch {
                    fadeOut(player)
                    player.release()
                    ambientPlayer = null
                    currentAmbientBiome = null
                    abandonAudioFocus()
                }
        }
    }

    /** Applies the current ambient volume preference to the running player immediately. */
    fun applyAmbientVolume() {
        scope.launch {
            val vol = prefs.ambientVolume.first()
            ambientPlayer?.setVolume(vol, vol)
        }
    }

    /** Releases all resources. Call from Activity.onDestroy(). */
    fun release() {
        fadeJob?.cancel()
        pool.release()
        ambientPlayer?.release()
        ambientPlayer = null
        abandonAudioFocus()
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private suspend fun fadeIn(
        player: MediaPlayer?,
        targetVolume: Float,
    ) {
        player ?: return
        repeat(FADE_STEPS) { step ->
            val vol = (step + 1).toFloat() / FADE_STEPS * targetVolume
            player.setVolume(vol, vol)
            delay(FADE_STEP_MS)
        }
        player.setVolume(targetVolume, targetVolume)
    }

    private suspend fun fadeOut(player: MediaPlayer) {
        val startVol = 1f // we don't track current vol precisely; start from max
        repeat(FADE_STEPS) { step ->
            val vol = (FADE_STEPS - step - 1).toFloat() / FADE_STEPS * startVol
            player.setVolume(vol, vol)
            delay(FADE_STEP_MS)
        }
        player.setVolume(0f, 0f)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { systemAudioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            systemAudioManager.requestAudioFocus(
                focusChangeListener,
                SystemAudioManager.STREAM_MUSIC,
                SystemAudioManager.AUDIOFOCUS_GAIN,
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { systemAudioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            systemAudioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    /** Returns the raw resource ID for [name], or null if the resource doesn't exist yet. */
    private fun resIdOrNull(name: String): Int? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (id == 0) null else id
    }
}

/** Maps each [Biome] to its ambient loop raw resource ID, or null if not yet added. */
val Biome.ambientResId: Int?
    get() = null // Placeholder — replace with R.raw.ambient_forest etc. once assets are added
