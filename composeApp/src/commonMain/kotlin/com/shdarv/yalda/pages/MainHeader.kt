package com.shdarv.yalda.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.ui.YaldaTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yalda.composeapp.generated.resources.Res
import yalda.composeapp.generated.resources.yalda_logo

@Composable
fun MainHeader(
    profileName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.yalda_logo),
                contentDescription = "Yalda logo",
                modifier = Modifier
                    .size(42.dp)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = profileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun HeaderPreview() {
    YaldaTheme {
        MainHeader(
            profileName = "Yalda"
        )
    }
}
