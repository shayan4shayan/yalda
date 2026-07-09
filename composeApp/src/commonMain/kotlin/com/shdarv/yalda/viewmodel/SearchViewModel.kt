package com.shdarv.yalda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.CategoryDao
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.db.WordEntryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val categoryDao: CategoryDao,
    private val wordEntryDao: WordEntryDao
) : ViewModel() {
    private val _activeProfileId = MutableStateFlow<Long?>(null)
    private val _query = MutableStateFlow("")

    val query: StateFlow<String> = _query.asStateFlow()

    val categories: StateFlow<List<Category>> = _query
        .flatMapLatest { query ->
            if (query.isBlank()) {
                categoryDao.getAllCategories()
            } else {
                categoryDao.searchCategories(query.trim())
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val words: StateFlow<List<WordEntry>> = combine(
        _activeProfileId.filterNotNull(),
        _query
    ) { profileId, query ->
        Pair(profileId, query.trim())
    }.flatMapLatest { (profileId, query) ->
        if (query.isBlank()) {
            wordEntryDao.getWordsForProfileFlow(profileId)
        } else {
            wordEntryDao.searchWordsForProfile(profileId, query)
        }
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActiveProfile(profileId: Long?) {
        _activeProfileId.value = profileId
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }
}
