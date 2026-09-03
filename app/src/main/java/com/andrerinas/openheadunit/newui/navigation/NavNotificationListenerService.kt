package com.andrerinas.openheadunit.newui.navigation

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads the ongoing turn-by-turn notification from whichever navigation app is running, per
 * ANDROID_IMPLEMENTATION.md — real navigation is a hand-off (Intent to Maps/OsmAnd/Waze), not an
 * in-app map, so the floating manoeuvre card on the Navigation screen has to come from somewhere
 * real: the nav app's own ongoing notification. Requires the user to grant notification access
 * once in system settings (there is no runtime-permission dialog for this).
 */
class NavNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { tryParse(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        tryParse(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName in NAV_PACKAGES) {
            _manoeuvre.value = null
        }
    }

    private fun tryParse(sbn: StatusBarNotification) {
        if (sbn.packageName !in NAV_PACKAGES) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return
        _manoeuvre.value = ManoeuvreInfo(
            distanceText = title.orEmpty(),
            instruction = text.orEmpty(),
        )
    }

    companion object {
        private val NAV_PACKAGES = setOf(
            "com.google.android.apps.maps",
            "net.osmand",
            "net.osmand.plus",
            "com.waze",
        )

        private val _manoeuvre = MutableStateFlow<ManoeuvreInfo?>(null)

        /** Latest parsed manoeuvre from the active nav app's ongoing notification, or null when none is running. */
        val manoeuvre: StateFlow<ManoeuvreInfo?> = _manoeuvre.asStateFlow()
    }
}
