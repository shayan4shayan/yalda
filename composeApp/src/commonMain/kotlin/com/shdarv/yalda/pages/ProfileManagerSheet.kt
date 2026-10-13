package com.shdarv.yalda.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.localization.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagerSheet(
    profiles: List<Profile>,
    selectedProfile: Profile?,
    message: String?,
    onDismiss: () -> Unit,
    onSwitch: (Profile) -> Unit,
    onCreate: () -> Unit,
    onRename: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onClearMessage: () -> Unit
) {
    val strings = LocalAppStrings.current
    var renaming by remember { mutableStateOf<Profile?>(null) }
    var deleting by remember { mutableStateOf<Profile?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(strings("Profiles"), style = MaterialTheme.typography.titleLarge)
            message?.let {
                Text(strings(it), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onClearMessage) { Text(strings("Dismiss")) }
            }
            LazyColumn {
                items(profiles, key = { it.id }) { profile ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSwitch(profile) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (profile.id == selectedProfile?.id) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = strings("Selected"))
                        } else Spacer(Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile.name)
                            Text("${profile.sourceLanguage.uppercase()} → ${profile.targetLanguage.uppercase()}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { renaming = profile }) { Icon(Icons.Outlined.Edit, contentDescription = strings("Rename")) }
                        IconButton(onClick = { deleting = profile }, enabled = profiles.size > 1) {
                            Icon(Icons.Outlined.Delete, contentDescription = strings("Delete"))
                        }
                    }
                }
            }
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(strings("Create profile"))
            }
        }
    }

    renaming?.let { profile ->
        var name by remember(profile.id) { mutableStateOf(profile.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(strings("Rename profile")) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(profile.copy(name = name)); renaming = null }, enabled = name.isNotBlank()) { Text(strings("Save")) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(strings("Cancel")) } }
        )
    }
    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(strings("Delete {0}?", profile.name)) },
            text = { Text(strings("Its independent categories, words, and setup selections will be deleted.")) },
            confirmButton = { TextButton(onClick = { onDelete(profile); deleting = null }) { Text(strings("Delete")) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(strings("Cancel")) } }
        )
    }
}
