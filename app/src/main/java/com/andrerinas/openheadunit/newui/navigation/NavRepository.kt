package com.andrerinas.openheadunit.newui.navigation

import kotlinx.coroutines.flow.StateFlow

data class NavDestination(
    val id: String,
    val name: String,
    val subtitle: String,
    val etaText: String,
    /** Passed straight into a `geo:0,0?q=` / `google.navigation:q=` hand-off intent. */
    val geoQuery: String,
)

data class ManoeuvreInfo(val distanceText: String, val instruction: String)

data class NavUiState(
    val recents: List<NavDestination> = emptyList(),
    val activeManoeuvre: ManoeuvreInfo? = null,
    /** Whether this app is granted notification access, needed to read the nav app's ongoing turn-by-turn notification. */
    val listenerGranted: Boolean = false,
)

/**
 * Real hand-off, not an in-app map: per ANDROID_IMPLEMENTATION.md, navigation is routed to
 * Google Maps / OsmAnd via `Intent(ACTION_VIEW, "google.navigation:q=...")`, and the floating
 * manoeuvre card is populated by reading the active nav app's ongoing notification through
 * [NavNotificationListenerService] (requires the user to grant notification access once, in
 * system settings — that permission can't be requested at runtime).
 */
interface NavRepository {
    val state: StateFlow<NavUiState>
    fun search(query: String)
    fun navigateTo(destination: NavDestination)
    fun requestNotificationAccess()
}
