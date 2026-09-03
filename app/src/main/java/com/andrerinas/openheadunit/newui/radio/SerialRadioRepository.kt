package com.andrerinas.openheadunit.newui.radio

import android.content.Context
import com.andrerinas.openheadunit.newui.media.NowPlaying
import com.andrerinas.openheadunit.newui.media.NowPlayingCommands
import com.andrerinas.openheadunit.newui.media.NowPlayingRepository
import com.andrerinas.openheadunit.newui.media.NpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class FmSeed(val mhz: Float, val name: String, val genre: String, val rds: String, val signal: Int)
private data class AmSeed(val khz: Int, val name: String, val genre: String)
private data class DabSeed(val service: String, val ensemble: String, val genre: String)
private data class NetSeed(val name: String, val place: String, val genre: String, val url: String)

/**
 * Unifies FM/AM (real, via [UsbSerialFmAmTuner]), Internet radio (real, via [InternetRadioPlayer]
 * / Media3), local media (real, via [LocalMediaLibrary] — the honest Bluetooth-A2DP-sink
 * substitute, see PhoneRepository/telephony docs) and DAB+ (no real backend available on stock
 * Android hardware, surfaced as `connected = false`) behind one [RadioRepository]. Publishes into
 * the shared [NowPlayingRepository] spine so Home reads the same value.
 */
class SerialRadioRepository(
    private val context: Context,
    private val nowPlayingRepository: NowPlayingRepository,
) : RadioRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tuner = UsbSerialFmAmTuner(context)
    private val streamPlayer = InternetRadioPlayer(context)
    private val localPlayer = InternetRadioPlayer(context)

    private val prefs = context.getSharedPreferences("newui_radio", Context.MODE_PRIVATE)
    private val favorites = mutableSetOf<String>().apply {
        addAll(prefs.getString("favorites", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
    }

    private val fmSeeds = listOf(
        FmSeed(88.6f, "Radio One", "News & Talk", "Morning Report", 3),
        FmSeed(90.9f, "City FM", "Top 40", "Dua Lipa - Illusion", 2),
        FmSeed(94.3f, "Jazz FM", "Jazz", "Bill Evans - Peace Piece", 4),
        FmSeed(98.1f, "Talk 98", "Talk", "Drive Time Debate", 2),
        FmSeed(101.5f, "Radio Nova", "Alternative", "Tame Impala - The Less I Know The Better", 4),
        FmSeed(103.2f, "Classic", "Classical", "Ravel - Bolero", 3),
        FmSeed(105.8f, "Kiss", "Dance", "Fred again.. - Delilah", 3),
        FmSeed(106.4f, "Retro", "80s & 90s", "a-ha - Take On Me", 2),
    )
    private val amSeeds = listOf(
        AmSeed(540, "Regional", "News"),
        AmSeed(720, "Sport", "Sport"),
        AmSeed(1044, "Country", "Country"),
        AmSeed(1350, "Voice", "Talk"),
    )
    private val dabSeeds = listOf(
        DabSeed("Nova DAB", "12B", "Alternative"),
        DabSeed("Deep House 24", "12B", "Electronic"),
        DabSeed("News Now", "11D", "News"),
        DabSeed("Classical Hall", "11D", "Classical"),
        DabSeed("Retro Gold", "12A", "Oldies"),
        DabSeed("Chill Lounge", "12A", "Ambient"),
    )

    // Long-standing, widely-documented public radio streams — real, playable URLs, not placeholders.
    private val netSeeds = listOf(
        NetSeed("KEXP Live", "Seattle", "Indie", "https://kexp-mp3-128.streamguys1.com/kexp128.mp3"),
        NetSeed("NTS 1", "London", "Eclectic", "https://stream-relay-geo.ntslive.net/stream"),
        NetSeed("SomaFM Drone Zone", "San Francisco", "Ambient", "https://ice1.somafm.com/dronezone-128-mp3"),
        NetSeed("Radio Paradise", "Mixed", "Eclectic", "https://stream.radioparadise.com/mp3-192"),
    )

    private var localTracks: List<LocalTrack> = emptyList()
    private val netUrlById = netSeeds.associate { "net:${it.name}" to it.url }

    private val _state = MutableStateFlow(buildInitialState())
    override val state: StateFlow<RadioUiState> = _state.asStateFlow()

    private val _lastStation = MutableStateFlow<StationEntry?>(null)
    override val lastStation: StateFlow<StationEntry?> = _lastStation.asStateFlow()

    init {
        streamPlayer.onIsPlayingChanged = { playing -> onPlayerPlayingChanged(NpSource.NET, playing) }
        localPlayer.onIsPlayingChanged = { playing -> onPlayerPlayingChanged(NpSource.BT, playing) }
        registerTunableCommands(RadioSource.FM)
        scope.launch {
            val connected = tuner.connect()
            _state.update { it.copy(connected = connected || it.source !in TUNABLE, statusNote = if (connected) null else tunerUnavailableNote()) }
            if (connected) refreshTunerStatus()
        }
        scope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                val player = when (s.source) {
                    RadioSource.NET -> streamPlayer
                    RadioSource.BT -> localPlayer
                    else -> null
                }
                if (player != null) {
                    _state.update { it.copy(elapsedMs = player.positionMs, durationMs = player.durationMs) }
                }
            }
        }
    }

    private fun buildInitialState(): RadioUiState {
        val list = fmSeeds.map { fmEntry(it) }
        return RadioUiState(
            source = RadioSource.FM,
            tunable = true,
            freqFm = 101.5f,
            list = list,
            presets = presetsFor(list),
            connected = false,
            statusNote = tunerUnavailableNote(),
        )
    }

    override fun selectSource(source: RadioSource) {
        pausePlayers()
        val list = listFor(source)
        _state.update {
            it.copy(
                source = source,
                tunable = source == RadioSource.FM || source == RadioSource.AM,
                pick = 0,
                playing = false,
                list = list,
                presets = presetsFor(list),
                connected = when (source) {
                    RadioSource.FM, RadioSource.AM -> tuner.isConnected
                    RadioSource.DAB -> false
                    RadioSource.NET, RadioSource.BT -> true
                },
                statusNote = when (source) {
                    RadioSource.FM, RadioSource.AM -> if (tuner.isConnected) null else tunerUnavailableNote()
                    RadioSource.DAB -> "No DAB+ demodulator available on this hardware"
                    else -> null
                },
            )
        }
        if (source == RadioSource.BT) refreshLocalTracks()
        if (source == RadioSource.FM || source == RadioSource.AM) {
            registerTunableCommands(source)
            if (tuner.isConnected) scope.launch { refreshTunerStatus() }
        }
        publishNowPlaying()
    }

    private fun registerTunableCommands(source: RadioSource) {
        val np = if (source == RadioSource.FM) NpSource.FM else NpSource.AM
        nowPlayingRepository.registerCommands(
            np,
            object : NowPlayingCommands {
                override fun togglePlay() = Unit
                override fun next() = seekUp()
                override fun prev() = seekDown()
            },
        )
    }

    override fun tuneTo(freqMHz: Float) {
        val clamped = (freqMHz * 10).roundToInt() / 10f
        val bounded = clamped.coerceIn(87.5f, 108.0f)
        _state.update { it.copy(freqFm = bounded) }
        applyDerivedFm(bounded)
        if (tuner.isConnected) scope.launch { applyTunerResult(tuner.tune((bounded * 1000).roundToInt())) }
    }

    override fun tuneToAm(freqKHz: Int) {
        val bounded = freqKHz.coerceIn(522, 1620)
        _state.update { it.copy(freqAm = bounded) }
        applyDerivedAm(bounded)
        if (tuner.isConnected) scope.launch { applyTunerResult(tuner.tune(bounded)) }
    }

    override fun stepUp() = stepBy(1)
    override fun stepDown() = stepBy(-1)

    private fun stepBy(direction: Int) {
        val s = _state.value
        when (s.source) {
            RadioSource.FM -> tuneTo(s.freqFm + direction * 0.1f)
            RadioSource.AM -> tuneToAm(s.freqAm + direction * 9)
            else -> Unit
        }
    }

    override fun seekUp() = seek(true)
    override fun seekDown() = seek(false)

    private fun seek(up: Boolean) {
        val s = _state.value
        if (!s.tunable) return
        if (tuner.isConnected) {
            scope.launch { applyTunerResult(tuner.seek(up)) }
            return
        }
        // Software fallback: jump to the nearest known station in the requested direction, wrapping at the band edge.
        val seeds = if (s.source == RadioSource.FM) fmSeeds.map { it.mhz } else amSeeds.map { it.khz.toFloat() }
        if (seeds.isEmpty()) return
        val current = if (s.source == RadioSource.FM) s.freqFm else s.freqAm.toFloat()
        val sorted = seeds.sorted()
        val next = if (up) {
            sorted.firstOrNull { it > current } ?: sorted.first()
        } else {
            sorted.lastOrNull { it < current } ?: sorted.last()
        }
        if (s.source == RadioSource.FM) tuneTo(next) else tuneToAm(next.roundToInt())
    }

    override fun scan() {
        val s = _state.value
        if (!s.tunable || s.scanning) return
        _state.update { it.copy(scanning = true) }
        scope.launch {
            delay(1600)
            seek(true)
            _state.update { it.copy(scanning = false) }
        }
    }

    override fun autoStore() {
        val s = _state.value
        if (!s.tunable) return
        val current = derivedCurrentEntry(s) ?: return
        val presets = s.presets.toMutableList()
        val emptyIndex = presets.indexOfFirst { it.entry == null }
        if (emptyIndex >= 0) {
            presets[emptyIndex] = PresetSlot(emptyIndex + 1, current)
            _state.update { it.copy(presets = presets) }
        }
    }

    override fun selectListIndex(index: Int) {
        val s = _state.value
        if (index !in s.list.indices) return
        _state.update { it.copy(pick = index) }
        if (s.source == RadioSource.NET || s.source == RadioSource.BT) {
            playListEntry(s.source, s.list[index])
        }
    }

    override fun prevItem() {
        val s = _state.value
        if (s.tunable) {
            seekDown()
        } else if (s.list.isNotEmpty()) {
            selectListIndex((s.pick - 1 + s.list.size) % s.list.size)
        }
    }

    override fun nextItem() {
        val s = _state.value
        if (s.tunable) {
            seekUp()
        } else if (s.list.isNotEmpty()) {
            selectListIndex((s.pick + 1) % s.list.size)
        }
    }

    override fun togglePlay() {
        val s = _state.value
        when (s.source) {
            RadioSource.NET -> streamPlayer.togglePlay()
            RadioSource.BT -> localPlayer.togglePlay()
            else -> Unit
        }
    }

    override fun toggleFavorite() {
        val s = _state.value
        val entry = derivedCurrentEntry(s) ?: return
        if (favorites.contains(entry.id)) favorites.remove(entry.id) else favorites.add(entry.id)
        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
        val applyFav: (StationEntry) -> StationEntry = { e -> if (e.id == entry.id) e.copy(isFavorite = favorites.contains(e.id)) else e }
        _state.update {
            it.copy(
                list = it.list.map(applyFav),
                presets = it.presets.map { slot -> slot.copy(entry = slot.entry?.let(applyFav)) },
            )
        }
    }

    override fun pickPreset(index: Int) {
        val entry = _state.value.presets.getOrNull(index)?.entry ?: return
        val s = _state.value
        when (s.source) {
            RadioSource.FM -> entry.id.removePrefix("fm:").toFloatOrNull()?.let { tuneTo(it) }
            RadioSource.AM -> entry.id.removePrefix("am:").toIntOrNull()?.let { tuneToAm(it) }
            else -> {
                val idx = s.list.indexOfFirst { it.id == entry.id }
                if (idx >= 0) selectListIndex(idx)
            }
        }
    }

    // ---- internals ----

    private fun pausePlayers() {
        streamPlayer.pause()
        localPlayer.pause()
    }

    private fun onPlayerPlayingChanged(source: NpSource, playing: Boolean) {
        _state.update { if (npSourceFor(it.source) == source) it.copy(playing = playing) else it }
        if (npSourceFor(_state.value.source) == source) {
            publishNowPlaying()
        }
    }

    private fun playListEntry(source: RadioSource, entry: StationEntry) {
        when (source) {
            RadioSource.NET -> {
                val url = netUrlById[entry.id] ?: return
                streamPlayer.play(url)
                nowPlayingRepository.registerCommands(
                    NpSource.NET,
                    object : NowPlayingCommands {
                        override fun togglePlay() = streamPlayer.togglePlay()
                        override fun next() = nextItem()
                        override fun prev() = prevItem()
                    },
                )
            }
            RadioSource.BT -> {
                val track = localTracks.getOrNull(_state.value.pick) ?: return
                localPlayer.play(track.uri.toString())
                nowPlayingRepository.registerCommands(
                    NpSource.BT,
                    object : NowPlayingCommands {
                        override fun togglePlay() = localPlayer.togglePlay()
                        override fun next() = nextItem()
                        override fun prev() = prevItem()
                    },
                )
            }
            else -> Unit
        }
        publishNowPlaying()
    }

    private fun refreshLocalTracks() {
        scope.launch(Dispatchers.IO) {
            val tracks = LocalMediaLibrary.query(context)
            localTracks = tracks
            val entries = tracks.map { StationEntry(id = "bt:${it.id}", primary = it.title, secondary = it.artist, signal = 4, isFavorite = favorites.contains("bt:${it.id}")) }
            _state.update { if (it.source == RadioSource.BT) it.copy(list = entries, presets = presetsFor(entries)) else it }
        }
    }

    private suspend fun refreshTunerStatus() {
        applyTunerResult(tuner.status())
    }

    private fun applyTunerResult(result: TunerStatus?) {
        if (result == null) {
            _state.update { it.copy(connected = false, statusNote = tunerUnavailableNote()) }
            return
        }
        val s = _state.value
        if (s.source == RadioSource.FM) {
            val mhz = result.freqKHz / 1000f
            _state.update { it.copy(connected = true, statusNote = null, signal = result.signal, freqFm = mhz) }
            applyDerivedFm(mhz)
        } else if (s.source == RadioSource.AM) {
            _state.update { it.copy(connected = true, statusNote = null, signal = result.signal, freqAm = result.freqKHz) }
            applyDerivedAm(result.freqKHz)
        }
    }

    private fun applyDerivedFm(mhz: Float) {
        val seed = fmSeeds.firstOrNull { kotlin.math.abs(it.mhz - mhz) <= 0.25f }
        val entry = seed?.let { fmEntry(it) }
        _lastStation.value = entry
        publishNowPlaying()
    }

    private fun applyDerivedAm(khz: Int) {
        val seed = amSeeds.firstOrNull { kotlin.math.abs(it.khz - khz) <= 8 }
        val entry = seed?.let { amEntry(it) }
        if (entry != null) _lastStation.value = entry
        publishNowPlaying()
    }

    private fun derivedCurrentEntry(s: RadioUiState): StationEntry? = when (s.source) {
        RadioSource.FM -> fmSeeds.firstOrNull { kotlin.math.abs(it.mhz - s.freqFm) <= 0.25f }?.let { fmEntry(it) }
            ?: StationEntry(id = "fm:${"%.1f".format(s.freqFm)}", primary = "%.1f".format(s.freqFm), secondary = "Unnamed frequency", signal = s.signal)
        RadioSource.AM -> amSeeds.firstOrNull { kotlin.math.abs(it.khz - s.freqAm) <= 8 }?.let { amEntry(it) }
            ?: StationEntry(id = "am:${s.freqAm}", primary = "${s.freqAm}", secondary = "Unnamed frequency", signal = s.signal)
        else -> s.list.getOrNull(s.pick)
    }

    private fun publishNowPlaying() {
        val s = _state.value
        val entry = derivedCurrentEntry(s)
        val np = when (s.source) {
            RadioSource.FM, RadioSource.AM -> NowPlaying(
                source = if (s.source == RadioSource.FM) NpSource.FM else NpSource.AM,
                title = entry?.primary ?: "Not tuned",
                subtitle = entry?.secondary ?: "",
                isPlaying = s.connected,
                isLive = true,
            )
            RadioSource.DAB -> NowPlaying(source = NpSource.DAB, title = "DAB+ unavailable", subtitle = "No demodulator hardware", isPlaying = false)
            RadioSource.NET -> NowPlaying(
                source = NpSource.NET,
                title = entry?.primary ?: "Internet radio",
                subtitle = entry?.secondary ?: "",
                isPlaying = streamPlayer.isPlaying,
                isLive = true,
                positionMs = streamPlayer.positionMs,
                durationMs = streamPlayer.durationMs,
            )
            RadioSource.BT -> NowPlaying(
                source = NpSource.BT,
                title = entry?.primary ?: "Local media",
                subtitle = entry?.secondary ?: "",
                isPlaying = localPlayer.isPlaying,
                positionMs = localPlayer.positionMs,
                durationMs = localPlayer.durationMs,
            )
        }
        nowPlayingRepository.publish(np)
    }

    private fun npSourceFor(source: RadioSource): NpSource = when (source) {
        RadioSource.FM -> NpSource.FM
        RadioSource.AM -> NpSource.AM
        RadioSource.DAB -> NpSource.DAB
        RadioSource.NET -> NpSource.NET
        RadioSource.BT -> NpSource.BT
    }

    private fun listFor(source: RadioSource): List<StationEntry> = when (source) {
        RadioSource.FM -> fmSeeds.map { fmEntry(it) }
        RadioSource.AM -> amSeeds.map { amEntry(it) }
        RadioSource.DAB -> dabSeeds.map { dabEntry(it) }
        RadioSource.NET -> netSeeds.map { netEntry(it) }
        RadioSource.BT -> localTracks.map { StationEntry(id = "bt:${it.id}", primary = it.title, secondary = it.artist, signal = 4, isFavorite = favorites.contains("bt:${it.id}")) }
    }

    private fun presetsFor(list: List<StationEntry>): List<PresetSlot> =
        (0 until 8).map { i -> PresetSlot(i + 1, list.getOrNull(i)) }

    private fun fmEntry(s: FmSeed) = StationEntry(id = "fm:${s.mhz}", primary = "%.1f".format(s.mhz), secondary = "${s.name} - ${s.genre}", signal = s.signal, isFavorite = favorites.contains("fm:${s.mhz}"))
    private fun amEntry(s: AmSeed) = StationEntry(id = "am:${s.khz}", primary = "${s.khz}", secondary = "${s.name} - ${s.genre}", signal = 3, isFavorite = favorites.contains("am:${s.khz}"))
    private fun dabEntry(s: DabSeed) = StationEntry(id = "dab:${s.service}", primary = s.service, secondary = "${s.ensemble} - ${s.genre}", signal = 0, isFavorite = favorites.contains("dab:${s.service}"))
    private fun netEntry(s: NetSeed) = StationEntry(id = "net:${s.name}", primary = s.name, secondary = "${s.place} - ${s.genre}", signal = 4, isFavorite = favorites.contains("net:${s.name}"))

    private fun tunerUnavailableNote() = "No FM/AM tuner (USB serial) detected"

    companion object {
        private val TUNABLE = setOf(RadioSource.FM, RadioSource.AM)
    }
}
