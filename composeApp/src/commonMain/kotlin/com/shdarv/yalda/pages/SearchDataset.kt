package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.viewmodel.rememberProfilesViewModel
import com.shdarv.yalda.viewmodel.rememberWordStoreViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload


@Composable
fun SearchDatasetPageView() {

    val profilesViewModel = rememberProfilesViewModel()
    val viewModel = rememberWordStoreViewModel()

    val selectedProfile by profilesViewModel.selectedProfile.collectAsState()

    val files = viewModel.categories


    LazyColumn {
        items(files) {

            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Text(text = it.name)

                Spacer(Modifier.weight(1f))

                OutlinedButton(onClick = {
                    selectedProfile?.let { p ->
                        viewModel.loadAndImportCategory(p, it)
                    }
                }) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = "Import")
                }
            }

        }
    }

}
