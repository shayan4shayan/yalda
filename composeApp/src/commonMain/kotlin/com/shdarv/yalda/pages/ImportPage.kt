package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.models.RemoteFile
import com.shdarv.yalda.viewmodel.WordsStoreViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import kotlinx.coroutines.delay

@Composable
fun ImportPage(
    profile: Profile?,
    storeViewModel: WordsStoreViewModel
) {
    val categories: List<RemoteFile> = storeViewModel.categories
    var pendingImportFile by remember { mutableStateOf<RemoteFile?>(null) }
    val importMessage = storeViewModel.importMessage

    LaunchedEffect(profile?.id, categories.size) {
        storeViewModel.refreshImportedCategories(profile)
    }
    LaunchedEffect(importMessage) {
        if (importMessage != null) {
            delay(3000)
            storeViewModel.clearImportMessage()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Predefined categories", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { storeViewModel.loadFromNetwork() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (importMessage != null) {
            Text(importMessage, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (categories.isEmpty()) {
            Text("No remote categories found.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(categories, key = { it.address }) { file ->
                    val isImported = profile?.let { storeViewModel.isCategoryImported(file) } ?: false
                    val isImporting = storeViewModel.isCategoryImporting(file)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.fillMaxWidth(0.7f)) {
                                Text(file.name)
                                Text(file.address.slice(0..25), style = MaterialTheme.typography.bodySmall)
                                if (isImported) {
                                    Text("Imported", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (profile != null) {
                                        pendingImportFile = file
                                    }
                                },
                                enabled = profile != null && !isImported && !isImporting
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Outlined.CloudDownload, contentDescription = "Import category")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingImportFile != null) {
        AlertDialog(
            onDismissRequest = { pendingImportFile = null },
            title = { Text("Import category") },
            text = { Text("Import ${pendingImportFile?.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = pendingImportFile
                        pendingImportFile = null
                        if (file != null && profile != null) {
                            storeViewModel.loadAndImportCategory(profile, file)
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportFile = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
