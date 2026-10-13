package com.shdarv.yalda.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.translation.TranslationViewModel
import com.shdarv.yalda.viewmodel.SearchViewModel

private const val CATEGORY_PREVIEW_LIMIT = 4
private const val WORD_PREVIEW_LIMIT = 20

@Composable
fun SearchPage(
    searchViewModel: SearchViewModel,
    profileId: Long?,
    allCategories: List<Category>,
    wordCounts: Map<Long, Int>,
    sourceLanguage: String?,
    targetLanguage: String?,
    translationViewModel: TranslationViewModel,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddWord: (String, String, String?, Long) -> Unit,
    onUpdateWord: (WordEntry) -> Unit,
    onDeleteWord: (WordEntry) -> Unit
) {
    val strings = LocalAppStrings.current
    val query by searchViewModel.query.collectAsState()
    val categoryResults by searchViewModel.categories.collectAsState()
    val wordResults by searchViewModel.words.collectAsState()
    val profileCategories = allCategories.filter { it.profileIdFk == profileId }
    val matchingCategories = categoryResults.filter { it.profileIdFk == profileId }
    val matchingWords = wordResults.filter { it.profileIdFk == profileId }

    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    var showAllWords by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable(profileId) { mutableStateOf<Long?>(null) }
    var newCategoryName by remember { mutableStateOf<String?>(null) }
    var newWordText by remember { mutableStateOf<String?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }
    var editingWord by remember { mutableStateOf<WordEntry?>(null) }
    var deletingWord by remember { mutableStateOf<WordEntry?>(null) }

    val normalizedQuery = query.trim()
    val selectedCategory = profileCategories.firstOrNull { it.id == selectedCategoryId }
    val wordsForSection = matchingWords.filter { word ->
        selectedCategoryId == null || word.categoryIdFk == selectedCategoryId
    }
    val visibleCategories = if (showAllCategories || normalizedQuery.isEmpty()) {
        matchingCategories
    } else {
        matchingCategories.take(CATEGORY_PREVIEW_LIMIT)
    }
    val visibleWords = if (showAllWords) {
        wordsForSection
    } else {
        wordsForSection.take(WORD_PREVIEW_LIMIT)
    }
    var libraryQuery by rememberSaveable(profileId) { mutableStateOf("") }

    fun openCategory(categoryId: Long) {
        libraryQuery = query
        selectedCategoryId = categoryId
        searchViewModel.setQuery("")
    }

    fun closeCategory() {
        selectedCategoryId = null
        searchViewModel.setQuery(libraryQuery)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(query, selectedCategoryId) {
        listState.scrollToItem(0)
        showAllCategories = false
        showAllWords = false
    }

    LaunchedEffect(profileId) {
        showAllCategories = false
        showAllWords = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (selectedCategory != null) {
            TextButton(onClick = { closeCategory() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Text(strings("Library"), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            text = selectedCategory?.name ?: strings("Library"),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { searchViewModel.setQuery(it) },
            placeholder = {
                Text(strings(if (selectedCategory == null) "Search categories and words" else "Search words"))
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { searchViewModel.setQuery("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = strings("Clear search"))
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            onClick = {
                if (selectedCategory != null) newWordText = normalizedQuery
                else newCategoryName = normalizedQuery
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(
                strings(if (selectedCategory != null) "Add word" else "Create category"),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedCategory == null) {
                item {
                    SectionHeader(
                        title = strings("Categories"),
                        totalCount = matchingCategories.size,
                        previewLimit = if (normalizedQuery.isEmpty()) Int.MAX_VALUE else CATEGORY_PREVIEW_LIMIT,
                        expanded = showAllCategories,
                        onToggleExpanded = { showAllCategories = !showAllCategories }
                    )
                }
                if (matchingCategories.isEmpty()) {
                    item {
                        EmptySectionMessage(
                            if (normalizedQuery.isEmpty()) strings("No categories yet. Add your first category.")
                            else strings("No categories match “{0}”.", normalizedQuery)
                        )
                    }
                }
                items(visibleCategories, key = { "category-${it.id}" }) { category ->
                    LibraryCategoryCard(
                        category = category,
                        wordCount = wordCounts[category.id] ?: 0,
                        onOpen = { openCategory(category.id) },
                        onEdit = { editingCategory = category },
                        onDelete = { deletingCategory = category }
                    )
                }
            }
            if (selectedCategory != null || normalizedQuery.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = strings("Words"),
                        totalCount = wordsForSection.size,
                        previewLimit = WORD_PREVIEW_LIMIT,
                        expanded = showAllWords,
                        onToggleExpanded = { showAllWords = !showAllWords }
                    )
                }
                if (wordsForSection.isEmpty()) {
                    item {
                        EmptySectionMessage(
                            if (normalizedQuery.isEmpty()) strings("No words yet. Add your first word.")
                            else strings("No words match “{0}”.", normalizedQuery)
                        )
                    }
                }
                items(visibleWords, key = { "word-${it.wordId}" }) { word ->
                    WordSearchCard(
                        word = word,
                        categoryName = profileCategories.firstOrNull { it.id == word.categoryIdFk }?.name
                            ?: strings("Unknown category"),
                        onOpen = {
                            if (selectedCategory == null) openCategory(word.categoryIdFk)
                            else editingWord = word
                        },
                        onEdit = { editingWord = word },
                        onDelete = { deletingWord = word }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    newCategoryName?.let { initialName ->
        CategoryEditDialog(
            title = strings("Create Category"),
            initialValue = initialName,
            onDismiss = { newCategoryName = null },
            onConfirm = { name ->
                onAddCategory(name)
                newCategoryName = null
                searchViewModel.setQuery("")
            }
        )
    }

    editingCategory?.let { category ->
        CategoryEditDialog(
            title = strings("Edit Category"),
            initialValue = category.name,
            onDismiss = { editingCategory = null },
            onConfirm = { name ->
                onUpdateCategory(category.copy(name = name))
                editingCategory = null
            }
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text(strings("Delete Category")) },
            text = { Text(strings("Delete {0} and all its words?", category.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedCategoryId == category.id) {
                            selectedCategoryId = null
                        }
                        onDeleteCategory(category)
                        deletingCategory = null
                    }
                ) { Text(strings("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text(strings("Cancel")) }
            }
        )
    }

    newWordText?.let { initialWord ->
        WordEditDialog(
            title = strings("Add Word"),
            categories = profileCategories.filter { it.id == selectedCategoryId },
            initialWord = initialWord,
            initialMeaning = "",
            initialDescription = "",
            initialCategoryId = selectedCategoryId,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            translationViewModel = translationViewModel,
            onDismiss = { newWordText = null },
            onConfirm = { word, meaning, description, categoryId ->
                onAddWord(word, meaning, description, categoryId)
                newWordText = null
            }
        )
    }

    editingWord?.let { word ->
        WordEditDialog(
            title = strings("Edit Word"),
            categories = profileCategories,
            initialWord = word.word,
            initialMeaning = word.meaning,
            initialDescription = word.description.orEmpty(),
            initialCategoryId = word.categoryIdFk,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            translationViewModel = translationViewModel,
            onDismiss = { editingWord = null },
            onConfirm = { updatedWord, meaning, description, categoryId ->
                onUpdateWord(
                    word.copy(
                        word = updatedWord,
                        meaning = meaning,
                        description = description,
                        categoryIdFk = categoryId
                    )
                )
                editingWord = null
            }
        )
    }

    deletingWord?.let { word ->
        AlertDialog(
            onDismissRequest = { deletingWord = null },
            title = { Text(strings("Delete Word")) },
            text = { Text(strings("Delete {0}?", word.word)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWord(word)
                        deletingWord = null
                    }
                ) { Text(strings("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { deletingWord = null }) { Text(strings("Cancel")) }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    totalCount: Int,
    previewLimit: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    secondaryAction: Pair<String, () -> Unit>? = null
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = strings(if (totalCount == 1) "{0} item" else "{0} items", totalCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        secondaryAction?.let { (label, action) ->
            TextButton(onClick = action) { Text(label) }
        }
        if (totalCount > previewLimit) {
            TextButton(onClick = onToggleExpanded) {
                Text(strings(if (expanded) "Show less" else "See all"))
            }
        }
    }
}

@Composable
private fun EmptySectionMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun WordSearchCard(
    word: WordEntry,
    categoryName: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.word, fontWeight = FontWeight.SemiBold)
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "${strings("Edit Word")}: ${word.word}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = strings("Delete {0}?", word.word))
            }
        }
    }
}

@Composable
private fun LibraryCategoryCard(
    category: Category,
    wordCount: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.SemiBold)
                Text(strings("{0} words", wordCount), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = strings("Edit Category"))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = strings("Delete Category"))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}
