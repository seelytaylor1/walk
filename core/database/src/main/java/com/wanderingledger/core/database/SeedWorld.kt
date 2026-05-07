package com.wanderingledger.core.database

import androidx.room.withTransaction

object SeedWorld {
    const val STARTING_TOWN_ID = 1L

    suspend fun ensureSeeded(database: WanderingLedgerDatabase, now: Long = System.currentTimeMillis()) {
        if (database.playerDao().getPlayerSnapshot() != null) return

        database.withTransaction {
            database.townDao().insertTowns(
                listOf(
                    TownEntity(1, "Hearthwick", "Greenway", 50, "visited", now),
                    TownEntity(2, "Stoneford", "Highpass", 50, "new", 0),
                    TownEntity(3, "Mistfall", "Lowmarsh", 50, "new", 0),
                ),
            )
            database.goodDao().insertGoods(
                listOf(
                    GoodEntity(1, "Apples", 8),
                    GoodEntity(2, "Iron", 18),
                    GoodEntity(3, "Silk", 30),
                ),
            )
            database.townDao().insertProducedGoods(
                listOf(
                    TownProducesEntity(1, 1),
                    TownProducesEntity(2, 2),
                    TownProducesEntity(3, 3),
                ),
            )
            database.townDao().insertDemandedGoods(
                listOf(
                    TownDemandsEntity(1, 2),
                    TownDemandsEntity(2, 3),
                    TownDemandsEntity(3, 1),
                ),
            )
            database.townPriceDao().upsertPrices(
                listOf(
                    TownPriceEntity(townId = 1, goodId = 1, buyPrice = 5, sellPrice = 8, supplyLevel = "Abundant", lastUpdatedAt = now),
                    TownPriceEntity(townId = 1, goodId = 2, buyPrice = 22, sellPrice = 30, supplyLevel = "Scarce", lastUpdatedAt = now),
                    TownPriceEntity(townId = 2, goodId = 2, buyPrice = 12, sellPrice = 18, supplyLevel = "Abundant", lastUpdatedAt = now),
                    TownPriceEntity(townId = 2, goodId = 3, buyPrice = 35, sellPrice = 44, supplyLevel = "Scarce", lastUpdatedAt = now),
                    TownPriceEntity(townId = 3, goodId = 3, buyPrice = 20, sellPrice = 30, supplyLevel = "Abundant", lastUpdatedAt = now),
                    TownPriceEntity(townId = 3, goodId = 1, buyPrice = 12, sellPrice = 16, supplyLevel = "Scarce", lastUpdatedAt = now),
                ),
            )
            database.roadSegmentDao().insertRoads(
                listOf(
                    RoadSegmentEntity(1, 1, 2, 120, "short", "[\"merchant-cart\"]"),
                    RoadSegmentEntity(2, 2, 1, 120, "short", "[\"merchant-cart\"]"),
                    RoadSegmentEntity(3, 2, 3, 180, "long", "[\"fog-bank\"]"),
                    RoadSegmentEntity(4, 3, 2, 180, "long", "[\"fog-bank\"]"),
                    RoadSegmentEntity(5, 1, 3, 240, "long", "[\"old-road\"]"),
                    RoadSegmentEntity(6, 3, 1, 240, "long", "[\"old-road\"]"),
                ),
            )
            database.companionDao().upsertCompanions(
                listOf(
                    CompanionEntity(1, "Mira", "Scout", 3, 0, "available", 1, false),
                    CompanionEntity(2, "Bram", "Fighter", 5, 0, "available", 2, false),
                ),
            )
            database.playerDao().upsertPlayer(
                PlayerStateEntity(
                    playerId = 1,
                    name = "Ledger Keeper",
                    playerClass = "Wanderer",
                    gold = 50,
                    currentTownId = STARTING_TOWN_ID,
                    inventorySlots = 12,
                    bankedSteps = 0,
                    lifetimeSteps = 0,
                    lastSyncAt = now,
                ),
            )
        }
    }
}
