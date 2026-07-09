package com.shdarv.yalda.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppSection(val label: String, val icon: ImageVector) {
    Categories("Cats", Icons.Outlined.Category),
    Words("Words", Icons.Outlined.AutoStories),
    Search("Search", Icons.Outlined.Search),
    Quiz("Quiz", Icons.Outlined.Quiz),
    Menu("Menu", Icons.Outlined.Menu)
}
