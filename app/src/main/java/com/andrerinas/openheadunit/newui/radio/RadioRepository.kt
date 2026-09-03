package com.andrerinas.openheadunit.newui.radio

import kotlinx.coroutines.flow.StateFlow

enum class RadioSource { FM, AM, DAB, NET, BT }

data class StationEntry(
    val id: String,
    val primary: String,
    val secondary: String,
    val signal: Int = 3,
    val isFavorite: Boolean = false,
)

data class PresetSlot(val index: Int, val entry: StationEntry?)

data class RadioUiState(
    val source: RadioSource = RadioSource.FM,
    val tunable: Boolean = true,
    val freqFm: Float = 101.5f,
    val freqAm: Int = 720,
    val pick: Int = 0,
    val playing: Boolean = true,
    val scanning: Boolean = false,
    val list: List<StationEntry> = emptyList(),
    val presets: List<PresetSlot> = emptyList(),
    val signal: Int = 3,
    /** False when the backend this source needs (USB tuner, network) isn't available. */
    val connected: Boolean = false,
    val statusNote: String? = null,
    val elapsedMs: Long = 0L,
    val durationMs: Long = 0L,
)

/**
 * Everything the Radio screen and Home's "Last station" recents card need. FM/AM is backed by a
 * real USB-serial tuner driver (see [UsbSerialFmAmTuner]); Internet is backed by real
 * ExoPlayer/Media3 streaming; DAB+ has no in-repo hardware driver (a separate DAB demodulator is
 * out of scope, see ANDROID_IMPLEMENTATION.md) so it surfaces `connected = false` with a
 * `statusNote` rather than pretending to tune; Bluetooth here means locally scanned media
 * (MediaStore), not an A2DP sink — see PhoneRepository/telephony docs for why true A2DP sink
 * audio needs a privileged build this project doesn't assume.
 */
interface RadioRepository {
    val state: StateFlow<RadioUiState>
    val lastStation: StateFlow<StationEntry?>

    fun selectSource(source: RadioSource)
    fun tuneTo(freqMHz: Float)
    fun tuneToAm(freqKHz: Int)
    fun stepUp()
    fun stepDown()
    fun seekUp()
    fun seekDown()
    fun scan()
    fun autoStore()
    fun selectListIndex(index: Int)
    fun prevItem()
    fun nextItem()
    fun togglePlay()
    fun toggleFavorite()
    fun pickPreset(index: Int)
}
