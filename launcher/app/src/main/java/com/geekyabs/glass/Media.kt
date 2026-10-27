package com.geekyabs.glass

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/** Does nothing; it exists so Glass can be given notification access, which is what lets it see media sessions. */
class MediaListener : NotificationListenerService()

class NowPlaying(
    val pkg: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val duration: Long,
    val playing: Boolean,
    private val position: Long,
    private val updated: Long, // elapsedRealtime at which [position] was true
    private val speed: Float,
    val controls: MediaController.TransportControls,
) {
    fun positionAt(elapsedRealtime: Long) = if (playing) position + ((elapsedRealtime - updated) * speed).toLong() else position
}

/** The session worth showing: whatever is playing, else whatever is paused. Null without notification access. */
@Composable
fun rememberNowPlaying(): NowPlaying? {
    val ctx = LocalContext.current
    var np by remember { mutableStateOf<NowPlaying?>(null) }
    DisposableEffect(Unit) {
        val msm = ctx.getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(ctx, MediaListener::class.java)
        val main = Handler(Looper.getMainLooper())
        var sessions = emptyList<MediaController>()
        fun update() {
            val c = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED || it.playbackState?.state == PlaybackState.STATE_BUFFERING }
            np = c?.let(::read)
        }
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = update()
            override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        }
        fun track(list: List<MediaController>?) {
            sessions.forEach { it.unregisterCallback(callback) }
            sessions = list.orEmpty()
            sessions.forEach { it.registerCallback(callback, main) }
            update()
        }
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { track(it) }
        val granted = runCatching { // SecurityException until access is granted over adb
            msm.addOnActiveSessionsChangedListener(listener, component, main)
            track(msm.getActiveSessions(component))
        }.isSuccess
        onDispose {
            sessions.forEach { it.unregisterCallback(callback) }
            if (granted) msm.removeOnActiveSessionsChangedListener(listener)
        }
    }
    return np
}

private fun read(c: MediaController): NowPlaying? {
    val m = c.metadata ?: return null
    val s = c.playbackState ?: return null
    val title = m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
    val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
    val art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    return NowPlaying(
        c.packageName, title, artist, art?.asImageBitmap(), m.getLong(MediaMetadata.METADATA_KEY_DURATION),
        s.state == PlaybackState.STATE_PLAYING, s.position, s.lastPositionUpdateTime, s.playbackSpeed, c.transportControls,
    )
}
