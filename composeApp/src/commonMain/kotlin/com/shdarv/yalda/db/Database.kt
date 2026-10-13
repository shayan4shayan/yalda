@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.shdarv.yalda.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized


expect object AppDatabaseCtor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

@ConstructedBy(AppDatabaseCtor::class)
@Database(
    entities = [Profile::class, Category::class, WordEntry::class, ProfileSetupPreference::class, AuditEvent::class],
    version = 3,
    exportSchema = true // Recommended to keep schema history
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wordEntryDao(): WordEntryDao
    abstract fun datasetImportDao(): DatasetImportDao
}


fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .addCallback(AUDIT_CALLBACK)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

/**
 * Makes legacy global categories profile-specific without losing any words. A category is copied
 * once for every profile that referenced it. Empty categories are assigned to the oldest profile.
 * Legacy words deliberately receive no dataset provenance, so updates treat them as user-owned.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `profileIdFk` INTEGER NOT NULL,
                `datasetPackId` TEXT,
                `datasetEntryId` TEXT,
                `installedDatasetVersion` TEXT,
                `isUserModified` INTEGER NOT NULL,
                FOREIGN KEY(`profileIdFk`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE TEMP TABLE `category_profile_map` AS
            SELECT
                legacyCategoryId,
                profileId,
                ROW_NUMBER() OVER (ORDER BY legacyCategoryId, profileId) AS newCategoryId
            FROM (
                SELECT c.id AS legacyCategoryId, w.profileIdFk AS profileId
                FROM categories c
                JOIN word_entries w ON w.categoryIdFk = c.id
                GROUP BY c.id, w.profileIdFk
                UNION
                SELECT c.id AS legacyCategoryId, (SELECT MIN(id) FROM profiles) AS profileId
                FROM categories c
                WHERE NOT EXISTS (
                    SELECT 1 FROM word_entries w WHERE w.categoryIdFk = c.id
                ) AND EXISTS (SELECT 1 FROM profiles)
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO categories_new (
                id, name, profileIdFk, datasetPackId, datasetEntryId,
                installedDatasetVersion, isUserModified
            )
            SELECT m.newCategoryId, c.name, m.profileId, NULL, NULL, NULL, 1
            FROM category_profile_map m
            JOIN categories c ON c.id = m.legacyCategoryId
            """.trimIndent()
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `word_entries_new` (
                `wordId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `word` TEXT NOT NULL,
                `meaning` TEXT NOT NULL,
                `description` TEXT,
                `categoryIdFk` INTEGER NOT NULL,
                `profileIdFk` INTEGER NOT NULL,
                `datasetPackId` TEXT,
                `datasetEntryId` TEXT,
                `installedDatasetVersion` TEXT,
                `isUserModified` INTEGER NOT NULL,
                FOREIGN KEY(`categoryIdFk`) REFERENCES `categories_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`profileIdFk`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO word_entries_new (
                wordId, word, meaning, description, categoryIdFk, profileIdFk,
                datasetPackId, datasetEntryId, installedDatasetVersion, isUserModified
            )
            SELECT w.wordId, w.word, w.meaning, w.description, m.newCategoryId, w.profileIdFk,
                NULL, NULL, NULL, 1
            FROM word_entries w
            JOIN category_profile_map m
              ON m.legacyCategoryId = w.categoryIdFk AND m.profileId = w.profileIdFk
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE word_entries")
        connection.execSQL("DROP TABLE categories")
        connection.execSQL("ALTER TABLE categories_new RENAME TO categories")
        connection.execSQL("ALTER TABLE word_entries_new RENAME TO word_entries")
        connection.execSQL("DROP TABLE category_profile_map")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_categories_profileIdFk ON categories(profileIdFk)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_categories_profileIdFk_name ON categories(profileIdFk, name)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_categories_profileIdFk_datasetPackId ON categories(profileIdFk, datasetPackId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_word_entries_categoryIdFk ON word_entries(categoryIdFk)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_word_entries_profileIdFk ON word_entries(profileIdFk)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_word_entries_word_profileIdFk ON word_entries(word, profileIdFk)")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `profile_setup_preferences` (
                `profileIdFk` INTEGER NOT NULL,
                `selectedDatasetPackIdsJson` TEXT NOT NULL,
                `prepareOfflineTranslation` INTEGER NOT NULL,
                PRIMARY KEY(`profileIdFk`),
                FOREIGN KEY(`profileIdFk`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_profile_setup_preferences_profileIdFk ON profile_setup_preferences(profileIdFk)")
        connection.execSQL(
            """
            INSERT OR IGNORE INTO profile_setup_preferences (
                profileIdFk, selectedDatasetPackIdsJson, prepareOfflineTranslation
            ) SELECT id, '[]', 0 FROM profiles
            """.trimIndent()
        )
    }
}

data class LegacyWordOwner(val categoryId: Long, val profileId: Long)

/** Mirrors the ownership projection used by [MIGRATION_1_2] and is kept pure for host tests. */
fun projectLegacyCategoryOwners(
    profileIdsInCreationOrder: List<Long>,
    legacyCategoryIds: List<Long>,
    wordOwners: List<LegacyWordOwner>
): Set<LegacyWordOwner> {
    val oldestProfileId = profileIdsInCreationOrder.minOrNull()
    return legacyCategoryIds.flatMap { categoryId ->
        val usingProfiles = wordOwners.filter { it.categoryId == categoryId }.map { it.profileId }.distinct()
        when {
            usingProfiles.isNotEmpty() -> usingProfiles.map { LegacyWordOwner(categoryId, it) }
            oldestProfileId != null -> listOf(LegacyWordOwner(categoryId, oldestProfileId))
            else -> emptyList()
        }
    }.toSet()
}


object database {
    private var instance: AppDatabase? = null
    @OptIn(InternalCoroutinesApi::class)
    private val lock = SynchronizedObject()

    fun isInitialized(): Boolean = instance != null

    @OptIn(InternalCoroutinesApi::class)
    fun init(builder: RoomDatabase.Builder<AppDatabase>) {
        if (instance == null) {
            synchronized(lock) {
                if (instance == null) {
                    instance = getRoomDatabase(builder)
                }
            }
        }
    }

    fun get(): AppDatabase =
        instance ?: error("AppDatabase not initialized. Call appDatabase.init(builder) first.")

    @OptIn(InternalCoroutinesApi::class)
    fun destroy() {
        synchronized(lock) {
            instance?.close()
            instance = null
        }
    }

}
