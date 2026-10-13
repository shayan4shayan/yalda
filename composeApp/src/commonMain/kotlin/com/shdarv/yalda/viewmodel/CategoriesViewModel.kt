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
            categoryDao.getCategoriesForProfile(profileId)
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


    fun setActiveProfileId(profileId: Long?) {
        _activeProfileId.value = profileId
    }

    fun addCategory(categoryName: String) {
        val profileId = _activeProfileId.value ?: return
        val normalizedName = categoryName.trim()
        if (normalizedName.isEmpty()) return
        viewModelScope.launch {
            val existingCategory = categoryDao.getCategoryByName(profileId, normalizedName)
            if (existingCategory == null) {
                val newCategory = Category(
                    name = normalizedName,
                    profileIdFk = profileId,
                    isUserModified = true
                )
                categoryDao.insertCategory(newCategory)
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.updateCategory(category.copy(name = category.name.trim(), isUserModified = true))
            // The flow will update
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }

    fun getCategoryById(categoryId: Long, callback: (Category?) -> Unit) {
        viewModelScope.launch {
            callback(categoryDao.getCategoryById(categoryId))
        }
    }
}
