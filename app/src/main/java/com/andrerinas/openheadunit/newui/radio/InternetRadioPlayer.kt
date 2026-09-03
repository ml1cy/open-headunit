package com.andrerinas.openheadunit.newui.radio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Real streaming backend for internet radio (HLS/Icecast/SHOUTcast) via Media3/ExoPlayer, per
 * ANDROID_IMPLEMENTATION.md — no hardware needed, unlike FM/AM/DAB.
 */
class InternetRadioPlayer(context: Context) {

    private val player = ExoPlayer.Builder(context.applicationContext).build()

    var onIsPlayingChanged: (Boolean) -> Unit = {}
    var onError: (String) -> Unit = {}

    init {
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onIsPlayingChanged(isPlaying)
                }

                override fun onPlayerError(error: PlaybackException) {
                    onError(error.message ?: "Playback error")
                }
            },
        )
    }

    val isPlaying: Boolean get() = player.isPlaying
    val positionMs: Long get() = player.currentPosition.coerceAtLeast(0)
    val durationMs: Long get() = player.duration.takeIf { it > 0 } ?: 0L

    fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun pause() = player.pause()
    fun resume() = player.play()
    fun togglePlay() {
        if (player.isPlaying) pause() else resume()
    }

    fun release() = player.release()
}
