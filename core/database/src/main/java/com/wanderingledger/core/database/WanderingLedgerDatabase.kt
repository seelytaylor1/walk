package com.wanderingledger.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TownEntity::class,
        GoodEntity::class,
        TownProducesEntity::class,
        TownDemandsEntity::class,
        TownPriceEntity::class,
        PlayerStateEntity::class,
        InventoryItemEntity::class,
        CompanionEntity::class,
        RoadSegmentEntity::class,
        RumorEntity::class,
        StepRecordEntity::class,
        EventLogEntity::class,
        PriceHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class WanderingLedgerDatabase : RoomDatabase() {
    abstract fun townDao(): TownDao
    abstract fun goodDao(): GoodDao
    abstract fun townPriceDao(): TownPriceDao
    abstract fun playerDao(): PlayerDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun companionDao(): CompanionDao
    abstract fun roadSegmentDao(): RoadSegmentDao
    abstract fun rumorDao(): RumorDao
    abstract fun stepRecordDao(): StepRecordDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun priceHistoryDao(): PriceHistoryDao

    companion object {
        fun create(context: Context): WanderingLedgerDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                WanderingLedgerDatabase::class.java,
                "wandering-ledger.db",
            ).build()
    }
}
