package com.shdarv.yalda.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.db.ProfileDao
import com.shdarv.yalda.db.WordEntryDao
import com.shdarv.yalda.db.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileDao: ProfileDao,
    private val wordEntryDao: WordEntryDao
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()
    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    init {
        loadAllProfiles()
    }

    private fun loadAllProfiles() {
        viewModelScope.launch {
            profileDao.getAllProfiles()
                .distinctUntilChanged()
                .collect { profileList ->
                    _profiles.value = profileList
                    if (profileList.isEmpty()) {
                        addProfile(
                            profileName = "Yalda English to Persian",
                            sourceLanguage = "en",
                            targetLanguage = "fa"
                        )
                    } else if (_selectedProfile.value == null && profileList.isNotEmpty()) {
                         setSelectedProfile(profileList.first())
                    }
                }
        }
    }

    fun setSelectedProfile(profile: Profile?) {
        _selectedProfile.value = profile
    }

    fun addProfile(profileName: String, sourceLanguage: String, targetLanguage: String) {
        viewModelScope.launch {
            val newProfile = Profile(
                name = profileName,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
            profileDao.insertProfile(newProfile)
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            profileDao.updateProfile(profile)
            if (_selectedProfile.value?.id == profile.id) {
                _selectedProfile.value = profile
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileDao.deleteProfile(profile)
            if (_selectedProfile.value?.id == profile.id) {
                setSelectedProfile(null)
            }
        }
    }

    fun getProfileById(profileId: Long) {
        viewModelScope.launch {
            _selectedProfile.value = profileDao.getProfileById(profileId)
        }
    }
}

val profilesViewModelFactory = viewModelFactory {
    initializer {
        ProfilesViewModel(
            profileDao = database.get().profileDao(),
            wordEntryDao = database.get().wordEntryDao()
        )
    }
}

