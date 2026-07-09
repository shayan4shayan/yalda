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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.translation.TranslationViewModel

@Composable
fun SettingsPage(translationViewModel: TranslationViewModel) {
    val state by translationViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        translationViewModel.refresh()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("AI translation models", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = { translationViewModel.refresh() }) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh models")
                    }
                }
            }
        }

        if (!state.isSupported) {
            item {
                Text(
                    text = "Offline translation is not available on this platform.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        state.message?.let { message ->
            item {
                Text(
                    text = message,
                    color = if (message.contains("failed", ignoreCase = true)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(state.models, key = { it.model.id }) { modelState ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (modelState.isDownloaded) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.CloudDownload
                        },
                        contentDescription = null,
                        tint = if (modelState.isDownloaded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(modelState.model.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${modelState.model.sourceLanguage.uppercase()} to ${modelState.model.targetLanguage.uppercase()} - ${modelState.model.sizeMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val statusText = when {
                            modelState.isBusy -> modelState.status ?: "Working..."
                            modelState.isDownloaded -> "Ready"
                            else -> "Not downloaded"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (modelState.isDownloaded && !modelState.isBusy) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (modelState.isBusy) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = modelState.downloadProgress
                            if (progress == null) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    when {
                        modelState.isBusy -> {
                            OutlinedButton(
                                onClick = { translationViewModel.cancelModelDownload(modelState.model.id) }
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cancel download")
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Cancel")
                            }
                        }
                        modelState.isDownloaded -> {
                            OutlinedButton(
                                onClick = { translationViewModel.deleteModel(modelState.model.id) },
                                enabled = state.isSupported
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete model")
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Delete")
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = { translationViewModel.downloadModel(modelState.model.id) },
                                enabled = state.isSupported
                            ) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = "Download model")
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
