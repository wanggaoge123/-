/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.eprendre.tingshu.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import com.github.eprendre.tingshu.R
import com.github.eprendre.tingshu.extensions.isPlayEnabled
import com.github.eprendre.tingshu.extensions.isPlaying
import com.github.eprendre.tingshu.extensions.isSkipToNextEnabled
import com.github.eprendre.tingshu.extensions.isSkipToPreviousEnabled

const val NOW_PLAYING_CHANNEL: String = "com.github.eprendre.tingshu.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION: Int = 0xb339

/**
 * Helper class to encapsulate code for building notifications.
 */
class NotificationBuilder(private val context: Context) {
    private val platformNotificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val skipToPreviousAction = NotificationCompat.Action(
            R.drawable.exo_controls_previous,
            context.getString(R.string.notification_skip_to_previous),
            mediaButtonPendingIntent(ACTION_SKIP_TO_PREVIOUS))
    private val playAction = NotificationCompat.Action(
            R.drawable.exo_controls_play,
            context.getString(R.string.notification_play),
            mediaButtonPendingIntent(ACTION_PLAY))
    private val pauseAction = NotificationCompat.Action(
            R.drawable.exo_controls_pause,
            context.getString(R.string.notification_pause),
            mediaButtonPendingIntent(ACTION_PAUSE))
    private val skipToNextAction = NotificationCompat.Action(
            R.drawable.exo_controls_next,
            context.getString(R.string.notification_skip_to_next),
            mediaButtonPendingIntent(ACTION_SKIP_TO_NEXT))
    private val stopPendingIntent = mediaButtonPendingIntent(ACTION_STOP)

    /**
     * 自己造媒体按钮的 PendingIntent，不再用 MediaButtonReceiver 的同名方法。
     *
     * 原因：androidx.media:media:1.2.1 里 `buildMediaButtonPendingIntent` 内部用的是
     * `PendingIntent.getBroadcast(..., 0)`，没有 FLAG_IMMUTABLE；本项目 targetSdk 33，
     * 在 Android 12+ 上会抛 IllegalArgumentException（S+ requires FLAG_IMMUTABLE or
     * FLAG_MUTABLE）——而本类的属性初始化是在 TingShuService.onCreate 里跑的，
     * 一启动就闪退。
     *
     * 另外给每个动作单独的 requestCode：requestCode 相同时（都是 0），
     * 系统的 PendingIntent 匹配只看 action/data/type/component（不含 extras），
     * 5 个按钮会被当成同一个 PendingIntent，实测会串键。
     */
    private fun mediaButtonPendingIntent(action: Long): PendingIntent? {
        val keyCode = PlaybackStateCompat.toKeyCode(action)
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return null
        }
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setComponent(ComponentName(context, MediaButtonReceiver::class.java))
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, keyCode, intent, flags)
    }

    fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata.description
        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)

        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
        var playPauseIndex = 0
        if (playbackState.isSkipToPreviousEnabled) {
            builder.addAction(skipToPreviousAction)
            ++playPauseIndex
        }
        if (playbackState.isPlaying) {
            builder.addAction(pauseAction)
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
        }
        if (playbackState.isSkipToNextEnabled) {
            builder.addAction(skipToNextAction)
        }

        val mediaStyle = MediaStyle()
                .setCancelButtonIntent(stopPendingIntent)
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(playPauseIndex)
                .setShowCancelButton(true)

        return builder.setContentIntent(controller.sessionActivity)
                .setContentText(description.subtitle)
                .setContentTitle(description.title)
                .setDeleteIntent(stopPendingIntent)
                .setLargeIcon(description.iconBitmap)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
    }

    private fun shouldCreateNowPlayingChannel() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
            platformNotificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(NOW_PLAYING_CHANNEL,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = context.getString(R.string.notification_channel_description)
                }

        platformNotificationManager.createNotificationChannel(notificationChannel)
    }
}

