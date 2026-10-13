package com.shdarv.yalda.platform

expect class AppSettingsStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
}

object appSettings {
    private var store: AppSettingsStore? = null

    fun init(appSettingsStore: AppSettingsStore) {
        store = appSettingsStore
    }

    fun get(): AppSettingsStore =
        store ?: error("AppSettingsStore not initialized. Call appSettings.init() first.")
}
