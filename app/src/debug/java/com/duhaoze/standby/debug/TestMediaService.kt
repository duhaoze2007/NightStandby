package com.duhaoze.standby.debug

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import com.duhaoze.standby.R

/** Debug-only foreground media session used to verify the standby music card. */
class TestMediaService : Service() {

    private var player: MediaPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("test_media", "Standby test", NotificationManager.IMPORTANCE_LOW)
        )
        val notification: Notification = Notification.Builder(this, "test_media")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Standby media fixture")
            .setContentText("Local test tone")
            .build()
        startForeground(7, notification)

        player = MediaPlayer.create(this, R.raw.test_tone)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
        }
        val artwork = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        Canvas(artwork).apply {
            drawColor(Color.rgb(24, 24, 24))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(230, 181, 104) }
            drawCircle(256f, 256f, 160f, paint)
            paint.color = Color.rgb(8, 8, 8)
            drawCircle(256f, 256f, 80f, paint)
        }
        session = MediaSession(this, "StandbyTest").apply {
            setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "夜航测试曲")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Local Session")
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, "Offline Demo")
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, 40_000L)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                    .build()
            )
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackState.ACTION_SEEK_TO
                    )
                    .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                    .build()
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    player?.start()
                    updateState(PlaybackState.STATE_PLAYING)
                }

                override fun onPause() {
                    player?.pause()
                    updateState(PlaybackState.STATE_PAUSED)
                }

                override fun onSeekTo(pos: Long) {
                    player?.seekTo(pos.toInt())
                    updateState(if (player?.isPlaying == true) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED)
                }

                override fun onSkipToNext() {
                    onSeekTo(10_000L)
                }

                override fun onSkipToPrevious() {
                    onSeekTo(0L)
                }
            })
            isActive = true
        }
    }

    private fun updateState(state: Int) {
        val session = session ?: return
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO
                )
                .setState(state, player?.currentPosition?.toLong() ?: 0L, 1f)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        session?.isActive = false
        session?.release()
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
