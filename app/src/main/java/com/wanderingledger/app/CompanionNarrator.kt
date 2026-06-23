package com.wanderingledger.app

import com.wanderingledger.core.data.CompanionCommentaryContext
import com.wanderingledger.core.data.CompanionCommentaryEngine
import com.wanderingledger.core.data.CompanionCommentaryResult
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.model.Biome
import com.wanderingledger.feature.companions.CompanionCommentaryUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Single source of truth for companion commentary state.
 *
 * Owns the [CompanionCommentaryEngine] (cooldown timer) and the latest
 * spoken line as a [StateFlow]. Both JourneyViewModel and CompanionsViewModel
 * share the same narrator instance via [AppContainer], so commentary from
 * travel and from town interaction appear in the same flow.
 */
class CompanionNarrator(
    private val companionRepository: CompanionRepository,
    private val engine: CompanionCommentaryEngine,
) {
    private val _latestLine = MutableStateFlow<CompanionCommentaryUi?>(null)
    val latestLine: StateFlow<CompanionCommentaryUi?> = _latestLine.asStateFlow()

    suspend fun requestLine(
        companionId: Long,
        context: CompanionCommentaryContext,
        biome: Biome? = null,
        bankedSteps: Long? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): CompanionCommentaryResult {
        val companion = companionRepository.observeActiveCompanions()
            .firstOrNull()
            ?.firstOrNull { it.companionId == companionId }
            ?: return CompanionCommentaryResult.NotActive

        val result = engine.selectLine(companion, context, biome, bankedSteps, nowMs)
        if (result is CompanionCommentaryResult.Spoken) {
            _latestLine.value = result.commentary.toUi()
        }
        return result
    }
}
