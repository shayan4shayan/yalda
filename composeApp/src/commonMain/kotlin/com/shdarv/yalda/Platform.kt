package com.shdarv.yalda

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform