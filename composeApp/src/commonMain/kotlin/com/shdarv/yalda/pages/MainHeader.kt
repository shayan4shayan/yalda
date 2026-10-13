package com.shdarv.yalda.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shdarv.yalda.localization.LocalAppStrings
import com.shdarv.yalda.ui.YaldaTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yalda.composeapp.generated.resources.Res
import yalda.composeapp.generated.resources.yalda_logo

@Composable
fun MainHeader(
    profileName: String,
    sourceLanguage: String?,
    targetLanguage: String?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val source = supportedLanguageOptions.firstOrNull { it.code == sourceLanguage }
    val target = supportedLanguageOptions.firstOrNull { it.code == targetLanguage }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.yalda_logo),
                contentDescription = strings("Yalda"),
                modifier = Modifier
                    .size(42.dp)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = strings("Yalda"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    role = Role.Button
                    contentDescription = buildString {
                        append(strings("Switch profile"))
                        append(". ")
                        append(profileName)
                        source?.let { append(", ${strings.languageName(it.code)}") }
                        target?.let { append(" → ${strings.languageName(it.code)}") }
                    }
                }
                .clickable(onClick = onProfileClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(source?.flag ?: "🌐", fontSize = 22.sp)
            Text(
                text = "→",
                modifier = Modifier.padding(horizontal = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(target?.flag ?: "🌐", fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview
@Composable
fun HeaderPreview() {
    YaldaTheme {
        MainHeader(
            profileName = "English to Persian",
            sourceLanguage = "en",
            targetLanguage = "fa",
            onProfileClick = {}
        )
    }
}
