package com.shdarv.yalda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.shdarv.yalda.localization.AppStringCatalog
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.navigation.AppSection
import com.shdarv.yalda.pages.HomePage
import com.shdarv.yalda.pages.CreateProfilePage
import com.shdarv.yalda.pages.MainHeader
import com.shdarv.yalda.pages.MenuPage
import com.shdarv.yalda.pages.MenuSection
import com.shdarv.yalda.pages.QuizPage
import com.shdarv.yalda.pages.SearchPage
import com.shdarv.yalda.pages.ProfileManagerSheet
import com.shdarv.yalda.ui.YaldaTheme
import com.shdarv.yalda.viewmodel.rememberCategoriesViewModel
import com.shdarv.yalda.viewmodel.rememberProfilesViewModel
import com.shdarv.yalda.viewmodel.rememberSearchViewModel
import com.shdarv.yalda.viewmodel.rememberTranslationViewModel
import com.shdarv.yalda.viewmodel.rememberWordStoreViewModel
import com.shdarv.yalda.viewmodel.rememberWordsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Composable
@Preview
fun App() {
    val profilesViewModel = rememberProfilesViewModel()
    val categoriesViewModel = rememberCategoriesViewModel()
    val wordsViewModel = rememberWordsViewModel()
    val storeViewModel = rememberWordStoreViewModel()
    val searchViewModel = rememberSearchViewModel()
    val translationViewModel = rememberTranslationViewModel()

    val selectedProfile by profilesViewModel.selectedProfile.collectAsState()
    val profiles by profilesViewModel.profiles.collectAsState()
    val setupPreferences by profilesViewModel.setupPreferences.collectAsState()
    val profilesLoaded by profilesViewModel.isLoaded.collectAsState()
    val profileMessage by profilesViewModel.message.collectAsState()
    val categories by categoriesViewModel.categoriesForProfile.collectAsState()
    val wordCounts by categoriesViewModel.wordCountsForProfile.collectAsState()

    val homeStateHolder = rememberSaveableStateHolder()
    var currentSection by rememberSaveable { mutableStateOf(AppSection.Home) }
    var menuSection by rememberSaveable { mutableStateOf(MenuSection.Home) }
    var showCreateProfile by rememberSaveable { mutableStateOf(false) }
    var showProfileManager by rememberSaveable { mutableStateOf(false) }
    val selectedSetupPreference = selectedProfile?.id?.let(setupPreferences::get)
    val showSetupWizard = profilesLoaded && (profiles.isEmpty() || showCreateProfile)
    val appLanguage = selectedProfile?.sourceLanguage ?: "en"
    val strings = remember(appLanguage) { AppStringCatalog.stringsFor(appLanguage) }

    LaunchedEffect(selectedProfile?.id) {
        categoriesViewModel.setActiveProfileId(selectedProfile?.id)
        wordsViewModel.setActiveProfile(selectedProfile?.id)
        searchViewModel.setActiveProfile(selectedProfile?.id)
        translationViewModel.setActivePair(
            selectedProfile?.sourceLanguage,
            selectedProfile?.targetLanguage
        )
    }

    LaunchedEffect(selectedProfile?.id, selectedSetupPreference?.selectedDatasetPackIdsJson) {
        storeViewModel.setActiveProfile(
            selectedProfile,
            selectedProfile?.id?.let(profilesViewModel::selectedPackIds) ?: emptySet()
        )
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides if (appLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        YaldaTheme {
            Scaffold(
            bottomBar = {
                if (!showSetupWizard) {
                    NavigationBar {
                        AppSection.entries.forEach { section ->
                            NavigationBarItem(
                                selected = currentSection == section,
                                onClick = {
                                    currentSection = section
                                    if (section == AppSection.Menu) {
                                        menuSection = MenuSection.Home
                                    }
                                },
                                icon = { androidx.compose.material3.Icon(section.icon, contentDescription = strings(section.label)) },
                                label = { Text(strings(section.label), fontSize = 11.sp, maxLines = 1) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!profilesLoaded) {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                        }
                    } else if (showSetupWizard) {
                        CreateProfilePage(
                            canDismiss = profiles.isNotEmpty(),
                            catalogPacks = storeViewModel.packs,
                            isCatalogLoading = storeViewModel.isCatalogLoading,
                            catalogMessage = storeViewModel.catalogMessage,
                            onRetryCatalog = storeViewModel::loadFromNetwork,
                            onBack = { if (profiles.isNotEmpty()) showCreateProfile = false },
                            onCreateProfile = { name, source, target, selectedPackIds, prepareOffline ->
                                profilesViewModel.addProfile(
                                    name,
                                    source,
                                    target,
                                    selectedPackIds,
                                    prepareOffline
                                )
                                showCreateProfile = false
                            }
                        )
                    } else {
                        MainHeader(
                            profileName = selectedProfile?.name ?: strings("Your Library"),
                            sourceLanguage = selectedProfile?.sourceLanguage,
                            targetLanguage = selectedProfile?.targetLanguage,
                            onProfileClick = { showProfileManager = true }
                        )
                        when (currentSection) {
                            AppSection.Home -> selectedProfile?.let { profile ->
                                homeStateHolder.SaveableStateProvider(profile.id) {
                                    HomePage(
                                        profileId = profile.id,
                                        sourceLanguage = profile.sourceLanguage,
                                        targetLanguage = profile.targetLanguage,
                                        categories = categories.filter { it.profileIdFk == profile.id },
                                        translationViewModel = translationViewModel,
                                        onOpenSettings = {
                                            menuSection = MenuSection.Settings
                                            currentSection = AppSection.Menu
                                        },
                                        onAddCategory = { categoriesViewModel.addCategory(it) },
                                        onSaveWord = { word, meaning, categoryId, onComplete ->
                                            wordsViewModel.addWordEntry(
                                                word = word,
                                                meaning = meaning,
                                                description = null,
                                                categoryIdOverride = categoryId,
                                                profileIdOverride = profile.id,
                                                onComplete = onComplete
                                            )
                                        }
                                    )
                                }
                            }
                            AppSection.Search -> SearchPage(
                                searchViewModel = searchViewModel,
                                profileId = selectedProfile?.id,
                                allCategories = categories,
                                wordCounts = wordCounts,
                                sourceLanguage = selectedProfile?.sourceLanguage,
                                targetLanguage = selectedProfile?.targetLanguage,
                                translationViewModel = translationViewModel,
                                onAddCategory = { categoriesViewModel.addCategory(it) },
                                onUpdateCategory = { categoriesViewModel.updateCategory(it) },
                                onDeleteCategory = { categoriesViewModel.deleteCategory(it) },
                                onAddWord = { word, meaning, description, categoryId ->
                                    wordsViewModel.addWordEntry(
                                        word = word,
                                        meaning = meaning,
                                        description = description,
                                        categoryIdOverride = categoryId,
                                        profileIdOverride = selectedProfile?.id
                                    )
                                },
                                onUpdateWord = { wordsViewModel.updateWordEntry(it) },
                                onDeleteWord = { wordsViewModel.deleteWordEntry(it) }
                            )
                            AppSection.Quiz -> QuizPage(
                                profileId = selectedProfile?.id,
                                categories = categories,
                                wordsViewModel = wordsViewModel
                            )
                            AppSection.Menu -> MenuPage(
                                destination = menuSection,
                                profile = selectedProfile,
                                profiles = profiles,
                                setupPreference = selectedSetupPreference,
                                storeViewModel = storeViewModel,
                                translationViewModel = translationViewModel,
                                onOpenDestination = { menuSection = it },
                                onBackToMenu = { menuSection = MenuSection.Home }
                            )
                        }
                    }
                }
            }
        }

            if (showProfileManager && !showSetupWizard) {
                ProfileManagerSheet(
                profiles = profiles,
                selectedProfile = selectedProfile,
                message = profileMessage,
                onDismiss = { showProfileManager = false },
                onSwitch = {
                    profilesViewModel.setSelectedProfile(it)
                    showProfileManager = false
                },
                onCreate = {
                    showProfileManager = false
                    showCreateProfile = true
                },
                onRename = profilesViewModel::updateProfile,
                onDelete = profilesViewModel::deleteProfile,
                onClearMessage = profilesViewModel::clearMessage
                )
            }
        }
    }
}
