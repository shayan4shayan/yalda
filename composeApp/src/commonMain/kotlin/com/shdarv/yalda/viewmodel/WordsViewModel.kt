package com.shdarv.yalda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.db.WordEntryDao
import com.shdarv.yalda.db.WordWithCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

@OptIn(ExperimentalCoroutinesApi::class)
class WordsViewModel(
    private val wordEntryDao: WordEntryDao
) : ViewModel() {

    private val _activeProfileId = MutableStateFlow<Long?>(null)
    private val _activeCategoryId = MutableStateFlow<Long?>(null)

    val wordsForProfile: StateFlow<List<WordEntry>> = _activeProfileId
        .filterNotNull()
        .flatMapLatest { profileId ->
            wordEntryDao.getWordsForProfileFlow(profileId)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordsInCategoryForProfile: StateFlow<List<WordEntry>> = combine(
        _activeProfileId.filterNotNull(), _activeCategoryId.filterNotNull()
    ) { profileId, categoryId ->
        Pair(profileId, categoryId)
    }.flatMapLatest { (profileId, categoryId) ->
            wordEntryDao.getWordsForCategoryInProfile(categoryId, profileId)
        }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // If you want to display words with their category names directly
    val wordsWithCategoryForProfile: StateFlow<List<WordWithCategory>> = combine(
        _activeProfileId.filterNotNull(), _activeCategoryId.filterNotNull()
    ) { profileId, categoryId ->
        Pair(profileId, categoryId)
    }.flatMapLatest { (profileId, categoryId) ->
            wordEntryDao.getWordsWithCategoryForCategoryInProfile(
                categoryId = categoryId, profileId = profileId
            )
        }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun setActiveProfileAndCategory(profileId: Long?, categoryId: Long?) {
        _activeProfileId.value = profileId
        _activeCategoryId.value = categoryId
    }

    fun setActiveProfile(profileId: Long?) {
        _activeProfileId.value = profileId
    }

    fun setActiveCategory(categoryId: Long?) {
        _activeCategoryId.value = categoryId
    }

    suspend fun getWordsForQuiz(profileId: Long, categoryId: Long?): List<WordEntry> {
        return if (categoryId == null) {
            wordEntryDao.getWordsForProfile(profileId)
        } else {
            wordEntryDao.getWordsForCategoryInProfileOnce(categoryId, profileId)
        }
    }

    fun addWordEntry(
        word: String,
        meaning: String,
        description: String?,
        categoryIdOverride: Long? = null,
        profileIdOverride: Long? = null
    ) {
        val currentProfileId = profileIdOverride ?: _activeProfileId.value
        val currentCategoryId = categoryIdOverride ?: _activeCategoryId.value

        if (currentProfileId == null || currentCategoryId == null) {
            // Handle error: profile or category not selected
            println("Error: Profile or Category not selected to add word.")
            return
        }

        viewModelScope.launch {
            val newWordEntry = WordEntry(
                word = word,
                meaning = meaning,
                description = description,
                categoryIdFk = currentCategoryId,
                profileIdFk = currentProfileId
            )
            try {
                wordEntryDao.insertWordEntry(newWordEntry)
                // Flow will update the list
            } catch (e: Exception) {
                // Handle potential constraint violations (e.g., unique word in profile)
                println("Error adding word: ${e.message}")
                // You might want to show a user-facing error message here
            }
        }
    }

    fun updateWordEntry(wordEntry: WordEntry) {
// factory = object : ViewModelProvider.Entry being updated belongs to the currently active profile/category
        // This is a sanity check, usually the UI would only allow updating relevant words.
        if (wordEntry.profileIdFk != _activeProfileId.value || wordEntry.categoryIdFk != _activeCategoryId.value) {
            println("Warning: Attempting to update a word that doesn't match active profile/category.")
            // Decide if you want to proceed or block this. For now, we'll allow it assuming
            // the wordEntry object is correctly sourced.
        }
        viewModelScope.launch {
            wordEntryDao.updateWordEntry(wordEntry)
            // Flow will update the list
        }
    }

    fun deleteWordEntry(wordEntry: WordEntry) {
        viewModelScope.launch {
            wordEntryDao.deleteWordEntry(wordEntry)
            // Flow will update the list
        }
    }

    fun getWordEntryById(wordId: Long, callback: (WordEntry?) -> Unit) {
        viewModelScope.launch {
            callback(wordEntryDao.getWordEntryById(wordId))
        }
    }
}
