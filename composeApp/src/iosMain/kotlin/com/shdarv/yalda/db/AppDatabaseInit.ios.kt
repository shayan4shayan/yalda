package com.shdarv.yalda.db

import com.shdarv.yalda.getDatabaseBuilder

// Platform-specific init for iOS
fun database.init() {
    if (!isInitialized()) {
        val builder = getDatabaseBuilder()
        init(builder)
    }
}

