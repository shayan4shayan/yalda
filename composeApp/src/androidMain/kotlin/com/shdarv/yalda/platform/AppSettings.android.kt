package com.shdarv.yalda.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

actual class AppSettingsStore(private val preferences: SharedPreferences) {
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    actual fun putBoolean(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }

    actual fun getInt(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    actual fun putInt(key: String, value: Int) {
        preferences.edit { putInt(key, value) }
    }
}

fun appSettings.init(context: Context) {
    val preferences = context.getSharedPreferences("yalda.settings", Context.MODE_PRIVATE)
    init(AppSettingsStore(preferences))
}
