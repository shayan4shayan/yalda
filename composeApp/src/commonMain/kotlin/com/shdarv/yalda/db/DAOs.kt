package com.shdarv.yalda.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.shdarv.yalda.models.DatasetImportSummary
import com.shdarv.yalda.models.DatasetPack
import com.shdarv.yalda.models.DatasetPackFile
import com.shdarv.yalda.models.DatasetImportPlanner
import com.shdarv.yalda.models.DatasetMergeAction

// --- DAOs ---

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Update
    suspend fun updateProfile(profile: Profile)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long): Int

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    suspend fun getAllProfilesOnce(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetupPreference(preference: ProfileSetupPreference)

    @Query("SELECT * FROM profile_setup_preferences WHERE profileIdFk = :profileId")
    fun getSetupPreference(profileId: Long): Flow<ProfileSetupPreference?>

    @Query("SELECT * FROM profile_setup_preferences")
    fun getAllSetupPreferences(): Flow<List<ProfileSetupPreference>>

    @Transaction
    suspend fun insertProfileWithSetup(
        profile: Profile,
        selectedDatasetPackIds: Set<String>,
        prepareOfflineTranslation: Boolean
    ): Long {
        val profileId = insertProfile(profile)
        insertSetupPreference(
            ProfileSetupPreference(
                profileIdFk = profileId,
                selectedDatasetPackIdsJson = Json.encodeToString(selectedDatasetPackIds.sorted()),
                prepareOfflineTranslation = prepareOfflineTranslation
            )
        )
        return profileId
    }

    @Transaction
    suspend fun deleteProfileUnlessLast(profileId: Long): Boolean {
        if (getProfileCount() <= 1) return false
        return deleteProfileById(profileId) > 0
    }
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if category name already exists
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE profileIdFk = :profileId ORDER BY name ASC")
    fun getCategoriesForProfile(profileId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE profileIdFk = :profileId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCategories(profileId: Long, query: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE profileIdFk = :profileId AND name = :name LIMIT 1")
    suspend fun getCategoryByName(profileId: Long, name: String): Category?

    @Query("SELECT * FROM categories WHERE profileIdFk = :profileId AND datasetPackId = :packId LIMIT 1")
    suspend fun getCategoryByDatasetPackId(profileId: Long, packId: String): Category?

    @Query("SELECT datasetPackId FROM categories WHERE profileIdFk = :profileId AND datasetPackId IS NOT NULL")
    suspend fun getInstalledDatasetPackIds(profileId: Long): List<String>
}

@Dao
interface WordEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) // Abort if word + profileId combination exists
    suspend fun insertWordEntry(wordEntry: WordEntry): Long

    @Update
    suspend fun updateWordEntry(wordEntry: WordEntry)

    @Delete
    suspend fun deleteWordEntry(wordEntry: WordEntry)

    @Query("DELETE FROM word_entries WHERE categoryIdFk = :categoryId")
    suspend fun deleteWordsByCategoryId(categoryId: Long)

    @Query("DELETE FROM word_entries WHERE profileIdFk = :profileId")
    suspend fun deleteWordsByProfileId(profileId: Long)

    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId ORDER BY word ASC")
    suspend fun getWordsForProfile(profileId: Long): List<WordEntry>

    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId ORDER BY word ASC")
    fun getWordsForProfileFlow(profileId: Long): Flow<List<WordEntry>>

    @Query("SELECT * FROM word_entries WHERE categoryIdFk = :categoryId AND profileIdFk = :profileId ORDER BY word ASC")
    fun getWordsForCategoryInProfile(categoryId: Long, profileId: Long): Flow<List<WordEntry>>

    @Query("SELECT * FROM word_entries WHERE categoryIdFk = :categoryId AND profileIdFk = :profileId ORDER BY word ASC")
    suspend fun getWordsForCategoryInProfileOnce(categoryId: Long, profileId: Long): List<WordEntry>

    @Query("SELECT * FROM word_entries WHERE categoryIdFk = :categoryId")
    suspend fun getWordsForCategory(categoryId: Long): List<WordEntry>

    @Query("SELECT * FROM word_entries WHERE wordId = :id")
    suspend fun getWordEntryById(id: Long): WordEntry?

    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId AND datasetPackId = :packId AND datasetEntryId = :entryId LIMIT 1")
    suspend fun getWordByDatasetEntry(profileId: Long, packId: String, entryId: String): WordEntry?

    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId AND word = :word COLLATE NOCASE")
    suspend fun getWordsByText(profileId: Long, word: String): List<WordEntry>

    @Query(
        "SELECT * FROM word_entries " +
            "WHERE profileIdFk = :profileId AND " +
            "(word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
            "ORDER BY word ASC"
    )
    fun searchWordsForProfile(profileId: Long, query: String): Flow<List<WordEntry>>

    @Query(
        "SELECT * FROM word_entries " +
            "WHERE profileIdFk = :profileId AND categoryIdFk = :categoryId AND " +
            "(word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
            "ORDER BY word ASC"
    )
    fun searchWordsForCategory(profileId: Long, categoryId: Long, query: String): Flow<List<WordEntry>>

    // Example of a query using the WordWithCategory relationship
    @Transaction // Recommended for queries spanning multiple tables
    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId ORDER BY categoryIdFk, word ASC")
    suspend fun getWordsWithCategoryForProfile(profileId: Long): List<WordWithCategory>

    @Transaction
    @Query("SELECT * FROM word_entries WHERE categoryIdFk = :categoryId AND profileIdFk = :profileId ORDER BY word ASC")
    fun getWordsWithCategoryForCategoryInProfile(categoryId: Long, profileId: Long): Flow<List<WordWithCategory>>

    @Query(
        "SELECT categoryIdFk as categoryId, COUNT(*) as wordCount " +
            "FROM word_entries WHERE profileIdFk = :profileId GROUP BY categoryIdFk"
    )
    suspend fun getWordCountsForProfile(profileId: Long): List<CategoryWordCount>

    @Insert
    suspend fun insertAll(wordEntries: List<WordEntry>)
}

@Dao
interface DatasetImportDao {
    @Query("SELECT * FROM categories WHERE profileIdFk = :profileId AND datasetPackId = :packId LIMIT 1")
    suspend fun getCategory(profileId: Long, packId: String): Category?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId AND datasetPackId = :packId AND datasetEntryId = :entryId LIMIT 1")
    suspend fun getDatasetWord(profileId: Long, packId: String, entryId: String): WordEntry?

    @Query("SELECT * FROM word_entries WHERE profileIdFk = :profileId AND word = :word COLLATE NOCASE")
    suspend fun getWordsByText(profileId: Long, word: String): List<WordEntry>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWord(word: WordEntry): Long

    @Update
    suspend fun updateWord(word: WordEntry)

    @Transaction
    suspend fun importPack(
        profileId: Long,
        catalogPack: DatasetPack,
        packFile: DatasetPackFile
    ): DatasetImportSummary {
        val existingCategory = getCategory(profileId, catalogPack.id)
        val categoryId = if (existingCategory == null) {
            insertCategory(
                Category(
                    name = packFile.category.title,
                    profileIdFk = profileId,
                    datasetPackId = catalogPack.id,
                    datasetEntryId = packFile.category.id,
                    installedDatasetVersion = catalogPack.version,
                    isUserModified = false
                )
            )
        } else {
            updateCategory(
                existingCategory.copy(
                    name = if (existingCategory.isUserModified) existingCategory.name else packFile.category.title,
                    datasetEntryId = packFile.category.id,
                    installedDatasetVersion = catalogPack.version
                )
            )
            existingCategory.id
        }

        var added = 0
        var updated = 0
        var unchanged = 0
        var preserved = 0
        var collisions = 0
        packFile.entries.forEach { entry ->
            val existing = getDatasetWord(profileId, catalogPack.id, entry.id)
            val action = DatasetImportPlanner.decide(
                existingDatasetEntryIsUserModified = existing?.isUserModified,
                hasUserOwnedWordCollision = existing == null &&
                    getWordsByText(profileId, entry.word).any { it.datasetPackId == null }
            )
            when (action) {
                DatasetMergeAction.PreserveUserEdit -> {
                    preserved += 1
                }
                DatasetMergeAction.UpdateDatasetEntry -> {
                    checkNotNull(existing)
                    val replacement = existing.copy(
                        word = entry.word,
                        meaning = entry.meaning,
                        description = entry.description,
                        categoryIdFk = categoryId,
                        installedDatasetVersion = catalogPack.version
                    )
                    if (replacement == existing) {
                        unchanged += 1
                    } else {
                        updateWord(replacement)
                        updated += 1
                    }
                }
                DatasetMergeAction.SkipUserOwnedCollision -> collisions += 1
                DatasetMergeAction.Insert -> {
                    insertWord(
                        WordEntry(
                            word = entry.word,
                            meaning = entry.meaning,
                            description = entry.description,
                            categoryIdFk = categoryId,
                            profileIdFk = profileId,
                            datasetPackId = catalogPack.id,
                            datasetEntryId = entry.id,
                            installedDatasetVersion = catalogPack.version,
                            isUserModified = false
                        )
                    )
                    added += 1
                }
            }
        }
        return DatasetImportSummary(added, updated, unchanged, preserved, collisions)
    }
}
