package com.shdarv.yalda.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shdarv.yalda.db.CategoryWithWordsDto
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.db.WordEntryDto
import com.shdarv.yalda.io.CategoryDataHandler
import com.shdarv.yalda.io.ioDataHandler
import com.shdarv.yalda.models.RemoteFile
import com.shdarv.yalda.network.github
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordsStoreViewModel: ViewModel() {

    val categories = mutableStateListOf<RemoteFile>()
    val importedCategories = mutableStateListOf<String>()
    val importingCategories = mutableStateListOf<String>()
    var importMessage: String? by mutableStateOf(null)
    val importHandler : CategoryDataHandler = ioDataHandler.get()

    init {
        loadFromNetwork()
    }


    fun loadFromNetwork() {
        viewModelScope.launch {
            categories.clear()
            categories.addAll(
                github.getFilesList()
            )
        }
    }

    fun loadAndImportCategory(profile: Profile, file: RemoteFile) {
        viewModelScope.launch(Dispatchers.Default) {
            setImporting(file.name, true)
            try {
                if (isCategoryImportedInternal(file)) {
                    markCategoryImported(file)
                    setImportMessage("${file.name} is already imported.")
                } else {
                    val words = github.getWordsList(file.address)
                    if (words.isEmpty()) {
                        setImportMessage("Import failed for ${file.name}.")
                    } else {
                        val category = CategoryWithWordsDto(
                            file.name,
                            words = words.map {
                                WordEntryDto(
                                    word = it.word,
                                    definition = it.meaning,
                                    description = it.description
                                )
                            }
                        )
                        importHandler.saveImportedCategory(profile, category)
                        markCategoryImported(file)
                        setImportMessage("Imported ${file.name}.")
                    }
                }
            } catch (e: Exception) {
                setImportMessage("Import failed for ${file.name}.")
            } finally {
                setImporting(file.name, false)
            }
        }
    }

    suspend fun refreshImportedCategories(profile: Profile?) {
        if (profile == null) {
            withContext(Dispatchers.Main) {
                importedCategories.clear()
            }
            return
        }

        val importedNames = categories.mapNotNull { file ->
            if (isCategoryImportedInternal(file)) file.name else null
        }
        withContext(Dispatchers.Main) {
            importedCategories.clear()
            importedCategories.addAll(importedNames)
        }
    }

    fun isCategoryImported(file: RemoteFile): Boolean =
        importedCategories.contains(file.name)

    fun isCategoryImporting(file: RemoteFile): Boolean =
        importingCategories.contains(file.name)

    fun clearImportMessage() {
        importMessage = null
    }

    private suspend fun markCategoryImported(file: RemoteFile) {
        withContext(Dispatchers.Main) {
            if (!importedCategories.contains(file.name)) {
                importedCategories.add(file.name)
            }
        }
    }

    private suspend fun isCategoryImportedInternal(file: RemoteFile): Boolean =
        importHandler.hasCategoryByName(file.name)

    private suspend fun setImporting(address: String, isImporting: Boolean) {
        withContext(Dispatchers.Main) {
            if (isImporting) {
                if (!importingCategories.contains(address)) {
                    importingCategories.add(address)
                }
            } else {
                importingCategories.remove(address)
            }
        }
    }

    private suspend fun setImportMessage(message: String?) {
        withContext(Dispatchers.Main) {
            importMessage = message
        }
    }
}
