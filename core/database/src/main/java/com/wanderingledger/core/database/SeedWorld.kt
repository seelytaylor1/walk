package com.wanderingledger.core.database

import androidx.room.withTransaction

object SeedWorld {
    const val STARTING_TOWN_ID = 1L

    suspend fun ensureSeeded(
        database: WanderingLedgerDatabase,
        now: Long = System.currentTimeMillis(),
    ) {
        if (database.playerDao().getPlayerSnapshot() != null) return

        database.withTransaction {
            database.townDao().insertTowns(
                listOf(
                    TownEntity(1, "Hearthwick", "Greenway", "Forest", 50, "visited", now),
                    TownEntity(2, "Stoneford", "Highpass", "Mountain", 50, "new", 0),
                    TownEntity(3, "Mistfall", "Lowmarsh", "Swamp", 50, "new", 0),
                ),
            )
            database.goodDao().insertGoods(
                listOf(
                    GoodEntity(1, "Apples", 8),
                    GoodEntity(2, "Iron", 18),
                    GoodEntity(3, "Silk", 30),
                    // Rare goods: rep-gated, require Reputation ≥ 60 to appear at source town
                    GoodEntity(4, "Medicines", 20),
                    GoodEntity(5, "Dyes", 17),
                    GoodEntity(6, "Charts", 22),
                ),
            )
            database.townDao().insertProducedGoods(
                listOf(
                    TownProducesEntity(1, 1), // Hearthwick → Apples
                    TownProducesEntity(2, 2), // Stoneford → Iron
                    TownProducesEntity(3, 3), // Mistfall → Silk
                    TownProducesEntity(3, 4), // Mistfall → Medicines
                    TownProducesEntity(1, 5), // Hearthwick → Dyes
                    TownProducesEntity(2, 6), // Stoneford → Charts
                ),
            )
            database.townDao().insertDemandedGoods(
                listOf(
                    TownDemandsEntity(1, 2), // Hearthwick demands Iron
                    TownDemandsEntity(2, 3), // Stoneford demands Silk
                    TownDemandsEntity(3, 1), // Mistfall demands Apples
                    TownDemandsEntity(1, 4), // Hearthwick demands Medicines
                    TownDemandsEntity(3, 5), // Mistfall demands Dyes
                    TownDemandsEntity(3, 6), // Mistfall demands Charts
                ),
            )
            database.townPriceDao().upsertPrices(
                listOf(
                    // Hearthwick (1)
                    // Apples (1), Base 8, Abundant: Sell=5, Buy=3
                    TownPriceEntity(
                        townId = 1,
                        goodId = 1,
                        buyPrice = 3,
                        sellPrice = 5,
                        supplyLevel = "Abundant",
                        lastUpdatedAt = now,
                    ),
                    // Iron (2), Base 18, Scarce: Sell=27, Buy=17
                    TownPriceEntity(
                        townId = 1,
                        goodId = 2,
                        buyPrice = 17,
                        sellPrice = 27,
                        supplyLevel = "Scarce",
                        lastUpdatedAt = now,
                    ),
                    // Stoneford (2)
                    // Iron (2), Base 18, Abundant: Sell=12, Buy=7
                    TownPriceEntity(
                        townId = 2,
                        goodId = 2,
                        buyPrice = 7,
                        sellPrice = 12,
                        supplyLevel = "Abundant",
                        lastUpdatedAt = now,
                    ),
                    // Silk (3), Base 30, Scarce: Sell=45, Buy=29
                    TownPriceEntity(
                        townId = 2,
                        goodId = 3,
                        buyPrice = 29,
                        sellPrice = 45,
                        supplyLevel = "Scarce",
                        lastUpdatedAt = now,
                    ),
                    // Mistfall (3)
                    // Silk (3), Base 30, Abundant: Sell=21, Buy=13
                    TownPriceEntity(
                        townId = 3,
                        goodId = 3,
                        buyPrice = 13,
                        sellPrice = 21,
                        supplyLevel = "Abundant",
                        lastUpdatedAt = now,
                    ),
                    // Apples (1), Base 8, Scarce: Sell=12, Buy=7
                    TownPriceEntity(
                        townId = 3,
                        goodId = 1,
                        buyPrice = 7,
                        sellPrice = 12,
                        supplyLevel = "Scarce",
                        lastUpdatedAt = now,
                    ),
                    // Medicines (4): Mistfall source (rep-gated), Hearthwick destination
                    TownPriceEntity(
                        townId = 3,
                        goodId = 4,
                        buyPrice = 8,
                        sellPrice = 14,
                        supplyLevel = "Abundant",
                        lastUpdatedAt = now,
                        minReputation = 60,
                    ),
                    TownPriceEntity(
                        townId = 1,
                        goodId = 4,
                        buyPrice = 24,
                        sellPrice = 40,
                        supplyLevel = "Scarce",
                        lastUpdatedAt = now,
                        minReputation = 0,
                    ),
                    // Dyes (5): Hearthwick source (rep-gated), Mistfall destination
                    TownPriceEntity(
                        townId = 1,
                        goodId = 5,
                        buyPrice = 7,
                        sellPrice = 12,
                        supplyLevel = "Abundant",
                        lastUpdatedAt = now,
                        minReputation = 60,
                    ),
                    TownPriceEntity(
                        townId = 3,
                        goodId = 5,
                        buyPrice = 20,
                        sellPrice = 32,
                        supplyLevel = "Scarce",
                        lastUpdatedAt = now,
                        minReputation = 0,
                    ),
                    // Charts (6): Stoneford source (rep-gated), Mistfall destination
                    TownPriceEntity(
                        townId = 2,
                        goodId = 6,
                        buyPrice = 9,
                        sellPrice = 15,
                        supplyLevel = "Abundant",
                        lastUpdatedAt = now,
                        minReputation = 60,
                    ),
                    TownPriceEntity(
                        townId = 3,
                        goodId = 6,
                        buyPrice = 23,
                        sellPrice = 37,
                        supplyLevel = "Scarce",
                        lastUpdatedAt = now,
                        minReputation = 0,
                    ),
                ),
            )
            database.roadSegmentDao().insertRoads(
                listOf(
                    RoadSegmentEntity(1, 1, 2, 1000, "short", "[\"merchant-cart\"]"),
                    RoadSegmentEntity(2, 2, 1, 1000, "short", "[\"merchant-cart\"]"),
                    RoadSegmentEntity(3, 2, 3, 2500, "medium", "[\"fog-bank\"]"),
                    RoadSegmentEntity(4, 3, 2, 2500, "medium", "[\"fog-bank\"]"),
                    RoadSegmentEntity(5, 1, 3, 5000, "long", "[\"old-road\",\"bandit-ambush\"]"),
                    RoadSegmentEntity(6, 3, 1, 5000, "long", "[\"old-road\",\"bandit-ambush\"]"),
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
