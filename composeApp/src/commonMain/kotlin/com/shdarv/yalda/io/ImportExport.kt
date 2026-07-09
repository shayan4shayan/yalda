package com.shdarv.yalda.io

import com.shdarv.yalda.db.AppDatabase
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.CategoryWithWordsDto
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.db.WordEntryDto
import com.shdarv.yalda.db.database
import kotlinx.coroutines.flow.last


expect class FileExporter {
    suspend fun exportCategory(categoryWithWords: CategoryWithWordsDto, fileName: String): Boolean
}

expect class FileImporter {
    suspend fun importCategory(fileName: String): CategoryWithWordsDto?
}

// Import your expect/actual FileExporter and FileImporter

class CategoryDataHandler(
    private val appDatabase: AppDatabase,
    private val fileExporter: FileExporter,
    private val fileImporter: FileImporter
) {

    // Function to prepare data for export
    suspend fun getCategoryWithWordsForExport(categoryId: Long): CategoryWithWordsDto? {
        val category = appDatabase.categoryDao().getCategoryById(categoryId) ?: return null
        val words = appDatabase.wordEntryDao().getWordsForCategory(categoryId = categoryId)

        val wordDtos = words.map { WordEntryDto(
            it.word, it.meaning,
            description = it.description
        ) }
        return CategoryWithWordsDto(category.name, wordDtos /*, other category fields */)
    }

    suspend fun exportCategoryToFile(categoryId: Long, fileName: String): Boolean {
        val dataToExport = getCategoryWithWordsForExport(categoryId) ?: return false
        return fileExporter.exportCategory(dataToExport, fileName)
    }

    // Function to process imported data
    suspend fun saveImportedCategory(profile: Profile, importedData: CategoryWithWordsDto) {
        // Create or update category
        var category = appDatabase.categoryDao().getCategoryByName(importedData.categoryName)
        val categoryId: Long
        if (category == null) {
            categoryId = appDatabase.categoryDao().insertCategory(Category(name = importedData.categoryName))
        } else {
            categoryId = category.id
            // Optionally update existing category details if needed
        }

        // Add words, potentially checking for duplicates or deciding on an update strategy
        val wordEntries = importedData.words.map {
            WordEntry(
                word = it.word,
                meaning = it.definition,
                description = it.description,
                categoryIdFk = categoryId,
                profileIdFk = profile.id,
            )
        }
        appDatabase.wordEntryDao().insertAll(wordEntries) // Ensure your DAO has insertAll
    }

    suspend fun importCategoryFromFile(profile: Profile, fileName: String): Boolean {
        val importedData = fileImporter.importCategory(fileName)
        return if (importedData != null) {
            saveImportedCategory(profile, importedData)
            true
        } else {
            false
        }
    }

    suspend fun hasCategoryByName(name: String): Boolean =
        appDatabase.categoryDao().getCategoryByName(name) != null
}

object ioDataHandler {
    private lateinit var handler: CategoryDataHandler

    fun init(fileExporter: FileExporter, fileImporter: FileImporter) {
        handler = CategoryDataHandler(database.get(), fileExporter, fileImporter)
    }

    fun get() : CategoryDataHandler = handler
}
