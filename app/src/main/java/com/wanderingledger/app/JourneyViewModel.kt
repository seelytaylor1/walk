package com.wanderingledger.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderingledger.core.data.CompanionCommentaryContext
import com.wanderingledger.core.data.CompanionCommentaryEngine
import com.wanderingledger.core.data.CompanionCommentaryResult
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.TravelResult
import com.wanderingledger.core.data.applyScoutDiscount
import com.wanderingledger.core.data.requestCommentary
import com.wanderingledger.core.model.CompanionRole
import com.wanderingledger.core.designsystem.accessibility.AccessibilityPreferences
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.feature.journey.CampState
import com.wanderingledger.feature.journey.CampStateDetector
import com.wanderingledger.feature.journey.JourneyScreenState
import com.wanderingledger.feature.journey.buildJourneyScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One-shot side effects the journey screen produces that are the Activity's
 * concern — audio, haptics, navigation and the shared "most recent commentary"
 * surfaced on the companions screen. The ViewModel owns no Android UI, sensors
 * or navigation; it emits these and [MainActivity] performs them.
 */
sealed interface JourneyEffect {
    /** A travel was initiated (play the travel-begin cue). */
    data object TravelBegin : JourneyEffect

    /** A travel was blocked, e.g. not enough steps (play the error cue). */
    data object TravelBlocked : JourneyEffect

    /** Travel succeeded; navigate to the destination's arrival screen. */
    data class Arrived(
        val townId: Long,
        val remainingSteps: Long,
    ) : JourneyEffect

    /** Start the ambient bed for the current biome. */
    data class StartAmbient(
        val biome: Biome,
    ) : JourneyEffect

    /** A companion spoke; remember it so the companions screen can show it. */
    data class CommentaryGenerated(
        val commentary: com.wanderingledger.feature.companions.CompanionCommentaryUi,
    ) : JourneyEffect
}

/**
 * Owns all journey-screen state: the [JourneyScreenState] flow, camp state and
 * the time of the last travel (used by [CampStateDetector]). Travel, camp and
 * step-simulation actions live here. See issue 06 for the ViewModel decision.
 */
class JourneyViewModel(
    private val gameRepository: GameRepository,
    private val companionRepository: CompanionRepository,
    private val companionCommentaryEngine: CompanionCommentaryEngine,
    private val stepTrackerService: StepTrackerService,
    private val accessibilityPreferences: AccessibilityPreferences,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            JourneyScreenState(
                currentTownName = "",
                currentTownRegion = "",
                currentBiome = Biome.Forest,
                bankedSteps = 0,
                lifetimeSteps = 0,
                routes = emptyList(),
                message = null,
            ),
        )
    val state: StateFlow<JourneyScreenState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<JourneyEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<JourneyEffect> = _effects.asSharedFlow()

    private var campState: CampState? = null
    private var lastTravelTime: Long = System.currentTimeMillis()

    /** Recompute the journey state and (re)start the biome ambient bed. */
    fun refresh(message: String? = null) {
        viewModelScope.launch {
            val screenState = withContext(Dispatchers.IO) { buildState(message) }
            _state.value = screenState
            _effects.emit(JourneyEffect.StartAmbient(screenState.currentBiome))
        }
    }

    fun onTravel(segmentId: Long) {
        viewModelScope.launch {
            _effects.emit(JourneyEffect.TravelBegin)
            val result = withContext(Dispatchers.IO) { gameRepository.travel(segmentId) }
            when (result) {
                is TravelResult.Arrived -> {
                    lastTravelTime = System.currentTimeMillis()
                    _effects.emit(JourneyEffect.Arrived(result.townId, result.remainingSteps))
                }
                else -> {
                    val commentaryMessage =
                        if (result is TravelResult.NotEnoughSteps) {
                            _effects.emit(JourneyEffect.TravelBlocked)
                            firstCompanionCommentaryMessage(
                                context = CompanionCommentaryContext.LowSteps,
                                biome = null,
                                bankedSteps = result.available,
                            )
                        } else {
                            null
                        }
                    refresh(commentaryMessage ?: result.toMessage())
                }
            }
        }
    }

    fun onSimulateSteps() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                stepTrackerService.recordSensorDelta(75, StepSource.Simulation)
            }
            campState =
                campState?.let {
                    it.copy(stepsEarnedWhileCamping = it.stepsEarnedWhileCamping + 75)
                }
            refresh("Banked 75 simulated steps.")
        }
    }

    fun onMakeCamp() {
        viewModelScope.launch {
            val companions =
                withContext(Dispatchers.IO) { companionRepository.observeActiveCompanions().first() }
            val town = withContext(Dispatchers.IO) { gameRepository.observeCurrentTown().first() }
            campState = CampState.camping(biome = town.biome, companions = companions)
            val commentaryMessage =
                firstCompanionCommentaryMessage(
                    context = CompanionCommentaryContext.Camp,
                    biome = town.biome,
                    bankedSteps = null,
                )
            refresh(commentaryMessage ?: "Setting up camp...")
        }
    }

    /**
     * Record that a travel completed elsewhere (e.g. from the world map) so the
     * camp-detection clock stays in sync when the journey screen is next shown.
     */
    fun notifyTraveled() {
        lastTravelTime = System.currentTimeMillis()
    }

    fun onWakeFromCamp() {
        viewModelScope.launch {
            campState = null
            lastTravelTime = System.currentTimeMillis()
            refresh("Breaking camp...")
        }
    }

    private suspend fun buildState(message: String?): JourneyScreenState {
        val player = gameRepository.observePlayerState().first()
        val town = gameRepository.observeCurrentTown().first()
        val activeCompanions = companionRepository.observeActiveCompanions().first()
        val reduceMotion = accessibilityPreferences.reduceMotion.first()

        campState =
            if (campState == null &&
                CampStateDetector.shouldEnterCamp(
                    lastTravelTime = lastTravelTime,
                    currentTime = System.currentTimeMillis(),
                    bankedSteps = player.bankedSteps,
                )
            ) {
                CampState.camping(biome = town.biome, companions = activeCompanions)
            } else {
                campState?.withUpdatedDuration(System.currentTimeMillis())
            }

        return buildJourneyScreenState(
            currentTownName = town.name,
            currentTownRegion = town.region,
            currentBiome = town.biome,
            bankedSteps = player.bankedSteps,
            lifetimeSteps = player.lifetimeSteps,
            routeDestinations = run {
                val hasActiveScout = activeCompanions.any {
                    it.role == CompanionRole.Scout && it.isActive
                }
                gameRepository.observeTravelRoutesFromCurrentTown().first().map { route ->
                    Triple(
                        route.segment.segmentId,
                        route.destination.name,
                        Pair(
                            applyScoutDiscount(route.segment.stepCost, hasActiveScout),
                            route.segment.narrativeDistance,
                        ),
                    )
                }
            },
            message = message,
            campState = campState,
            activeCompanions = activeCompanions,
            reducedMotion = reduceMotion,
        )
    }

    private suspend fun firstCompanionCommentaryMessage(
        context: CompanionCommentaryContext,
        biome: Biome?,
        bankedSteps: Long?,
    ): String? {
        val companion =
            withContext(Dispatchers.IO) {
                companionRepository.observeActiveCompanions().first().firstOrNull()
            } ?: return null
        val result =
            withContext(Dispatchers.IO) {
                companionRepository.requestCommentary(
                    companionId = companion.companionId,
                    context = context,
                    engine = companionCommentaryEngine,
                    biome = biome,
                    bankedSteps = bankedSteps,
                )
            }
        return when (result) {
            is CompanionCommentaryResult.Spoken -> {
                _effects.emit(JourneyEffect.CommentaryGenerated(result.commentary.toUi()))
                "${result.commentary.companionName}: ${result.commentary.line}"
            }
            is CompanionCommentaryResult.OnCooldown,
            CompanionCommentaryResult.NotActive,
            -> null
        }
    }

    private fun TravelResult.toMessage(): String =
        when (this) {
            is TravelResult.Arrived -> "Arrived with $remainingSteps steps left."
            is TravelResult.NotEnoughSteps -> "Need ${required - available} more steps."
            TravelResult.NotAtRoadOrigin -> "That road starts elsewhere."
            TravelResult.RoadNotFound -> "Road not found."
        }
}
