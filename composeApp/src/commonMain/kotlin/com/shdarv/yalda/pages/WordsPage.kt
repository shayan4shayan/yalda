package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.localization.LocalAppStrings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.foundation.shape.RoundedCornerShape
import com.shdarv.yalda.translation.TranslationResult
import com.shdarv.yalda.translation.TranslationViewModel

@Composable
fun WordsPage(
    categories: List<Category>,
    selectedCategoryId: Long?,
    words: List<WordEntry>,
    sourceLanguage: String?,
    targetLanguage: String?,
    translationViewModel: TranslationViewModel,
    onSelectCategory: (Category?) -> Unit,
    onAddWord: (String, String, String?, Long) -> Unit,
    onUpdateWord: (WordEntry) -> Unit,
    onDeleteWord: (WordEntry) -> Unit
) {
    val strings = LocalAppStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<WordEntry?>(null) }
    var deletingWord by remember { mutableStateOf<WordEntry?>(null) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCategory?.name ?: strings("All Words"),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { categoryMenuOpen = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = strings("Filter category"))
                    }
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings("All categories")) },
                            onClick = {
                                onSelectCategory(null)
                                categoryMenuOpen = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onSelectCategory(category)
                                    categoryMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    enabled = categories.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = strings("Add word"))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (words.isEmpty()) {
            Text(strings("No words yet. Add your first word."))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(words, key = { it.wordId }) { word ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = word.word)
                            Text(text = word.meaning, style = MaterialTheme.typography.bodyMedium)
                            if (selectedCategoryId == null) {
                                val categoryName = categories.firstOrNull { it.id == word.categoryIdFk }?.name
                                if (categoryName != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = strings("Category: {0}", categoryName),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            word.description?.takeIf { it.isNotBlank() }?.let { description ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { editingWord = word }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = strings("Edit Word"))
                                }
                                IconButton(onClick = { deletingWord = word }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = strings("Delete Word"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        WordEditDialog(
            title = strings("Add Word"),
            categories = categories,
            initialWord = "",
            initialMeaning = "",
            initialDescription = "",
            initialCategoryId = selectedCategoryId,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            translationViewModel = translationViewModel,
            onDismiss = { showAddDialog = false },
            onConfirm = { word, meaning, description, categoryId ->
                onAddWord(word, meaning, description, categoryId)
                showAddDialog = false
            }
        )
    }

    editingWord?.let { word ->
        WordEditDialog(
            title = strings("Edit Word"),
            categories = categories,
            initialWord = word.word,
            initialMeaning = word.meaning,
            initialDescription = word.description ?: "",
            initialCategoryId = word.categoryIdFk,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            translationViewModel = translationViewModel,
            onDismiss = { editingWord = null },
            onConfirm = { newWord, newMeaning, newDescription, categoryId ->
                val updated = word.copy(
                    word = newWord,
                    meaning = newMeaning,
                    description = newDescription,
                    categoryIdFk = categoryId
                )
                onUpdateWord(updated)
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
                TextButton(onClick = { deletingWord = null }) {
                    Text(strings("Cancel"))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordEditDialog(
    title: String,
    categories: List<Category>,
    initialWord: String,
    initialMeaning: String,
    initialDescription: String,
    initialCategoryId: Long?,
    sourceLanguage: String?,
    targetLanguage: String?,
    translationViewModel: TranslationViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Long) -> Unit
) {
    val strings = LocalAppStrings.current
    var word by remember { mutableStateOf(initialWord) }
    var meaning by remember { mutableStateOf(initialMeaning) }
    var description by remember { mutableStateOf(initialDescription) }
    var categoryId by remember { mutableStateOf(initialCategoryId ?: categories.firstOrNull()?.id) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var translationMessage by remember { mutableStateOf<String?>(null) }
    var showTranslationAttribution by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val translationState by translationViewModel.uiState.collectAsState()
    val isTranslationBusy = translationState.isTranslating || translationState.isDownloadingPair
    val canTranslate = word.trim().isNotEmpty() && !isTranslationBusy

    fun handleTranslationResult(result: TranslationResult) {
        when (result) {
            is TranslationResult.Success -> {
                meaning = result.text
                translationMessage = null
                showTranslationAttribution = true
            }
            is TranslationResult.PairNotReady -> {
                translationMessage = strings("Download this profile's offline translation setup in Settings.")
            }
            is TranslationResult.UnsupportedLanguagePair -> {
                translationMessage = strings("Offline translation is unavailable for this language pair.")
            }
            is TranslationResult.Failure -> {
                translationMessage = strings.runtime(result.message)
            }
        }
    }

    fun requestTranslation() {
        val source = sourceLanguage
        val target = targetLanguage
        if (source == null || target == null) {
            translationMessage = strings("Select a profile before using translation.")
            return
        }
        translationViewModel.translate(
            text = word,
            sourceLanguage = source,
            targetLanguage = target,
            onResult = ::handleTranslationResult
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text(strings("Word")) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = meaning,
                        onValueChange = { meaning = it },
                        label = { Text(strings("Meaning")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { requestTranslation() },
                        enabled = canTranslate
                    ) {
                        if (isTranslationBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Translate, contentDescription = strings("Translate word"))
                        }
                    }
                }
                translationMessage?.let { message ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (showTranslationAttribution) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings("Powered by Google Translate"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings("Description (optional)")) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (categories.size == 1) {
                    Text(
                        strings("Category: {0}", categories.single().name),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        enabled = categories.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.FilterList, contentDescription = strings("Select category"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedCategory?.name ?: strings("Category"))
                    }
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryId = category.id
                                    categoryMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val chosenCategoryId = categoryId
                    if (chosenCategoryId != null) {
                        onConfirm(
                            word.trim(),
                            meaning.trim(),
                            description.trim().ifBlank { null },
                            chosenCategoryId
                        )
                    }
                },
                enabled = word.trim().isNotEmpty() && meaning.trim().isNotEmpty() && categoryId != null
            ) {
                Text(strings("Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings("Cancel"))
            }
        }
    )

}
