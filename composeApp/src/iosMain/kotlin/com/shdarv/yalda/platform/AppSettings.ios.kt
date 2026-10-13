package com.shdarv.yalda.platform

import platform.Foundation.NSUserDefaults

actual class AppSettingsStore(private val defaults: NSUserDefaults) {
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    actual fun getInt(key: String, defaultValue: Int): Int =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.integerForKey(key).toInt()

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    actual fun getLong(key: String, defaultValue: Long): Long =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.integerForKey(key)

    actual fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    actual fun getString(key: String, defaultValue: String): String =
        defaults.stringForKey(key) ?: defaultValue

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}

fun appSettings.initialize() {
    init(AppSettingsStore(NSUserDefaults.standardUserDefaults))
}
