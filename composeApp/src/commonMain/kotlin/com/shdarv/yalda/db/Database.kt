@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.shdarv.yalda.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
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
    entities = [Profile::class, Category::class, WordEntry::class],
    version = 1, // Increment this when you change the schema
    exportSchema = true // Recommended to keep schema history
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wordEntryDao(): WordEntryDao
}


fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
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
