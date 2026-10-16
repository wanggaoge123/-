package com.github.eprendre.tingshu.widget

import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.github.eprendre.tingshu.App
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.albumArt
import com.github.eprendre.tingshu.extensions.artist
import com.github.eprendre.tingshu.extensions.displaySubtitle
import com.github.eprendre.tingshu.extensions.displayTitle
import com.github.eprendre.tingshu.extensions.downloadStatus
import com.github.eprendre.tingshu.extensions.getBitmapFromVectorDrawable
import com.github.eprendre.tingshu.extensions.mediaUri
import com.github.eprendre.tingshu.extensions.title
import com.github.eprendre.tingshu.extensions.toMediaSource
import com.github.eprendre.tingshu.utils.Prefs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

/**
 * 书源与播放器之间的桥梁。
 * 书源提取到真实的音频地址后，调用 [play] 交给播放器播放。
 */
object PlaybackBridge {
    var exoPlayer: ExoPlayer? = null
    var dataSourceFactory: DataSource.Factory? = null

    fun play(audioUrl: String, autoPlay: Boolean = true, isCache: Boolean = false) {
        if (audioUrl.isBlank()) {
            return
        }
        Handler(Looper.getMainLooper()).post {
            val player = exoPlayer ?: return@post
            val factory = dataSourceFactory
                ?: DefaultDataSourceFactory(
                    App.appContext,
                    Util.getUserAgent(App.appContext, "tingshu")
                )

            var art = App.coverBitmap
            if (art == null) {
                // 兜底封面：矢量图需转成 Bitmap 才能塞进 MediaMetadataCompat
                art = getBitmapFromVectorDrawable(App.appContext, R.drawable.ic_launcher_background)
            }

            val bookName = (Prefs.currentEpisodeName ?: "") + " - " + (Prefs.currentBookName ?: "")

            val metadata = MediaMetadataCompat.Builder()
                .apply {
                    title = bookName
                    artist = Prefs.artist
                    mediaUri = audioUrl
                    displayTitle = bookName
                    displaySubtitle = Prefs.artist
                    downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED.toLong()
                    albumArt = art
                }
                .build()

            try {
                val source = metadata.toMediaSource(factory)
                player.prepare(source)
                player.playWhenReady = autoPlay
                if (Prefs.currentEpisodePosition > 0) {
                    player.seekTo(Prefs.currentEpisodePosition)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun control(action: String) {
        Handler(Looper.getMainLooper()).post {
            val player = exoPlayer ?: return@post
            try {
                when (action) {
                    "play" -> player.playWhenReady = true
                    "pause" -> player.playWhenReady = false
                    "skipToNext" -> player.next()
                    "skipToPrevious" -> player.previous()
                    "stop" -> player.stop()
                    "rewind" -> player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                    "fastForward" -> player.seekTo(player.currentPosition + 10000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
