package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.viewmodel.WordsStoreViewModel
import kotlinx.coroutines.delay

@Composable
fun ImportPage(profile: Profile?, storeViewModel: WordsStoreViewModel) {
    val strings = LocalAppStrings.current
    val profileIsActive = profile?.id == storeViewModel.activeProfileId
    val packs = if (profileIsActive) storeViewModel.orderedPacksForActiveProfile() else emptyList()
    val selected = if (profileIsActive) storeViewModel.selectedPacksForActiveProfile() else emptyList()
    val importMessage = storeViewModel.importMessage

    LaunchedEffect(importMessage) {
        if (importMessage != null) {
            delay(5000)
            storeViewModel.clearImportMessage()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(strings("Dataset packs"), style = MaterialTheme.typography.titleMedium)
                Text(strings("Selected during setup appear first."), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = storeViewModel::loadFromNetwork) {
                if (storeViewModel.isCatalogLoading) CircularProgressIndicator(strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, contentDescription = strings("Refresh catalog"))
            }
        }

        Spacer(Modifier.height(10.dp))
        if (selected.isNotEmpty()) {
            OutlinedButton(
                onClick = storeViewModel::importSelected,
                enabled = profile != null && selected.none(storeViewModel::isPackImporting),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                Spacer(Modifier.padding(3.dp))
                Text(strings("Import selected ({0})", selected.size))
            }
        }
        storeViewModel.catalogMessage?.let { Text(strings.runtime(it), style = MaterialTheme.typography.bodySmall) }
        importMessage?.let { Text(strings.runtime(it), style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))

        if (packs.isEmpty()) {
            Text(
                if (storeViewModel.isCatalogLoading) strings("Loading compatible packs...")
                else strings("No packs are available for this directed language pair.")
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(packs, key = { it.id }) { pack ->
                    val installed = storeViewModel.isPackInstalled(pack)
                    val importing = storeViewModel.isPackImporting(pack)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(pack.title)
                                Text(
                                    "v${pack.version} · ${if (pack.legacy) strings("Legacy") else strings("{0} words", pack.wordCount)} · ${pack.license}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (pack.id in selected.map { it.id }) Text(strings("Selected in setup"), style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { if (profile != null) storeViewModel.loadAndImportCategory(profile, pack) },
                                enabled = profile != null && !importing
                            ) {
                                if (importing) CircularProgressIndicator(strokeWidth = 2.dp)
                                else Text(strings(if (installed) "Update" else "Import"))
                            }
                        }
                    }
                }
            }
        }
    }
}
