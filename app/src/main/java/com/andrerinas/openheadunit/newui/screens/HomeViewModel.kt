package com.andrerinas.openheadunit.newui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.media.NowPlaying
import com.andrerinas.openheadunit.newui.navigation.NavRepository
import com.andrerinas.openheadunit.newui.radio.StationEntry
import com.andrerinas.openheadunit.newui.vehicle.VehicleSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val nowPlaying: NowPlaying = NowPlaying(),
    val volume: Int = 62,
    val vehicle: VehicleSnapshot = VehicleSnapshot(),
    val lastStation: StationEntry? = null,
    val autoConnected: Boolean = false,
    val autoStatusShort: String = "Not connected",
    val missedCallsCount: Int = 0,
    val navTopDestinationLabel: String = "No recent destinations",
)

/**
 * Composes the shared repositories into what the Home screen needs. Per
 * ANDROID_IMPLEMENTATION.md, Home never talks to a source directly — it only reads the shared
 * [com.andrerinas.openheadunit.newui.media.NowPlayingRepository] (the "single now-playing model")
 * plus small summaries from Vehicle/Radio/Phone/Nav/CommManager.
 */
class HomeViewModel(container: HuContainer, private val volumePercent: () -> Int) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        container.nowPlayingRepository.nowPlaying,
        container.vehicleRepository.state,
        container.radioRepository.lastStation,
        container.phoneRepository.state,
    ) { np, vehicle, lastStation, phone ->
        HomeUiState(
            nowPlaying = np,
            volume = volumePercent(),
            vehicle = vehicle,
            lastStation = lastStation,
            missedCallsCount = phone.missedCount,
        )
    }.combine(container.commManager.connectionState) { partial, connState ->
        partial.copy(
            autoConnected = connState is CommManager.ConnectionState.TransportStarted,
            autoStatusShort = autoStatusShortFor(connState),
        )
    }.combine(container.navRepository.state) { partial, nav ->
        val top = nav.recents.firstOrNull()
        partial.copy(navTopDestinationLabel = if (top != null) "${top.name} · ${top.etaText}" else "No recent destinations")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun autoStatusShortFor(state: CommManager.ConnectionState): String = when (state) {
        is CommManager.ConnectionState.TransportStarted -> "Connected · projecting"
        is CommManager.ConnectionState.HandshakeComplete -> "Connecting…"
        is CommManager.ConnectionState.StartingTransport -> "Connecting…"
        is CommManager.ConnectionState.Connecting -> "Connecting…"
        is CommManager.ConnectionState.Connected -> "Connecting…"
        else -> "Not connected"
    }
}
