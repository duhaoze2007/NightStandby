package com.duhaoze.standby.media

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService

/**
 * 只用于取得媒体会话控制权。
 * 不读取、保存或转发任何通知内容：未覆写 onNotificationPosted / onNotificationRemoved。
 */
class MediaListenerService : NotificationListenerService() {

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            MediaSessionStore.onSessionsChanged(this, controllers.orEmpty())
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaSessionStore.attach(this)
        val manager = getSystemService(MediaSessionManager::class.java)
        runCatching {
            manager?.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                ComponentName(this, MediaListenerService::class.java),
                Handler(Looper.getMainLooper())
            )
        }
        MediaSessionStore.refreshSessions()
    }

    override fun onListenerDisconnected() {
        val manager = getSystemService(MediaSessionManager::class.java)
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        MediaSessionStore.detach(this)
        super.onListenerDisconnected()
    }
}
