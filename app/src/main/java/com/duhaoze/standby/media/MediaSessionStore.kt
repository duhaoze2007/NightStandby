package com.duhaoze.standby.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.util.Log
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.ui.graphics.asImageBitmap
import com.duhaoze.standby.data.MediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaSessionStore {

    private val _state = MutableStateFlow(MediaState())
    val state: StateFlow<MediaState> = _state.asStateFlow()

    private var service: MediaListenerService? = null
    private var controller: MediaController? = null
    private var sessionPackage: String? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() {
            refreshSessions()
        }
    }

    fun attach(listenerService: MediaListenerService) {
        service = listenerService
        refreshSessions()
    }

    fun detach(listenerService: MediaListenerService) {
        if (service !== listenerService) return
        service = null
        releaseController()
        _state.value = MediaState(connected = false, accessGranted = hasAccess(listenerService))
    }

    fun onSessionsChanged(listenerService: MediaListenerService, controllers: List<MediaController>) {
        service = listenerService
        pickSession(controllers)
    }

    fun refreshSessions() {
        val listenerService = service ?: return
        val manager = listenerService.getSystemService(MediaSessionManager::class.java)
        val sessions = try {
            manager?.getActiveSessions(
                ComponentName(listenerService, MediaListenerService::class.java)
            ).orEmpty()
        } catch (t: Throwable) {
            emptyList()
        }
        pickSession(sessions)
    }

    fun refreshAccess(context: Context) {
        val granted = hasAccess(context)
        _state.value = _state.value.copy(accessGranted = granted)
        if (!granted) return
        // HyperOS 可能因“自启动”限制拒绝系统绑定监听服务；直接通过
        // MediaSessionManager 读取会话不依赖服务已连接，只要通知使用权已开启。
        refreshFromContext(context)
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, MediaListenerService::class.java)
            )
        }
    }

    fun refreshFromContext(context: Context) {
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return
        val sessions = try {
            manager.getActiveSessions(
                ComponentName(context, MediaListenerService::class.java)
            )
        } catch (t: Throwable) {
            Log.w("StandbyMedia", "getActiveSessions failed: ${t.javaClass.simpleName}")
            return
        }
        pickSession(sessions)
    }

    private fun pickSession(controllers: List<MediaController>) {
        val candidate = controllers
            .filter { c ->
                val state = c.playbackState?.state ?: PlaybackState.STATE_NONE
                val hasMetadata = c.metadata != null
                hasMetadata || state == PlaybackState.STATE_PLAYING ||
                    state == PlaybackState.STATE_PAUSED ||
                    state == PlaybackState.STATE_BUFFERING
            }
            .filter { it.packageName != "com.duhaoze.standby" }
            .maxByOrNull { controllerScore(it) }
        if (candidate == null) {
            releaseController()
            _state.value = _state.value.copy(
                connected = true,
                active = false,
                title = "",
                artist = "",
                album = "",
                artwork = null,
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
                updatedAt = SystemClock.elapsedRealtime()
            )
            return
        }
        if (controller?.sessionToken == candidate.sessionToken) {
            publish()
            return
        }
        releaseController()
        controller = candidate
        sessionPackage = candidate.packageName
        try {
            candidate.registerCallback(callback)
        } catch (_: Throwable) {
        }
        publish()
    }

    private fun controllerScore(c: MediaController): Int {
        val playback = c.playbackState
        val state = playback?.state ?: PlaybackState.STATE_NONE
        var score = when (state) {
            PlaybackState.STATE_PLAYING -> 100
            PlaybackState.STATE_BUFFERING -> 85
            PlaybackState.STATE_PAUSED -> 70
            PlaybackState.STATE_CONNECTING -> 50
            PlaybackState.STATE_STOPPED -> 20
            else -> 10
        }
        if (c.metadata != null) score += 15
        return score
    }

    private fun publish() {
        val c = controller ?: return
        val metadata = try { c.metadata } catch (_: Throwable) { null }
        val playback = try { c.playbackState } catch (_: Throwable) { null }
        val position = playback?.position ?: 0L
        val playing = playback?.state == PlaybackState.STATE_PLAYING ||
            playback?.state == PlaybackState.STATE_BUFFERING
        val speed = if (playing) (playback?.playbackSpeed ?: 1f).coerceAtLeast(0.1f) else 0f
        val elapsed = SystemClock.elapsedRealtime()
        val estimatedPosition = if (playing && playback != null) {
            position + ((elapsed - playback.lastPositionUpdateTime).coerceAtLeast(0L) * speed).toLong()
        } else {
            position
        }
        val duration = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L)
            .coerceAtLeast(0L)
        val artworkBitmap = try {
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        } catch (_: Throwable) {
            null
        }

        _state.value = MediaState(
            accessGranted = true,
            connected = true,
            active = true,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "正在播放",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: "",
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            packageName = sessionPackage ?: c.packageName,
            isPlaying = playing,
            positionMs = estimatedPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = speed,
            artwork = artworkBitmap?.asImageBitmap(),
            canSeek = (playback?.actions ?: 0L) and PlaybackState.ACTION_SEEK_TO != 0L,
            canNext = (playback?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canPrevious = (playback?.actions ?: 0L) and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            updatedAt = elapsed
        )
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun next() {
        runCatching { controller?.transportControls?.skipToNext() }
    }

    fun previous() {
        runCatching { controller?.transportControls?.skipToPrevious() }
    }

    fun seekTo(positionMs: Long) {
        runCatching { controller?.transportControls?.seekTo(positionMs) }
    }

    fun currentPositionMs(): Long {
        val s = _state.value
        if (!s.isPlaying) return s.positionMs
        return s.positionMs + ((SystemClock.elapsedRealtime() - s.updatedAt).coerceAtLeast(0L) * s.speed).toLong()
    }

    private fun releaseController() {
        controller?.let {
            runCatching { it.unregisterCallback(callback) }
        }
        controller = null
        sessionPackage = null
    }

    private fun hasAccess(context: Context): Boolean {
        return try {
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            nm?.isNotificationListenerAccessGranted(
                ComponentName(context, MediaListenerService::class.java)
            ) ?: false
        } catch (_: Throwable) {
            false
        }
    }
}
