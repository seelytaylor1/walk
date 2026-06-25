package com.wanderingledger.core.data

import com.wanderingledger.core.model.Companion
import com.wanderingledger.core.model.CompanionRole
import kotlin.random.Random

data class EncounterOutcome(
    val encounterId: String,
    val resultText: String,
    val goldChange: Long = 0,
    val bondChange: Int = 0,
    val success: Boolean = true,
)

/**
 * Pure logic for resolving road encounters deterministically based on a seed.
 */
object EncounterEngine {
    fun resolve(
        seed: Long,
        encounterId: String,
        party: List<Companion>,
    ): EncounterOutcome {
        val random = Random(seed)

        return when (encounterId) {
            "merchant-cart" -> resolveMerchantCart(random, party)
            "fog-bank" -> resolveFogBank(random, party)
            "old-road" -> resolveOldRoad(random, party)
            "bandit-ambush" -> resolveBanditAmbush(random, party)
            else -> EncounterOutcome(encounterId, "Nothing unusual happened.")
        }
    }

    private fun resolveMerchantCart(
        random: Random,
        party: List<Companion>,
    ): EncounterOutcome {
        val rogue = party.firstOrNull { it.role == CompanionRole.Rogue && it.isActive }
        val bonus = if (rogue != null) 20 + (rogue.bondLevel * 4) else 0
        val roll = random.nextInt(100) + bonus

        return if (roll > 50) {
            EncounterOutcome(
                "merchant-cart",
                "You helped a merchant fix their wheel. They gave you some gold as thanks.",
                goldChange = 15,
            )
        } else {
            EncounterOutcome("merchant-cart", "You passed a merchant cart, but they were too busy to talk.")
        }
    }

    private fun resolveFogBank(
        random: Random,
        party: List<Companion>,
    ): EncounterOutcome {
        val hasScout = party.any { it.role == CompanionRole.Scout }
        val roll = random.nextInt(100)

        return if (hasScout || roll > 40) {
            EncounterOutcome("fog-bank", "Your party navigated the fog safely.")
        } else {
            EncounterOutcome(
                "fog-bank",
                "The fog was disorienting. You lost some time and some gold dropped from your pack.",
                goldChange = -10,
                success = false,
            )
        }
    }

    private fun resolveOldRoad(
        random: Random,
        party: List<Companion>,
    ): EncounterOutcome {
        val roll = random.nextInt(100)
        return if (roll > 70) {
            EncounterOutcome(
                "old-road",
                "You found an old cache of coins hidden in a hollow tree!",
                goldChange = 50,
            )
        } else {
            EncounterOutcome("old-road", "The old road was quiet and peaceful.")
        }
    }

    private fun resolveBanditAmbush(
        random: Random,
        party: List<Companion>,
    ): EncounterOutcome {
        val effectivePower = party.sumOf { it.combatPower + it.bondLevel }
        val roll = random.nextInt(100) + effectivePower * 2

        return if (roll > 70) {
            EncounterOutcome(
                encounterId = "bandit-ambush",
                resultText = "Bandits tried to ambush you, but your party drove them off!",
                bondChange = 1,
            )
        } else {
            EncounterOutcome(
                encounterId = "bandit-ambush",
                resultText = "You were ambushed by bandits and couldn't hold them off. They took what they could.",
                goldChange = -30,
                success = false,
            )
        }
    }
}
