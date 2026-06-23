package com.wanderingledger.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderingledger.core.data.CompanionCommentaryContext
import com.wanderingledger.core.data.CompanionCommentaryResult
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.RecruitmentResult
import com.wanderingledger.core.designsystem.accessibility.AccessibilityPreferences
import com.wanderingledger.core.model.Companion
import com.wanderingledger.feature.companions.CompanionCommentaryUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompanionsViewState(
    val active: List<Companion>,
    val recruitable: List<Companion>,
    val reduceMotion: Boolean,
    val message: String?,
    val latestCommentary: CompanionCommentaryUi?,
)

sealed interface CompanionsEffect {
    data object InteractSuccess : CompanionsEffect
    data object CooldownActive : CompanionsEffect
}

class CompanionsViewModel(
    private val companionRepository: CompanionRepository,
    private val gameRepository: GameRepository,
    private val narrator: CompanionNarrator,
    private val accessibilityPreferences: AccessibilityPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<CompanionsViewState?>(null)
    val state: StateFlow<CompanionsViewState?> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CompanionsEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<CompanionsEffect> = _effects.asSharedFlow()

    /** The narrator's StateFlow flows directly — all companion commentary in one place. */
    val latestCommentary: StateFlow<CompanionCommentaryUi?> = narrator.latestLine

    private var observeJob: Job? = null

    fun activate(townId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                companionRepository.observeActiveCompanions(),
                companionRepository.observeRecruitableCompanionsAtTown(townId),
                accessibilityPreferences.reduceMotion,
                narrator.latestLine,
            ) { active, recruitable, reduceMotion, commentary ->
                CompanionsViewState(
                    active = active,
                    recruitable = recruitable,
                    reduceMotion = reduceMotion,
                    message = _state.value?.message,
                    latestCommentary = commentary,
                )
            }.collect { _state.value = it }
        }
    }

    fun deactivate() {
        observeJob?.cancel()
        observeJob = null
    }

    fun recruit(companionId: Long) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                companionRepository.recruitCompanion(companionId)
            }
            val message = when (result) {
                RecruitmentResult.Success -> "A new voice joins the road."
                RecruitmentResult.AlreadyActive -> "They are already traveling with you."
                RecruitmentResult.PartyFull -> "The party is full."
                RecruitmentResult.NotFound -> "That companion is not available here."
                RecruitmentResult.NotEnoughTrades -> "Complete a few more trades first."
            }
            _state.value = _state.value?.copy(message = message)
        }
    }

    fun interact(companionId: Long, townId: Long) {
        viewModelScope.launch {
            val player = withContext(Dispatchers.IO) {
                gameRepository.observePlayerState().first()
            }
            val town = withContext(Dispatchers.IO) {
                gameRepository.observeTown(townId).first()
            }
            val context = if (player.bankedSteps < 80L)
                CompanionCommentaryContext.LowSteps
            else
                CompanionCommentaryContext.Town

            val result = withContext(Dispatchers.IO) {
                narrator.requestLine(
                    companionId = companionId,
                    context = context,
                    biome = town?.biome,
                    bankedSteps = player.bankedSteps,
                )
            }
            when (result) {
                is CompanionCommentaryResult.Spoken -> {
                    withContext(Dispatchers.IO) {
                        companionRepository.updateBond(companionId, 1)
                    }
                    _effects.emit(CompanionsEffect.InteractSuccess)
                }
                is CompanionCommentaryResult.OnCooldown -> {
                    _state.value = _state.value?.copy(
                        message = "${result.companionName} is still considering the last thing they said.",
                    )
                    _effects.emit(CompanionsEffect.CooldownActive)
                }
                CompanionCommentaryResult.NotActive ->
                    _state.value = _state.value?.copy(
                        message = "Only active companions can answer from the road.",
                    )
            }
        }
    }
}
