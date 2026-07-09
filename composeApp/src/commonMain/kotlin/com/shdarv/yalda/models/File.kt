package com.shdarv.yalda.models

import kotlinx.serialization.Serializable

@Serializable
data class RemoteFile(val name: String, val address: String)

@Serializable
data class RemoteWordEntry(val word: String, val meaning: String, val description: String)