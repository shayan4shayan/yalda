package com.shdarv.yalda.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shdarv.yalda.db.AppDatabase
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.models.DatasetPack
import com.shdarv.yalda.network.DatasetCatalogSource
import com.shdarv.yalda.network.github
import com.shdarv.yalda.platform.AppSettingsStore
import kotlinx.coroutines.launch

class WordsStoreViewModel(
    private val appDatabase: AppDatabase,
    private val settings: AppSettingsStore
) : ViewModel() {
    val packs = mutableStateListOf<DatasetPack>()
    val installedPackIds = mutableStateListOf<String>()
    private val importingPackKeys = mutableStateListOf<String>()

    var isCatalogLoading by mutableStateOf(false)
        private set
    var catalogMessage: String? by mutableStateOf(null)
        private set
    var importMessage: String? by mutableStateOf(null)
        private set

    private var activeProfile: Profile? = null
    private var selectedPackIds: Set<String> = emptySet()

    val activeProfileId: Long?
        get() = activeProfile?.id

    init {
        loadFromNetwork()
    }

    fun loadFromNetwork() {
        if (isCatalogLoading) return
        viewModelScope.launch {
            isCatalogLoading = true
            catalogMessage = null
            try {
                val cached = settings.getString(CATALOG_CACHE_KEY, "")
                val loaded = github.getCatalog(cached)
                packs.clear()
                packs.addAll(loaded.catalog.packs)
                if (loaded.source == DatasetCatalogSource.NetworkV2 && loaded.rawV2 != null) {
                    settings.putString(CATALOG_CACHE_KEY, loaded.rawV2)
                }
                catalogMessage = when (loaded.source) {
                    DatasetCatalogSource.NetworkV2 -> null
                    DatasetCatalogSource.CacheV2 -> "Showing the last saved catalog. Refresh failed."
                    DatasetCatalogSource.LegacyIndex -> "Using the legacy English to Persian catalog."
                }
            } catch (exception: Exception) {
                catalogMessage = exception.message ?: "Catalog refresh failed. Existing data was kept."
            } finally {
                isCatalogLoading = false
            }
        }
    }

    fun setActiveProfile(profile: Profile?, selectedDatasetPackIds: Set<String>) {
        if (activeProfile?.id != profile?.id) {
            installedPackIds.clear()
            importMessage = null
        }
        activeProfile = profile
        selectedPackIds = selectedDatasetPackIds
        refreshInstalledPacks()
    }

    fun compatiblePacks(sourceLanguage: String, targetLanguage: String): List<DatasetPack> =
        packs.filter { it.sourceLanguage == sourceLanguage && it.targetLanguage == targetLanguage }

    fun orderedPacksForActiveProfile(): List<DatasetPack> {
        val profile = activeProfile ?: return emptyList()
        return compatiblePacks(profile.sourceLanguage, profile.targetLanguage)
            .sortedWith(compareByDescending<DatasetPack> { it.id in selectedPackIds }.thenBy { it.title })
    }

    fun selectedPacksForActiveProfile(): List<DatasetPack> =
        orderedPacksForActiveProfile().filter { it.id in selectedPackIds }

    fun importSelected() {
        val profile = activeProfile ?: return
        val selected = selectedPacksForActiveProfile().filterNot { isPackImporting(it) }
        if (selected.isEmpty()) {
            importMessage = "No selected packs are available for this language pair."
            return
        }
        viewModelScope.launch {
            selected.forEach { pack -> importPack(profile, pack) }
        }
    }

    fun loadAndImportCategory(profile: Profile, pack: DatasetPack) {
        viewModelScope.launch { importPack(profile, pack) }
    }

    fun isPackInstalled(pack: DatasetPack): Boolean = pack.id in installedPackIds
    fun isPackImporting(pack: DatasetPack): Boolean =
        activeProfile?.let { importKey(it.id, pack.id) in importingPackKeys } == true

    fun clearImportMessage() {
        importMessage = null
    }

    private suspend fun importPack(profile: Profile, pack: DatasetPack) {
        if (pack.sourceLanguage != profile.sourceLanguage || pack.targetLanguage != profile.targetLanguage) {
            importMessage = "${pack.title} does not match this profile's directed language pair."
            return
        }
        val importKey = importKey(profile.id, pack.id)
        if (!importingPackKeys.add(importKey)) return
        try {
            val packFile = github.getPack(pack)
            val summary = appDatabase.datasetImportDao().importPack(profile.id, pack, packFile)
            if (activeProfile?.id == profile.id) {
                if (pack.id !in installedPackIds) installedPackIds.add(pack.id)
                importMessage = summary.displayText(pack.title)
            }
        } catch (exception: Exception) {
            if (activeProfile?.id == profile.id) {
                importMessage = "Import failed for ${pack.title}: ${exception.message ?: "invalid dataset"}"
            }
        } finally {
            importingPackKeys.remove(importKey)
        }
    }

    private fun refreshInstalledPacks() {
        val profileId = activeProfile?.id
        if (profileId == null) {
            installedPackIds.clear()
            return
        }
        viewModelScope.launch {
            val installed = appDatabase.categoryDao().getInstalledDatasetPackIds(profileId)
            if (activeProfile?.id == profileId) {
                installedPackIds.clear()
                installedPackIds.addAll(installed.distinct())
            }
        }
    }

    private fun importKey(profileId: Long, packId: String): String = "$profileId:$packId"

    private companion object {
        const val CATALOG_CACHE_KEY = "dataset.catalog.v2.cache"
    }
}
