package com.github.eprendre.tingshu.utils

import android.content.Context
import android.content.SharedPreferences
import com.github.eprendre.tingshu.App

/**
 * 书源可用的键值存储
 */
object ExternalSourcePrefs {
    // 同 Prefs：换个独立文件名，避免读到官方版同名文件里类型不一致的值
    private const val PREFS_FILENAME = "external_sources_zc"
    private val prefs: SharedPreferences by lazy {
        App.appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defValue: String?): String? {
        return try {
            prefs.getString(key, defValue)
        } catch (e: Exception) {
            e.printStackTrace()
            defValue
        }
    }
}
