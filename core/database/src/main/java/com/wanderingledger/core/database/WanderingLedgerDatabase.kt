package com.wanderingledger.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `orders` (
                    `orderId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `issuingTownId` INTEGER NOT NULL,
                    `destinationTownId` INTEGER NOT NULL,
                    `goodId` INTEGER NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `reputationReward` INTEGER NOT NULL,
                    `deadlineVisitsLeft` INTEGER NOT NULL,
                    `isActive` INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            // Seed rare goods for existing installs (new installs get these via SeedWorld)
            db.execSQL(
                "INSERT OR IGNORE INTO goods (goodId, name, baseValue, isContraband) " +
                    "VALUES (4, 'Medicines', 20, 0)",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO goods (goodId, name, baseValue, isContraband) " +
                    "VALUES (5, 'Dyes', 17, 0)",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO goods (goodId, name, baseValue, isContraband) " +
                    "VALUES (6, 'Charts', 22, 0)",
            )
            db.execSQL("INSERT OR IGNORE INTO town_produces (townId, goodId) VALUES (3, 4)")
            db.execSQL("INSERT OR IGNORE INTO town_produces (townId, goodId) VALUES (1, 5)")
            db.execSQL("INSERT OR IGNORE INTO town_produces (townId, goodId) VALUES (2, 6)")
            db.execSQL("INSERT OR IGNORE INTO town_demands (townId, goodId) VALUES (1, 4)")
            db.execSQL("INSERT OR IGNORE INTO town_demands (townId, goodId) VALUES (3, 5)")
            db.execSQL("INSERT OR IGNORE INTO town_demands (townId, goodId) VALUES (3, 6)")
            val tpCols =
                "townId, goodId, buyPrice, sellPrice, supplyLevel, lastUpdatedAt, minReputation"
            // Medicines: Mistfall(townId=3) source, Hearthwick(townId=1) destination
            db.execSQL(
                "INSERT OR IGNORE INTO town_prices ($tpCols) " +
                    "VALUES (3, 4, 8, 14, 'Abundant', 0, 60)",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO town_prices ($tpCols) " +
                    "VALUES (1, 4, 24, 40, 'Scarce', 0, 0)",
            )
            // Dyes: Hearthwick(townId=1) source, Mistfall(townId=3) destination
            db.execSQL(
                "INSERT OR IGNORE INTO town_prices ($tpCols) " +
                    "VALUES (1, 5, 7, 12, 'Abundant', 0, 60)",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO town_prices ($tpCols) " +
                    "VALUES (3, 5, 20, 32, 'Scarce', 0, 0)",
            )
            // Charts: Stoneford(townId=2) source, Mistfall(townId=3) destination
            db.execSQL(
                "INSERT OR IGNORE INTO town_prices ($tpCols) " +
                    "VALUES (2, 6, 9, 15, 'Abundant', 0, 60)",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO town_prices ($tpCols) " +
                    "VALUES (3, 6, 23, 37, 'Scarce', 0, 0)",
            )
        }
    }

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
        OrderEntity::class,
    ],
    version = 6,
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

    abstract fun orderDao(): OrderDao

    companion object {
        fun create(context: Context): WanderingLedgerDatabase =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    WanderingLedgerDatabase::class.java,
                    "wandering-ledger.db",
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
    }
}
