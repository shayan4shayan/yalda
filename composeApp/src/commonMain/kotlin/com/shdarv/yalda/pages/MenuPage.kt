package com.shdarv.yalda.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.Profile
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.translation.TranslationViewModel
import com.shdarv.yalda.viewmodel.WordsStoreViewModel

enum class MenuSection {
    Home,
    Learn,
    Import,
    Settings,
    About
}

@Composable
fun MenuPage(
    destination: MenuSection,
    profile: Profile?,
    profiles: List<Profile>,
    setupPreference: com.shdarv.yalda.db.ProfileSetupPreference?,
    storeViewModel: WordsStoreViewModel,
    translationViewModel: TranslationViewModel,
    onOpenDestination: (MenuSection) -> Unit,
    onBackToMenu: () -> Unit
) {
    val strings = LocalAppStrings.current
    when (destination) {
        MenuSection.Home -> MenuHome(onOpenDestination = onOpenDestination)
        MenuSection.Learn -> MenuSubPage(title = strings("Learn"), onBack = onBackToMenu) {
            LearnPage(profileId = profile?.id)
        }
        MenuSection.Import -> MenuSubPage(title = strings("Import"), onBack = onBackToMenu) {
            ImportPage(profile = profile, storeViewModel = storeViewModel)
        }
        MenuSection.Settings -> MenuSubPage(title = strings("Settings"), onBack = onBackToMenu) {
            SettingsPage(
                profile = profile,
                profiles = profiles,
                setupPreference = setupPreference,
                translationViewModel = translationViewModel
            )
        }
        MenuSection.About -> MenuSubPage(title = strings("About us"), onBack = onBackToMenu) {
            AboutPage()
        }
    }
}

@Composable
private fun MenuHome(onOpenDestination: (MenuSection) -> Unit) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(strings("Menu"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            MenuItem(
                title = strings("Learn"),
                icon = Icons.Outlined.NotificationsActive,
                onClick = { onOpenDestination(MenuSection.Learn) }
            )
        }
        item {
            MenuItem(
                title = strings("Import"),
                icon = Icons.Outlined.CloudDownload,
                onClick = { onOpenDestination(MenuSection.Import) }
            )
        }
        item {
            MenuItem(
                title = strings("Settings"),
                icon = Icons.Outlined.Settings,
                onClick = { onOpenDestination(MenuSection.Settings) }
            )
        }
        item {
            MenuItem(
                title = strings("About us"),
                icon = Icons.Outlined.Info,
                onClick = { onOpenDestination(MenuSection.About) }
            )
        }
    }
}

@Composable
private fun MenuSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val strings = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings("Back to menu"))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun MenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutPage() {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(strings("Yalda"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(strings("A vocabulary app for building and reviewing language libraries."))
        Text(strings("Offline translations are powered by Google Translate."))
        Text(
            strings("Non-English language pairs may translate through English and can have lower quality."),
            style = MaterialTheme.typography.bodySmall
        )
        Text(strings("Version {0}", "1.0"), style = MaterialTheme.typography.bodySmall)
    }
}
