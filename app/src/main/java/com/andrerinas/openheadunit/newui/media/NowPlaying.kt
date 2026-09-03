package com.andrerinas.openheadunit.newui.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NpSource { FM, AM, DAB, NET, BT, NONE }

data class NowPlaying(
    val source: NpSource = NpSource.NONE,
    val title: String = "Nothing playing",
    val subtitle: String = "Pick a source to get started",
    val isPlaying: Boolean = false,
    val isLive: Boolean = false,
    val positionMs: Long? = null,
    val durationMs: Long? = null,
)

/** Lets whichever source currently owns playback answer the shared transport controls. */
interface NowPlayingCommands {
    fun togglePlay()
    fun next()
    fun prev()
}

/**
 * The "single now-playing model" called for in ANDROID_IMPLEMENTATION.md: every source (FM/AM,
 * DAB, internet radio, Bluetooth media) publishes into this one repository; the Home hero, the
 * Radio hero and the dock's transport controls all read only from here. Whichever source is
 * active also registers itself as [NowPlayingCommands] so prev/play/next route to the right place
 * without Home or the dock needing to know which source is live.
 */
interface NowPlayingRepository {
    val nowPlaying: StateFlow<NowPlaying>
    fun publish(value: NowPlaying)
    fun registerCommands(source: NpSource, commands: NowPlayingCommands?)
    fun togglePlay()
    fun next()
    fun prev()
}

class DefaultNowPlayingRepository : NowPlayingRepository {
    private val _nowPlaying = MutableStateFlow(NowPlaying())
    override val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private var activeCommands: NowPlayingCommands? = null
    private var activeSource: NpSource = NpSource.NONE

    override fun publish(value: NowPlaying) {
        _nowPlaying.value = value
    }

    override fun registerCommands(source: NpSource, commands: NowPlayingCommands?) {
        if (commands == null) {
            if (activeSource == source) activeCommands = null
            return
        }
        activeSource = source
        activeCommands = commands
    }

    override fun togglePlay() = activeCommands?.togglePlay() ?: Unit
    override fun next() = activeCommands?.next() ?: Unit
    override fun prev() = activeCommands?.prev() ?: Unit
}
