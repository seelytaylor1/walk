package com.wanderingledger.core.data

import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import kotlinx.coroutines.flow.firstOrNull

const val COMPANION_COMMENTARY_COOLDOWN_MS = 45_000L

enum class CompanionCommentaryContext {
    Town,
    Camp,
    Road,
    Arrival,
    LowSteps,
}

data class CompanionCommentary(
    val companionId: Long,
    val companionName: String,
    val context: CompanionCommentaryContext,
    val line: String,
    val tone: String,
    val generatedAtMs: Long,
)

sealed interface CompanionCommentaryResult {
    data class Spoken(
        val commentary: CompanionCommentary,
    ) : CompanionCommentaryResult

    data class OnCooldown(
        val companionName: String,
        val remainingMs: Long,
    ) : CompanionCommentaryResult

    data object NotActive : CompanionCommentaryResult
}

class CompanionCommentaryEngine(
    private val cooldownMs: Long = COMPANION_COMMENTARY_COOLDOWN_MS,
) {
    private val lastSpokenAtByCompanion = mutableMapOf<Long, Long>()

    fun selectLine(
        companion: Companion,
        context: CompanionCommentaryContext,
        biome: Biome?,
        bankedSteps: Long?,
        nowMs: Long = System.currentTimeMillis(),
    ): CompanionCommentaryResult {
        val lastSpokenAt = lastSpokenAtByCompanion[companion.companionId]
        if (lastSpokenAt != null) {
            val remainingMs = cooldownMs - (nowMs - lastSpokenAt)
            if (remainingMs > 0) {
                return CompanionCommentaryResult.OnCooldown(companion.name, remainingMs)
            }
        }

        lastSpokenAtByCompanion[companion.companionId] = nowMs
        return CompanionCommentaryResult.Spoken(
            CompanionCommentary(
                companionId = companion.companionId,
                companionName = companion.name,
                context = context,
                line = companion.lineFor(context, biome, bankedSteps),
                tone = companion.toneFor(context, bankedSteps),
                generatedAtMs = nowMs,
            ),
        )
    }

    fun clearCooldown(companionId: Long) {
        lastSpokenAtByCompanion.remove(companionId)
    }
}

suspend fun CompanionRepository.requestCommentary(
    companionId: Long,
    context: CompanionCommentaryContext,
    engine: CompanionCommentaryEngine,
    biome: Biome? = null,
    bankedSteps: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
): CompanionCommentaryResult {
    val companion =
        observeActiveCompanions()
            .firstOrNull()
            ?.firstOrNull { it.companionId == companionId }
            ?: return CompanionCommentaryResult.NotActive

    return engine.selectLine(
        companion = companion,
        context = context,
        biome = biome,
        bankedSteps = bankedSteps,
        nowMs = nowMs,
    )
}

private fun Companion.lineFor(
    context: CompanionCommentaryContext,
    biome: Biome?,
    bankedSteps: Long?,
): String {
    if (context == CompanionCommentaryContext.LowSteps || (bankedSteps != null && bankedSteps < 80L)) {
        return when (role) {
            CompanionRole.Fighter -> "No shame in a slower road. We move when the legs are ready."
            CompanionRole.Scout -> "A short walk before dusk would open more of the map."
            CompanionRole.Healer -> "Rest counts too. The road is patient with careful feet."
            CompanionRole.Rogue -> "Could squeeze a little distance from a clever detour, but your boots decide."
            CompanionRole.Mage -> "The road listens for rhythm. A few more steps would wake it."
        }
    }

    return when (context) {
        CompanionCommentaryContext.Town -> townLine()
        CompanionCommentaryContext.Camp -> campLine(biome)
        CompanionCommentaryContext.Road -> roadLine(biome)
        CompanionCommentaryContext.Arrival -> arrivalLine()
        CompanionCommentaryContext.LowSteps -> error("Low step commentary handled before context selection.")
    }
}

private fun Companion.townLine(): String =
    when (role) {
        CompanionRole.Fighter -> "I trust this town more after seeing its gates. Still, I will keep watch."
        CompanionRole.Scout -> "The lanes here fold back on themselves. Good place for rumors to hide."
        CompanionRole.Healer -> "Warm windows, clean water, quiet benches. We should mend what the road shook loose."
        CompanionRole.Rogue -> "Every market has two prices: the one spoken, and the one meant."
        CompanionRole.Mage -> "Old stories cling to the eaves here. Listen long enough and they answer."
    }

private fun Companion.campLine(biome: Biome?): String =
    when (biome) {
        Biome.Mountain -> "Stone keeps the day's heat better than people think. Sit close."
        Biome.Swamp -> "The marsh sings in layers. Best sleep between the low notes."
        Biome.Coast -> "Tide air makes even a small fire feel like a lighthouse."
        Biome.Forest, null ->
            when (role) {
                CompanionRole.Fighter -> "Trees make decent walls if you know where to stand."
                CompanionRole.Scout -> "Something small crossed our trail twice. Curious, not dangerous."
                CompanionRole.Healer -> "I found mint near the roots. Tomorrow's tea will forgive us."
                CompanionRole.Rogue -> "A tucked-away camp is worth more than a locked door."
                CompanionRole.Mage -> "The canopy writes in shadows when the fire turns low."
            }
    }

private fun Companion.roadLine(biome: Biome?): String =
    when (role) {
        CompanionRole.Fighter -> "Keep the pack high and the pace honest. Trouble hates a steady line."
        CompanionRole.Scout ->
            when (biome) {
                Biome.Mountain -> "Loose shale ahead. Step where the lichen breaks."
                Biome.Swamp -> "The dry ground is pretending. Follow the reeds instead."
                Biome.Coast -> "Gulls are turning inland. Weather may follow."
                Biome.Forest, null -> "Birdsong thinned out ahead. We should soften our stride."
            }
        CompanionRole.Healer -> "Breathe from the belly on the climbs. It saves more strength than bravado."
        CompanionRole.Rogue -> "Road looks empty. That usually means someone worked very hard to make it look empty."
        CompanionRole.Mage -> "There is a bend coming. I can feel the old mile-stones remembering it."
    }

private fun Companion.arrivalLine(): String =
    when {
        bondLevel >= 4 -> "Another town, another page. I am glad it is our page."
        bondLevel >= 2 -> "We are learning how to arrive together."
        else -> "New roofs, new smells, new chances to misread a signboard."
    }

private fun Companion.toneFor(
    context: CompanionCommentaryContext,
    bankedSteps: Long?,
): String =
    when {
        context == CompanionCommentaryContext.LowSteps || (bankedSteps != null && bankedSteps < 80L) -> "Gentle"
        bondLevel >= 4 -> "Warm"
        bondLevel >= 2 -> "Steady"
        else -> "Curious"
    }
