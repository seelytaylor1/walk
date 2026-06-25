package com.wanderingledger.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE town_prices ADD COLUMN minReputation INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE player_states ADD COLUMN completedTradesCount INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                UPDATE road_segments SET stepCost = CASE segmentId
                    WHEN 1 THEN 1000
                    WHEN 2 THEN 1000
                    WHEN 3 THEN 2500
                    WHEN 4 THEN 2500
                    WHEN 5 THEN 5000
                    WHEN 6 THEN 5000
                    ELSE stepCost
                END
                """.trimIndent(),
            )
            db.execSQL("UPDATE road_segments SET narrativeDistance = 'medium' WHERE segmentId IN (3, 4)")
        }
    }

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
    version = 5,
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
            Room
                .databaseBuilder(
                    context.applicationContext,
                    WanderingLedgerDatabase::class.java,
                    "wandering-ledger.db",
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
