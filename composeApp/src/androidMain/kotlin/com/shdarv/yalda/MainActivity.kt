package com.shdarv.yalda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.shdarv.yalda.db.database
import com.shdarv.yalda.db.getDatabaseBuilder
import com.shdarv.yalda.io.init
import com.shdarv.yalda.io.ioDataHandler
import com.shdarv.yalda.platform.appSettings
import com.shdarv.yalda.platform.init
import com.shdarv.yalda.translation.MlKitLocalTranslationService
import com.shdarv.yalda.translation.localTranslations

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        database.init(getDatabaseBuilder(this))
        ioDataHandler.init(this)
        appSettings.init(this)
        localTranslations.init(MlKitLocalTranslationService(this))

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
