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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Category
import com.shdarv.yalda.db.WordEntry
import com.shdarv.yalda.viewmodel.SearchViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    searchViewModel: SearchViewModel,
    categoriesById: Map<Long, Category>,
    onOpenCategory: (Category) -> Unit,
    onOpenWord: (WordEntry) -> Unit
) {
    val query by searchViewModel.query.collectAsState()
    val categories by searchViewModel.categories.collectAsState()
    val words by searchViewModel.words.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { searchViewModel.setQuery(it) },
                label = { Text("Search categories and words") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Categories", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (categories.isEmpty()) {
            item { Text("No categories match.") }
        } else {
            items(categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category.name)
                            IconButton(onClick = { onOpenCategory(category) }) {
                                Icon(Icons.Outlined.ArrowForward, contentDescription = "Open category")
                            }
                        }
                    }
                }
            }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Words", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (words.isEmpty()) {
            item { Text("No words match.") }
        } else {
            items(words, key = { it.wordId }) { word ->
                val categoryName = categoriesById[word.categoryIdFk]?.name ?: "Unknown"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(word.word)
                            IconButton(onClick = { onOpenWord(word) }) {
                                Icon(Icons.Outlined.ArrowForward, contentDescription = "Open word")
                            }
                        }
                        Text(word.meaning, style = MaterialTheme.typography.bodyMedium)
                        Text("Category: $categoryName", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
