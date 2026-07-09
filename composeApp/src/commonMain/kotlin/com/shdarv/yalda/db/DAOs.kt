package com.shdarv.yalda.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- DAOs ---

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): Profile?
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if category name already exists
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCategories(query: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
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

    @Insert()
    suspend fun insertAll(wordEntries: List<WordEntry>)
}
