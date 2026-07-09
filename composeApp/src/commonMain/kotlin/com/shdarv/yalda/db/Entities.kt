package com.shdarv.yalda.db

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles"
)
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sourceLanguage: String,
    val targetLanguage: String
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = false)]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "word_entries",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryIdFk"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryIdFk"]),
        Index(value = ["profileIdFk"]),
        Index(value = ["word", "profileIdFk"], unique = false)
    ]
)
data class WordEntry(
    @PrimaryKey(autoGenerate = true)
    val wordId: Long = 0,
    val word: String,
    val meaning: String,
    val description: String?,
    val categoryIdFk: Long,
    val profileIdFk: Long
)

data class WordWithCategory(
    @Embedded val wordEntry: WordEntry,
    @Relation(
        parentColumn = "categoryIdFk",
        entityColumn = "id"
    )
    val category: Category
)

data class CategoryWordCount(
    val categoryId: Long,
    val wordCount: Int
)
