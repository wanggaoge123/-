package com.github.eprendre.tingshu.utils

import android.content.Context
import android.content.SharedPreferences
import com.github.eprendre.tingshu.App

/**
 * 书源可用的键值存储
 */
object ExternalSourcePrefs {
    private const val PREFS_FILENAME = "external_sources"
    private val prefs: SharedPreferences by lazy {
        App.appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defValue: String?): String? {
        return prefs.getString(key, defValue)
    }
}
