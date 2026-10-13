package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.db.ProfileSetupPreference
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.translation.TranslationCatalog
import com.shdarv.yalda.translation.TranslationLanguagePackState
import com.shdarv.yalda.translation.TranslationViewModel

@Composable
fun SettingsPage(
    profile: Profile?,
    profiles: List<Profile>,
    setupPreference: ProfileSetupPreference?,
    translationViewModel: TranslationViewModel
) {
    val strings = LocalAppStrings.current
    val state by translationViewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<TranslationLanguagePackState?>(null) }

    LaunchedEffect(profile?.id) {
        translationViewModel.setActivePair(profile?.sourceLanguage, profile?.targetLanguage)
    }

    val pair = state.activePair?.takeIf {
        profile != null &&
            it.sourceLanguage == profile.sourceLanguage &&
            it.targetLanguage == profile.targetLanguage
    }
    val visiblePacks = state.languagePacks.filter { pack ->
        pack.isDownloaded || pair?.requiredLanguagePacks?.contains(pack.language.code) == true
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings("Settings"), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(strings("Offline translation"), style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = translationViewModel::refresh) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else Icon(Icons.Outlined.Refresh, contentDescription = strings("Refresh language packs"))
                }
            }
        }

        if (pair == null) {
            item { Text(strings("Select a profile to manage its translation setup.")) }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            strings(
                                "{0} to {1}",
                                strings.languageName(pair.sourceLanguage),
                                strings.languageName(pair.targetLanguage)
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            strings(
                                "Estimated storage: up to {0} MB · {1}",
                                pair.estimatedSizeMb,
                                strings(if (state.isPairReady) "Ready" else "Not downloaded")
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (setupPreference?.prepareOfflineTranslation == true) {
                                strings("Offline preparation was selected during setup. Downloads still start only here.")
                            } else {
                                strings("Offline preparation was skipped during setup; you can download it now.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.isDownloadingPair) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = translationViewModel::cancelPairDownload) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(strings("Cancel"))
                            }
                        } else if (!state.isPairReady) {
                            OutlinedButton(
                                onClick = { translationViewModel.downloadActivePair(wifiOnly = true) },
                                enabled = state.isSupported
                            ) {
                                Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(strings("Download on Wi-Fi"))
                            }
                        }
                    }
                }
            }
        }

        if (!state.isSupported) {
            item {
                Text(
                    strings("Offline translation is unavailable on this platform (including the iOS Simulator)."),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        state.message?.let { message -> item { Text(strings.runtime(message), style = MaterialTheme.typography.bodySmall) } }

        if (visiblePacks.isNotEmpty()) {
            item { Text(strings("Shared language packs"), style = MaterialTheme.typography.titleMedium) }
            items(visiblePacks, key = { it.language.code }) { pack ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (pack.isDownloaded) Icons.Outlined.CheckCircle else Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = if (pack.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(strings.languageName(pack.language.code))
                            Text(
                                strings("{0} · about {1} MB", pack.language.code.uppercase(), TranslationCatalog.ESTIMATED_PACK_SIZE_MB),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (pack.isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else if (pack.isDownloaded) IconButton(onClick = { pendingDelete = pack }) {
                            Icon(Icons.Outlined.Delete, contentDescription = strings("Delete {0}?", strings.languageName(pack.language.code)))
                        }
                    }
                }
            }
        }

        item {
            Text(
                strings("Use Wi-Fi for model downloads. Non-English pairs may use English as an intermediary and can have lower quality."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    pendingDelete?.let { pack ->
        val affected = profiles.filter { other ->
            other.id != profile?.id &&
                (other.sourceLanguage == pack.language.code || other.targetLanguage == pack.language.code)
        }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings("Delete {0} pack?", strings.languageName(pack.language.code))) },
            text = {
                Text(
                    if (affected.isEmpty()) strings("This profile may lose offline translation readiness.")
                    else strings("Other profiles that will lose offline translation: {0}.", affected.joinToString { it.name })
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    translationViewModel.deleteLanguagePack(pack.language.code)
                    pendingDelete = null
                }) { Text(strings("Delete")) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(strings("Cancel")) } }
        )
    }
}
