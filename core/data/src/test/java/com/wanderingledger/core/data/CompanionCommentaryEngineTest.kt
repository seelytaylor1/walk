package com.wanderingledger.core.data

import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionCommentaryEngineTest {
    @Test
    fun lowStepsContextSelectsEncouragingLine() {
        val engine = CompanionCommentaryEngine(cooldownMs = 1_000L)

        val result =
            engine.selectLine(
                companion = mira,
                context = CompanionCommentaryContext.Town,
                biome = Biome.Forest,
                bankedSteps = 12L,
                nowMs = 1_000L,
            )

        assertTrue(result is CompanionCommentaryResult.Spoken)
        val commentary = (result as CompanionCommentaryResult.Spoken).commentary
        assertEquals("Gentle", commentary.tone)
        assertTrue(commentary.line.contains("walk", ignoreCase = true))
    }

    @Test
    fun repeatedCommentaryForSameCompanionUsesCooldown() {
        val engine = CompanionCommentaryEngine(cooldownMs = 1_000L)

        engine.selectLine(
            companion = mira,
            context = CompanionCommentaryContext.Town,
            biome = Biome.Forest,
            bankedSteps = 200L,
            nowMs = 1_000L,
        )
        val result =
            engine.selectLine(
                companion = mira,
                context = CompanionCommentaryContext.Town,
                biome = Biome.Forest,
                bankedSteps = 200L,
                nowMs = 1_500L,
            )

        assertTrue(result is CompanionCommentaryResult.OnCooldown)
        assertEquals(500L, (result as CompanionCommentaryResult.OnCooldown).remainingMs)
    }

    @Test
    fun cooldownDoesNotBlockDifferentCompanion() {
        val engine = CompanionCommentaryEngine(cooldownMs = 1_000L)

        engine.selectLine(
            companion = mira,
            context = CompanionCommentaryContext.Town,
            biome = Biome.Forest,
            bankedSteps = 200L,
            nowMs = 1_000L,
        )
        val result =
            engine.selectLine(
                companion = bram,
                context = CompanionCommentaryContext.Town,
                biome = Biome.Forest,
                bankedSteps = 200L,
                nowMs = 1_500L,
            )

        assertTrue(result is CompanionCommentaryResult.Spoken)
    }

    private val mira =
        Companion(
            companionId = 1L,
            name = "Mira",
            role = CompanionRole.Scout,
            combatPower = 3,
            bondLevel = 1,
            questState = "recruited",
            locationTownId = 1L,
            isActive = true,
        )

    private val bram =
        Companion(
            companionId = 2L,
            name = "Bram",
            role = CompanionRole.Fighter,
            combatPower = 5,
            bondLevel = 0,
            questState = "recruited",
            locationTownId = 2L,
            isActive = true,
        )
}
