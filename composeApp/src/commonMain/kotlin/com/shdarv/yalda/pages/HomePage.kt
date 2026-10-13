package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.translation.TranslationResult
import com.shdarv.yalda.translation.TranslationViewModel

@Composable
fun HomePage(
    profileId: Long,
    sourceLanguage: String,
    targetLanguage: String,
    categories: List<Category>,
    translationViewModel: TranslationViewModel,
    onOpenSettings: () -> Unit,
    onAddCategory: (String) -> Unit,
    onSaveWord: (String, String, Long, (Boolean) -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    val translationState by translationViewModel.uiState.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var translatedInput by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var showCreateCategory by rememberSaveable { mutableStateOf(false) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var requestId by remember { mutableStateOf(0) }
    val busy = translationState.isTranslating || translationState.isDownloadingPair
    val hasTranslation = translatedInput.isNotEmpty() && translatedInput == input.trim()

    LaunchedEffect(categories, pendingCategoryName) {
        pendingCategoryName?.let { name ->
            categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let {
                categoryId = it.id
                pendingCategoryName = null
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(strings("Home"), style = MaterialTheme.typography.headlineLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings("Translate"), style = MaterialTheme.typography.titleLarge)
                Text(
                    "${strings.languageName(sourceLanguage)} → ${strings.languageName(targetLanguage)}",
                    style = MaterialTheme.typography.labelLarge
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        meaning = ""
                        translatedInput = ""
                        message = null
                        requestId++
                    },
                    label = { Text(strings("Text to translate")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    enabled = !saving
                )
                if (!translationState.isSupported) {
                    Text(strings("Offline translation is not available on this device."))
                } else if (!translationState.isPairReady) {
                    Text(strings("Download this profile's offline translation setup in Settings."))
                    TextButton(onClick = onOpenSettings) { Text(strings("Settings")) }
                }
                Button(
                    onClick = {
                        val text = input.trim()
                        val currentRequest = ++requestId
                        message = null
                        translatedInput = ""
                        meaning = ""
                        translationViewModel.translate(text, sourceLanguage, targetLanguage) { result ->
                            if (currentRequest == requestId) {
                                when (result) {
                                    is TranslationResult.Success -> {
                                        translatedInput = text
                                        meaning = result.text
                                    }
                                    is TranslationResult.PairNotReady -> message = "Download this profile's offline translation setup in Settings."
                                    is TranslationResult.UnsupportedLanguagePair -> message = "Offline translation is unavailable for this language pair."
                                    is TranslationResult.Failure -> message = result.message
                                }
                            }
                        }
                    },
                    enabled = input.isNotBlank() && !busy && !saving && translationState.isSupported && translationState.isPairReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(strings("Translate"))
                }
                if (hasTranslation) {
                    OutlinedTextField(
                        value = meaning,
                        onValueChange = { meaning = it; message = null },
                        label = { Text(strings("Meaning")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5,
                        enabled = !saving
                    )
                    Text(strings("Powered by Google Translate"), style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(
                        onClick = { showCategories = true; message = null },
                        enabled = !saving && meaning.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings("Add to category")) }
                }
                message?.let { Text(strings.runtime(it), style = MaterialTheme.typography.bodyMedium) }
            }
        }
        key(profileId) { HomeStatisticsSection(profileId) }
    }

    if (showCategories && !showCreateCategory) {
        AlertDialog(
            onDismissRequest = { if (!saving) showCategories = false },
            title = { Text(strings("Add to category")) },
            text = {
                Column {
                    if (categories.isEmpty()) Text(strings("No categories yet. Add your first category."))
                    LazyColumn(Modifier.heightIn(max = 240.dp)) {
                        items(categories, key = { it.id }) { category ->
                            Row(
                                Modifier.fillMaxWidth().selectable(
                                    selected = categoryId == category.id,
                                    enabled = !saving,
                                    role = Role.RadioButton,
                                    onClick = { categoryId = category.id }
                                ).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = categoryId == category.id, onClick = null)
                                Text(category.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    TextButton(onClick = { showCreateCategory = true }, enabled = !saving) {
                        Text(strings("Create category"))
                    }
                    message?.let { Text(strings.runtime(it)) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !saving && categories.any { it.id == categoryId },
                    onClick = {
                        val selectedId = categoryId ?: return@TextButton
                        saving = true
                        onSaveWord(input.trim(), meaning.trim(), selectedId) { success ->
                            saving = false
                            message = if (success) "Word saved." else "Could not save word. Please try again."
                            if (success) showCategories = false
                        }
                    }
                ) { Text(strings("Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showCategories = false }, enabled = !saving) { Text(strings("Cancel")) }
            }
        )
    }
    if (showCreateCategory) {
        CategoryEditDialog(
            title = strings("Create Category"),
            initialValue = "",
            onDismiss = { showCreateCategory = false },
            onConfirm = { name ->
                pendingCategoryName = name
                onAddCategory(name)
                showCreateCategory = false
            }
        )
    }
}
