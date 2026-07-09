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
}

fun appSettings.initialize() {
    init(AppSettingsStore(NSUserDefaults.standardUserDefaults))
}
