package com.shdarv.yalda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.db.ProfileDao
import com.shdarv.yalda.db.ProfileSetupPreference
import com.shdarv.yalda.db.database
import com.shdarv.yalda.platform.AppSettingsStore
import com.shdarv.yalda.platform.appSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ProfilesViewModel(
    private val profileDao: ProfileDao,
    private val settings: AppSettingsStore
) : ViewModel() {
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    private val _setupPreferences = MutableStateFlow<Map<Long, ProfileSetupPreference>>(emptyMap())
    val setupPreferences: StateFlow<Map<Long, ProfileSetupPreference>> = _setupPreferences.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        observeProfiles()
        observeSetupPreferences()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            profileDao.getAllProfiles().distinctUntilChanged().collect { profileList ->
                _profiles.value = profileList
                val selectedId = _selectedProfile.value?.id
                    ?: settings.getLong(SELECTED_PROFILE_ID_KEY, NO_PROFILE_ID)
                        .takeUnless { it == NO_PROFILE_ID }
                val selected = selectedId?.let { id -> profileList.firstOrNull { it.id == id } }
                    ?: profileList.firstOrNull()
                _selectedProfile.value = selected
                settings.putLong(SELECTED_PROFILE_ID_KEY, selected?.id ?: NO_PROFILE_ID)
                _isLoaded.value = true
            }
        }
    }

    private fun observeSetupPreferences() {
        viewModelScope.launch {
            profileDao.getAllSetupPreferences().distinctUntilChanged().collect { preferences ->
                _setupPreferences.value = preferences.associateBy(ProfileSetupPreference::profileIdFk)
            }
        }
    }

    fun setSelectedProfile(profile: Profile) {
        if (_profiles.value.none { it.id == profile.id }) return
        _selectedProfile.value = profile
        settings.putLong(SELECTED_PROFILE_ID_KEY, profile.id)
    }

    fun addProfile(
        profileName: String,
        sourceLanguage: String,
        targetLanguage: String,
        selectedDatasetPackIds: Set<String>,
        prepareOfflineTranslation: Boolean
    ) {
        require(sourceLanguage != targetLanguage) { "Source and target languages must differ." }
        viewModelScope.launch {
            val profileId = profileDao.insertProfileWithSetup(
                profile = Profile(
                    name = profileName.trim(),
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                ),
                selectedDatasetPackIds = selectedDatasetPackIds,
                prepareOfflineTranslation = prepareOfflineTranslation
            )
            profileDao.getProfileById(profileId)?.let(::setSelectedProfile)
        }
    }

    fun updateProfile(profile: Profile) {
        val name = profile.name.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            profileDao.updateProfile(profile.copy(name = name))
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            if (!profileDao.deleteProfileUnlessLast(profile.id)) {
                _message.value = "At least one profile is required."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun selectedPackIds(profileId: Long): Set<String> =
        _setupPreferences.value[profileId]?.selectedDatasetPackIdsJson
            ?.let { encoded -> runCatching { Json.decodeFromString<List<String>>(encoded).toSet() }.getOrDefault(emptySet()) }
            ?: emptySet()

    private companion object {
        const val SELECTED_PROFILE_ID_KEY = "profiles.selected.id"
        const val NO_PROFILE_ID = -1L
    }
}

val profilesViewModelFactory = viewModelFactory {
    initializer {
        ProfilesViewModel(
            profileDao = database.get().profileDao(),
            settings = appSettings.get()
        )
    }
}
