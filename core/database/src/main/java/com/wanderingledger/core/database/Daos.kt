package com.wanderingledger.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TownDao {
    @Query("SELECT * FROM towns WHERE townId = :id")
    fun getTown(id: Long): Flow<TownEntity?>

    @Query("SELECT * FROM towns WHERE townId = :id")
    suspend fun getTownSnapshot(id: Long): TownEntity?

    @Query("SELECT * FROM towns ORDER BY townId")
    fun listTowns(): Flow<List<TownEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTown(town: TownEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTowns(towns: List<TownEntity>)

    @Update
    suspend fun updateTown(town: TownEntity)

    @Query("SELECT goodId FROM town_produces WHERE townId = :townId")
    fun listProducedGoods(townId: Long): Flow<List<Long>>

    @Query("SELECT goodId FROM town_demands WHERE townId = :townId")
    fun listDemandedGoods(townId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducedGoods(goods: List<TownProducesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDemandedGoods(goods: List<TownDemandsEntity>)

    @Query(
        "SELECT t.* FROM towns t JOIN town_demands td ON t.townId = td.townId " +
            "WHERE td.goodId = :goodId AND t.townId != :excludeTownId",
    )
    suspend fun getTownsDemanding(
        goodId: Long,
        excludeTownId: Long,
    ): List<TownEntity>

    @Query("UPDATE towns SET reputation = MIN(reputation + :amount, 100) WHERE townId = :townId")
    suspend fun addReputation(
        townId: Long,
        amount: Int,
    )
}

@Dao
interface GoodDao {
    @Query("SELECT * FROM goods WHERE goodId = :id")
    fun getGood(id: Long): Flow<GoodEntity?>

    @Query("SELECT * FROM goods WHERE goodId = :id")
    suspend fun getGoodSnapshot(id: Long): GoodEntity?

    @Query("SELECT * FROM goods ORDER BY goodId")
    fun listGoods(): Flow<List<GoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoods(goods: List<GoodEntity>)
}

@Dao
interface TownPriceDao {
    @Query("SELECT * FROM town_prices WHERE townId = :townId AND goodId = :goodId")
    fun getPrice(
        townId: Long,
        goodId: Long,
    ): Flow<TownPriceEntity?>

    @Query("SELECT * FROM town_prices WHERE townId = :townId ORDER BY goodId")
    fun listPricesForTown(townId: Long): Flow<List<TownPriceEntity>>

    @Query("SELECT * FROM town_prices WHERE townId = :townId ORDER BY goodId")
    suspend fun listPricesSnapshotForTown(townId: Long): List<TownPriceEntity>

    @Upsert
    suspend fun upsertPrice(price: TownPriceEntity)

    @Upsert
    suspend fun upsertPrices(prices: List<TownPriceEntity>)

    @Update
    suspend fun updatePrice(price: TownPriceEntity)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_states WHERE playerId = 1")
    fun getPlayer(): Flow<PlayerStateEntity?>

    @Query("SELECT * FROM player_states WHERE playerId = 1")
    suspend fun getPlayerSnapshot(): PlayerStateEntity?

    @Upsert
    suspend fun upsertPlayer(player: PlayerStateEntity)

    @Update
    suspend fun updatePlayer(player: PlayerStateEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE playerId = :playerId ORDER BY id")
    fun listInventory(playerId: Long): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun removeItem(itemId: Long)

    @Query("SELECT * FROM inventory_items WHERE playerId = :playerId AND goodId = :goodId LIMIT 1")
    suspend fun getItemSnapshot(
        playerId: Long,
        goodId: Long,
    ): InventoryItemEntity?

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query(
        "SELECT i.* FROM inventory_items i " +
            "JOIN goods g ON i.goodId = g.goodId " +
            "WHERE i.playerId = :playerId AND g.isContraband = 1",
    )
    suspend fun listContrabandItemsSnapshot(playerId: Long): List<InventoryItemEntity>
}

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companions WHERE isActive = 1 ORDER BY companionId")
    fun listActiveCompanions(): Flow<List<CompanionEntity>>

    @Query("SELECT * FROM companions WHERE isActive = 1 ORDER BY companionId")
    suspend fun listActiveCompanionsSnapshot(): List<CompanionEntity>

    @Query("SELECT * FROM companions WHERE isActive = 0 ORDER BY companionId")
    fun listRecruitableCompanions(): Flow<List<CompanionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompanion(companion: CompanionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompanions(companions: List<CompanionEntity>)
}

@Dao
interface RoadSegmentDao {
    @Query("SELECT * FROM road_segments ORDER BY segmentId")
    fun listRoads(): Flow<List<RoadSegmentEntity>>

    @Query("SELECT * FROM road_segments WHERE fromTownId = :townId ORDER BY segmentId")
    fun listRoadsFrom(townId: Long): Flow<List<RoadSegmentEntity>>

    @Query("SELECT * FROM road_segments WHERE segmentId = :segmentId")
    fun getRoad(segmentId: Long): Flow<RoadSegmentEntity?>

    @Query("SELECT * FROM road_segments WHERE segmentId = :segmentId")
    suspend fun getRoadSnapshot(segmentId: Long): RoadSegmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoads(roads: List<RoadSegmentEntity>)
}

@Dao
interface RumorDao {
    @Query("SELECT * FROM rumors WHERE expiryVisitsLeft > 0 ORDER BY createdAt DESC")
    fun listActiveRumors(): Flow<List<RumorEntity>>

    @Insert
    suspend fun insertRumor(rumor: RumorEntity)

    @Query("UPDATE rumors SET expiryVisitsLeft = MAX(expiryVisitsLeft - 1, 0) WHERE rumorId = :rumorId")
    suspend fun decrementExpiry(rumorId: Long)

    @Query("UPDATE rumors SET expiryVisitsLeft = MAX(expiryVisitsLeft - 1, 0) WHERE expiryVisitsLeft > 0")
    suspend fun decrementAllActive()
}

@Dao
interface StepRecordDao {
    @Insert
    suspend fun insertRecord(record: StepRecordEntity)

    @Query("SELECT * FROM step_records WHERE dateEpoch = :dateEpoch ORDER BY recordId DESC LIMIT 1")
    fun getStepsForDate(dateEpoch: Long): Flow<StepRecordEntity?>
}

@Dao
interface EventLogDao {
    @Insert
    suspend fun insertEvent(event: EventLogEntity)

    @Query("SELECT * FROM event_logs ORDER BY createdAt DESC LIMIT :limit")
    fun listRecentEvents(limit: Int): Flow<List<EventLogEntity>>
}

@Dao
interface OrderDao {
    @Query("SELECT COUNT(*) FROM orders WHERE issuingTownId = :townId AND isActive = 1")
    suspend fun countActiveOrdersForTown(townId: Long): Int

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET isActive = 0 WHERE orderId = :orderId")
    suspend fun deactivateOrder(orderId: Long)

    @Query(
        "UPDATE orders SET deadlineVisitsLeft = deadlineVisitsLeft - 1 " +
            "WHERE isActive = 1 AND deadlineVisitsLeft > 0",
    )
    suspend fun decrementAllActiveDeadlines()

    @Query("UPDATE orders SET isActive = 0 WHERE isActive = 1 AND deadlineVisitsLeft <= 0")
    suspend fun expireOverdueOrders()

    @Query("SELECT * FROM orders WHERE isActive = 1 ORDER BY orderId")
    suspend fun getActiveOrdersSnapshot(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE issuingTownId = :townId AND isActive = 1 ORDER BY orderId")
    suspend fun getActiveOrdersForTownSnapshot(townId: Long): List<OrderEntity>
}

@Dao
interface PriceHistoryDao {
    /**
     * Insert a new price snapshot for a (townId, goodId) pair.
     */
    @Insert
    suspend fun insertSnapshot(snapshot: PriceHistoryEntity)

    /**
     * Return the most recent [limit] snapshots for a given town/good pair, newest first.
     */
    @Query(
        "SELECT * FROM price_history WHERE townId = :townId AND goodId = :goodId " +
            "ORDER BY recordedAt DESC LIMIT :limit",
    )
    fun listHistory(
        townId: Long,
        goodId: Long,
        limit: Int = PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN,
    ): Flow<List<PriceHistoryEntity>>

    /**
     * Snapshot version for use inside transactions.
     */
    @Query(
        "SELECT * FROM price_history WHERE townId = :townId AND goodId = :goodId " +
            "ORDER BY recordedAt DESC LIMIT :limit",
    )
    suspend fun listHistorySnapshot(
        townId: Long,
        goodId: Long,
        limit: Int = PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN,
    ): List<PriceHistoryEntity>

    /**
     * Count of snapshots for a (townId, goodId) pair — used for trimming.
     */
    @Query("SELECT COUNT(*) FROM price_history WHERE townId = :townId AND goodId = :goodId")
    suspend fun countHistory(
        townId: Long,
        goodId: Long,
    ): Int

    /**
     * Delete the oldest snapshot(s) beyond the keep limit.
     */
    @Query(
        "DELETE FROM price_history WHERE id IN (" +
            "SELECT id FROM price_history WHERE townId = :townId AND goodId = :goodId " +
            "ORDER BY recordedAt ASC LIMIT :deleteCount" +
            ")",
    )
    suspend fun trimOldest(
        townId: Long,
        goodId: Long,
        deleteCount: Int,
    )
}
