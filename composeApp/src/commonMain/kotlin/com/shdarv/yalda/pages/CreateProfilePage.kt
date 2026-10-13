package com.shdarv.yalda.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shdarv.yalda.models.DatasetPack
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.translation.TranslationCatalog

data class LanguageOption(val name: String, val code: String, val flag: String)

val supportedLanguageOptions = listOf(
    LanguageOption("English", "en", "🇺🇸"),
    LanguageOption("Spanish", "es", "🇪🇸"),
    LanguageOption("French", "fr", "🇫🇷"),
    LanguageOption("German", "de", "🇩🇪"),
    LanguageOption("Japanese", "ja", "🇯🇵"),
    LanguageOption("Persian", "fa", "🇮🇷"),
    LanguageOption("Italian", "it", "🇮🇹"),
    LanguageOption("Portuguese", "pt", "🇵🇹")
)

@Composable
fun CreateProfilePage(
    canDismiss: Boolean,
    catalogPacks: List<DatasetPack>,
    isCatalogLoading: Boolean,
    catalogMessage: String?,
    onRetryCatalog: () -> Unit,
    onBack: () -> Unit,
    onCreateProfile: (
        name: String,
        sourceLanguage: String,
        targetLanguage: String,
        selectedPackIds: Set<String>,
        prepareOfflineTranslation: Boolean
    ) -> Unit,
    availableLanguages: List<LanguageOption> = supportedLanguageOptions
) {
    val strings = LocalAppStrings.current
    var step by remember { mutableStateOf(1) }
    var source by remember { mutableStateOf(availableLanguages.first()) }
    var target by remember { mutableStateOf(availableLanguages.first { it.code != source.code }) }
    var selectedPackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var prepareOfflineTranslation by remember { mutableStateOf(true) }

    val compatiblePacks = catalogPacks.filter {
        it.sourceLanguage == source.code && it.targetLanguage == target.code
    }
    val pair = TranslationCatalog.pairFor(source.code, target.code)

    LaunchedEffect(source.code, target.code, compatiblePacks.map { it.id }) {
        selectedPackIds = compatiblePacks.filter(DatasetPack::recommendedDefault).map(DatasetPack::id).toSet()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (step > 1) step -= 1 else onBack() },
                enabled = step > 1 || canDismiss
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings("Back"))
            }
            Text(
                strings("Create profile"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(strings("{0} of 3", step), style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { step / 3f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (step) {
                1 -> LanguageStep(
                    languages = availableLanguages,
                    source = source,
                    target = target,
                    onSource = { selected ->
                        source = selected
                        if (target.code == selected.code) target = availableLanguages.first { it.code != selected.code }
                    },
                    onTarget = { selected -> if (selected.code != source.code) target = selected }
                )
                2 -> PackStep(
                    packs = compatiblePacks,
                    selectedPackIds = selectedPackIds,
                    loading = isCatalogLoading,
                    message = catalogMessage,
                    onRetry = onRetryCatalog,
                    onToggle = { id ->
                        selectedPackIds = if (id in selectedPackIds) selectedPackIds - id else selectedPackIds + id
                    }
                )
                else -> TranslationStep(
                    source = source,
                    target = target,
                    estimatedSizeMb = pair?.estimatedSizeMb ?: 0,
                    prepare = prepareOfflineTranslation,
                    onPrepareChange = { prepareOfflineTranslation = it },
                    selectedPackCount = selectedPackIds.size
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 1) {
                OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) {
                    Text(strings("Back"))
                }
            }
            Button(
                onClick = {
                    if (step < 3) step += 1
                    else onCreateProfile(
                        strings("{0} to {1}", strings.languageName(source.code), strings.languageName(target.code)),
                        source.code,
                        target.code,
                        selectedPackIds,
                        prepareOfflineTranslation
                    )
                },
                enabled = source.code != target.code,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings(if (step == 3) "Create profile" else if (step == 2 && compatiblePacks.isEmpty()) "Skip & continue" else "Continue"))
            }
        }
    }
}

@Composable
private fun LanguageStep(
    languages: List<LanguageOption>,
    source: LanguageOption,
    target: LanguageOption,
    onSource: (LanguageOption) -> Unit,
    onTarget: (LanguageOption) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(strings("Choose languages"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(strings("Choose an exact source and target. They cannot be the same."))
        Spacer(Modifier.height(20.dp))
        LanguageDropdown(
            label = strings("Source language"),
            languages = languages,
            selected = source,
            onSelect = onSource
        )
        Spacer(Modifier.height(16.dp))
        LanguageDropdown(
            label = strings("Target language"),
            languages = languages.filter { it.code != source.code },
            selected = target,
            onSelect = onTarget
        )
    }
}

@Composable
private fun LanguageDropdown(
    label: String,
    languages: List<LanguageOption>,
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit
) {
    val strings = LocalAppStrings.current
    val density = LocalDensity.current
    var expanded by remember(languages.map(LanguageOption::code)) { mutableStateOf(false) }
    var menuWidth by remember { mutableStateOf(0.dp) }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    menuWidth = with(density) { coordinates.size.width.toDp() }
                }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (expanded) 2.dp else 1.dp,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (expanded) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selected.flag, fontSize = 25.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.languageName(selected.code),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            selected.code.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(menuWidth)
            ) {
                languages.forEach { language ->
                    val active = language.code == selected.code
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(language.flag, fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        strings.languageName(language.code),
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        language.code.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            if (active) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = strings("Selected"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            onSelect(language)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackStep(
    packs: List<DatasetPack>,
    selectedPackIds: Set<String>,
    loading: Boolean,
    message: String?,
    onRetry: () -> Unit,
    onToggle: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(Modifier.fillMaxSize()) {
        Text(strings("Choose category packs"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(strings("Recommended packs are preselected. Nothing is imported until you use Import."))
        Spacer(Modifier.height(12.dp))
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(strings("Loading compatible packs..."))
            }
        } else if (packs.isEmpty()) {
            Text(strings("No compatible packs are currently available. You can continue with an empty library."))
            message?.let { Text(strings.runtime(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = onRetry) { Text(strings("Retry catalog")) }
        } else {
            message?.let { Text(strings.runtime(it), style = MaterialTheme.typography.bodySmall) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(packs, key = { it.id }) { pack ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onToggle(pack.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = pack.id in selectedPackIds, onCheckedChange = { onToggle(pack.id) })
                            Column(Modifier.weight(1f)) {
                                Text(pack.title, fontWeight = FontWeight.SemiBold)
                                Text(strings("{0} words · v{1} · {2}", pack.wordCount, pack.version, pack.license), style = MaterialTheme.typography.bodySmall)
                            }
                            if (pack.recommendedDefault) Icon(Icons.Outlined.CheckCircle, contentDescription = strings("Recommended"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationStep(
    source: LanguageOption,
    target: LanguageOption,
    estimatedSizeMb: Int,
    prepare: Boolean,
    onPrepareChange: (Boolean) -> Unit,
    selectedPackCount: Int
) {
    val strings = LocalAppStrings.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings("Offline translation"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("${source.flag} ${strings.languageName(source.code)} → ${target.flag} ${strings.languageName(target.code)}", style = MaterialTheme.typography.titleMedium)
                Text(strings("Estimated model storage: up to {0} MB", estimatedSizeMb), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(strings("Prepare offline translation"))
                        Text(strings("Save this preference for Settings"), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = prepare, onCheckedChange = onPrepareChange)
                }
            }
        }
        Text(strings("Use Wi-Fi when downloading models. Profile creation will not download anything."))
        Text(strings("{0} dataset pack(s) selected. Importing remains a separate action.", selectedPackCount))
        if (source.code != "en" && target.code != "en") {
            Text(
                strings("This pair may use English as an intermediary and can have lower translation quality."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
