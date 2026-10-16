package com.github.eprendre.tingshu

import android.content.Context
import android.graphics.Bitmap
import androidx.multidex.MultiDexApplication
import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.Episode
import com.github.eprendre.tingshu.utils.Prefs

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Prefs.init()
    }

    companion object {
        lateinit var appContext: Context
        var playList: List<Episode> = emptyList()
        var coverBitmap: Bitmap? = null

        /** 当前正在播放/浏览的书籍 */
        var currentBook: Book? = null

        /** 今日播放时长（毫秒），供书源查询 */
        var todayPlaybackTime: Long = 0

        fun currentEpisodeIndex() = playList.indexOfFirst { it.url == Prefs.currentEpisodeUrl }

        fun currentEpisode() = playList.firstOrNull { it.url == Prefs.currentEpisodeUrl }
    }
}
