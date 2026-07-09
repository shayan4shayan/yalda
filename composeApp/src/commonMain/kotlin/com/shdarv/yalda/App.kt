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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shdarv.yalda.navigation.AppSection
import com.shdarv.yalda.pages.CategoriesPage
import com.shdarv.yalda.pages.CreateProfilePage
import com.shdarv.yalda.pages.MainHeader
import com.shdarv.yalda.pages.MenuPage
import com.shdarv.yalda.pages.MenuSection
import com.shdarv.yalda.pages.QuizPage
import com.shdarv.yalda.pages.SearchPage
import com.shdarv.yalda.pages.WordsPage
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
    val categories by categoriesViewModel.categoriesForProfile.collectAsState()
    val wordCounts by categoriesViewModel.wordCountsForProfile.collectAsState()
    val wordsForCategory by wordsViewModel.wordsInCategoryForProfile.collectAsState()
    val wordsForProfile by wordsViewModel.wordsForProfile.collectAsState()

    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var currentSection by rememberSaveable { mutableStateOf(AppSection.Categories) }
    var menuSection by rememberSaveable { mutableStateOf(MenuSection.Home) }
    var showCreateProfile by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedProfile?.id) {
        categoriesViewModel.setActiveProfileId(selectedProfile?.id)
        wordsViewModel.setActiveProfile(selectedProfile?.id)
        searchViewModel.setActiveProfile(selectedProfile?.id)
    }

    LaunchedEffect(selectedCategoryId) {
        wordsViewModel.setActiveCategory(selectedCategoryId)
    }

    YaldaTheme {
        Scaffold(
            bottomBar = {
                if (!showCreateProfile) {
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
                                icon = { androidx.compose.material3.Icon(section.icon, contentDescription = section.label) },
                                label = { Text(section.label, fontSize = 11.sp, maxLines = 1) },
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
                    if (showCreateProfile) {
                        CreateProfilePage(
                            onBack = { showCreateProfile = false },
                            onCreateProfile = { name, source, target ->
                                profilesViewModel.addProfile(name, source, target)
                                showCreateProfile = false
                            }
                        )
                    } else {
                        MainHeader(
                            profileName = selectedProfile?.name ?: "Your Library"
                        )
                        when (currentSection) {
                            AppSection.Categories -> CategoriesPage(
                                categories = categories,
                                wordCounts = wordCounts,
                                selectedCategoryId = selectedCategoryId,
                                onSelectCategory = { category ->
                                    selectedCategoryId = category.id
                                    currentSection = AppSection.Words
                                },
                                onAddCategory = { categoriesViewModel.addCategory(it) },
                                onUpdateCategory = { categoriesViewModel.updateCategory(it) },
                                onDeleteCategory = { categoriesViewModel.deleteCategory(it) }
                            )
                            AppSection.Words -> WordsPage(
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                                words = if (selectedCategoryId == null) wordsForProfile else wordsForCategory,
                                sourceLanguage = selectedProfile?.sourceLanguage,
                                targetLanguage = selectedProfile?.targetLanguage,
                                translationViewModel = translationViewModel,
                                onSelectCategory = { category ->
                                    selectedCategoryId = category?.id
                                },
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
                            AppSection.Search -> SearchPage(
                                searchViewModel = searchViewModel,
                                categoriesById = categories.associateBy { it.id },
                                onOpenCategory = { category ->
                                    selectedCategoryId = category.id
                                    currentSection = AppSection.Words
                                },
                                onOpenWord = { word ->
                                    selectedCategoryId = word.categoryIdFk
                                    currentSection = AppSection.Words
                                }
                            )
                            AppSection.Quiz -> QuizPage(
                                profileId = selectedProfile?.id,
                                categories = categories,
                                wordsViewModel = wordsViewModel
                            )
                            AppSection.Menu -> MenuPage(
                                destination = menuSection,
                                profile = selectedProfile,
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
    }
}
