package com.shdarv.yalda.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppSection(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Search("Library", Icons.Outlined.Folder),
    Quiz("Quiz", Icons.Outlined.Quiz),
    Menu("Menu", Icons.Outlined.Menu)
}
