package com.shdarv.yalda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.CategoryDao
import com.shdarv.yalda.db.WordEntryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    private val categoryDao: CategoryDao,
    private val wordEntryDao: WordEntryDao // To delete words when a category is deleted
) : ViewModel() {

    // This will hold the ID of the currently active profile
    private val _activeProfileId = MutableStateFlow<Long?>(null)
    val activeProfileId: StateFlow<Long?> = _activeProfileId.asStateFlow()

    // Categories for the current active profile
    val categoriesForProfile: StateFlow<List<Category>> = _activeProfileId
        .filterNotNull() // Only proceed if profileId is not null
        .flatMapLatest { profileId ->
            // For categories, they are not directly tied to a profile in the Category table.
            // We assume categories are global, but words are tied to profile+category.
            // So, we'll just load all categories for now.
            // If you intend categories to be profile-specific, you'd need a profileIdFk in Category entity.
            // For now, let's assume we show ALL categories and filter words later.
            categoryDao.getAllCategories()
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordCountsForProfile: StateFlow<Map<Long, Int>> = _activeProfileId
        .filterNotNull()
        .flatMapLatest { profileId ->
            wordEntryDao.getWordsForProfileFlow(profileId).map { words ->
                words.groupingBy { it.categoryIdFk }.eachCount()
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // If you actually want categories to be filterable or tied to a profile (e.g. show only categories
    // that HAVE words for the current profile), your Category entity or queries would need adjustments.
    // For now, this loads all globally defined categories.

    fun setActiveProfileId(profileId: Long?) {
        _activeProfileId.value = profileId
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            // Check if category already exists to avoid duplicates if your DAO uses IGNORE
            val existingCategory = categoryDao.getCategoryByName(categoryName)
            if (existingCategory == null) {
                val newCategory = Category(name = categoryName)
                categoryDao.insertCategory(newCategory)
                // The flow from getAllCategories() will update
            } else {
                // Handle case where category already exists (e.g., show a message)
                println("Category '$categoryName' already exists.")
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.updateCategory(category)
            // The flow will update
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            wordEntryDao.deleteWordsByCategoryId(category.id)
            categoryDao.deleteCategory(category) // Then delete the category itself
            // The flow will update
        }
    }

    fun getCategoryById(categoryId: Long, callback: (Category?) -> Unit) {
        viewModelScope.launch {
            callback(categoryDao.getCategoryById(categoryId))
        }
    }
}
