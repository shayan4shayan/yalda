package com.shdarv.yalda

import androidx.compose.ui.window.ComposeUIViewController
import com.shdarv.yalda.db.database
import com.shdarv.yalda.db.init
import com.shdarv.yalda.io.init
import com.shdarv.yalda.io.ioDataHandler
import com.shdarv.yalda.platform.appSettings
import com.shdarv.yalda.platform.initialize
import com.shdarv.yalda.translation.IosLocalTranslationService
import com.shdarv.yalda.translation.localTranslations

fun MainViewController() = ComposeUIViewController {
    database.init()
    ioDataHandler.init()
    appSettings.initialize()
    localTranslations.init(IosLocalTranslationService())
    App()
}
