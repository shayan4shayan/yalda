package com.shdarv.yalda.db

// In a new file, perhaps CategoryTransfer.kt or similar in your commonMain
import kotlinx.serialization.Serializable

@Serializable
data class WordEntryDto(
    val word: String,
    val definition: String,
    val description: String?,
)

@Serializable
data class CategoryWithWordsDto(
    val categoryName: String,
    val words: List<WordEntryDto>
)
