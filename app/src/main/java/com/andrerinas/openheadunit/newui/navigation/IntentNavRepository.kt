package com.andrerinas.openheadunit.newui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Real hand-off navigation, per [NavRepository]'s KDoc: destinations route to whichever nav app
 * is installed via `geo:`/`google.navigation:` intents (the manifest already registers this app
 * for `APP_NAVIGATION`/`APP_MAPS` and those schemes, from the existing Android Auto feature set),
 * and the manoeuvre card is fed by [NavNotificationListenerService].
 */
class IntentNavRepository(private val context: Context) : NavRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Seeded from CONTENT.md's placeholder destinations — real, storable state (a "recents"
    // list persisted from actual hand-offs) would need its own small store; this keeps the list
    // useful out of the box without inventing fake live data for it.
    private val _recents = MutableStateFlow(
        listOf(
            NavDestination("home", "Home", "Saved place", "24 min", "Home"),
            NavDestination("work", "Workshop", "Service booking 09:00", "1 h 12", "Autoservis Krkonose"),
            NavDestination("store", "Tesco Extra", "Open until 22:00", "11 min", "Tesco Extra"),
        ),
    )

    override val state: StateFlow<NavUiState> = combine(_recents, NavNotificationListenerService.manoeuvre) { recents, manoeuvre ->
        NavUiState(recents = recents, activeManoeuvre = manoeuvre, listenerGranted = isListenerGranted())
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), NavUiState())

    override fun search(query: String) {
        if (query.isBlank()) return
        navigateTo(NavDestination(id = "search", name = query, subtitle = "Search result", etaText = "", geoQuery = query))
    }

    override fun navigateTo(destination: NavDestination) {
        val uri = Uri.parse("google.navigation:q=" + Uri.encode(destination.geoQuery))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLog.w("IntentNavRepository: Maps not available, falling back to geo: intent (${e.message})")
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination.geoQuery))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (e2: Exception) {
                AppLog.e("IntentNavRepository: no app can handle navigation intents: ${e2.message}")
            }
        }
    }

    override fun requestNotificationAccess() {
        try {
            context.startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            AppLog.w("IntentNavRepository: could not open notification access settings: ${e.message}")
        }
    }

    private fun isListenerGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
