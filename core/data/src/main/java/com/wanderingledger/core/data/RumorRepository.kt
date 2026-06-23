package com.wanderingledger.core.data

import com.wanderingledger.core.database.RumorEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Rumor
import com.wanderingledger.core.model.SupplyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

/**
 * Repository for managing rumors and the player's ledger.
 */
class RumorRepository(
    private val database: WanderingLedgerDatabase,
) {
    /**
     * Observe all rumors that have not yet expired.
     */
    fun observeActiveRumors(): Flow<List<Rumor>> =
        database.rumorDao().listActiveRumors().map { entities ->
            entities.map { it.toModel() }
        }

    /**
     * Manually add a rumor (e.g., from a scripted event).
     */
    suspend fun addRumor(
        text: String,
        targetGoodId: Long? = null,
        sourceTownId: Long? = null,
        expiryVisits: Int = 3,
        isFalse: Boolean = false,
    ) {
        database.rumorDao().insertRumor(
            RumorEntity(
                text = text,
                targetGoodId = targetGoodId,
                sourceTownId = sourceTownId,
                createdAt = System.currentTimeMillis(),
                expiryVisitsLeft = expiryVisits,
                isFalse = isFalse,
            ),
        )
    }

    /**
     * Generate a rumor when visiting a town.
     * The rumor usually concerns prices in another town.
     *
     * @param seed seeds the [Random] used for every random choice, so the
     *   generated rumor is reproducible. Defaults to [System.nanoTime] for the
     *   non-deterministic behaviour callers relied on previously.
     */
    suspend fun generateRumorForTownVisit(
        visitedTownId: Long,
        seed: Long = System.nanoTime(),
    ) {
        val random = Random(seed)
        val allTowns = database.townDao().listTowns().first()
        val otherTowns = allTowns.filter { it.townId != visitedTownId }
        if (otherTowns.isEmpty()) return

        val sourceTown = otherTowns.random(random)
        val prices = database.townPriceDao().listPricesForTown(sourceTown.townId).first()
        if (prices.isEmpty()) return

        // Prefer abundant or scarce goods for "interesting" rumors
        val interestingPrices =
            prices.filter {
                it.supplyLevel == SupplyLevel.Abundant.name || it.supplyLevel == SupplyLevel.Scarce.name
            }

        val targetPrice =
            if (interestingPrices.isNotEmpty() && random.nextFloat() < 0.8f) {
                interestingPrices.random(random)
            } else {
                prices.random(random)
            }

        val good = database.goodDao().getGood(targetPrice.goodId).first() ?: return

        val isFalse = random.nextFloat() < 0.50f // 50% chance of a lie
        val supplyLevel =
            if (isFalse) {
                // Flip it or pick random
                val levels = SupplyLevel.entries.filter { it.name != targetPrice.supplyLevel }
                if (levels.isNotEmpty()) levels.random(random) else SupplyLevel.valueOf(targetPrice.supplyLevel)
            } else {
                SupplyLevel.valueOf(targetPrice.supplyLevel)
            }

        val text =
            when (supplyLevel) {
                SupplyLevel.Abundant ->
                    "A traveler mentions that ${good.name} is plentiful in ${sourceTown.name} right now."
                SupplyLevel.Scarce -> "Someone complains about the lack of ${good.name} in ${sourceTown.name}."
                SupplyLevel.Normal -> "You hear that trade for ${good.name} is steady in ${sourceTown.name}."
            }

        addRumor(
            text = text,
            targetGoodId = good.goodId,
            sourceTownId = sourceTown.townId,
            expiryVisits = 2,
            isFalse = isFalse,
        )
    }

    /**
     * Generate a rumor from a road event.
     *
     * @param seed seeds the [Random] used to pick which road event the rumor
     *   describes, so the result is reproducible. Defaults to [System.nanoTime].
     */
    suspend fun generateRumorFromRoadEvent(
        segmentId: Long,
        seed: Long = System.nanoTime(),
    ) {
        val random = Random(seed)
        val road = database.roadSegmentDao().getRoadSnapshot(segmentId) ?: return
        val eventPool =
            try {
                // Simple JSON array parsing or just pick from the string if it's simple
                road.eventPool
                    .trim('[', ']')
                    .split(',')
                    .map { it.trim(' ', '"') }
            } catch (e: Exception) {
                emptyList()
            }

        if (eventPool.isEmpty()) return
        val event = eventPool.random(random)

        val text =
            when (event) {
                "merchant-cart" ->
                    "You passed a merchant cart whose driver mentioned a shortcut near ${road.narrativeDistance}."
                "fog-bank" -> "A traveler in the fog whispered about strange lights in the marsh."
                "old-road" -> "You found a carved stone on the old road that speaks of hidden wealth."
                else -> "A strange occurrence on the road leaves you with a lingering thought."
            }

        addRumor(
            text = text,
            expiryVisits = 5, // Road rumors last longer
        )
    }
}

private fun RumorEntity.toModel(): Rumor =
    Rumor(
        rumorId = rumorId,
        text = text,
        targetGoodId = targetGoodId,
        sourceTownId = sourceTownId,
        createdAt = createdAt,
        expiryVisitsLeft = expiryVisitsLeft,
        isFalse = isFalse,
    )
