package com.andrerinas.openheadunit.newui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrerinas.openheadunit.aap.AapProjectionActivity
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.newui.HuContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AutoUiPhase { IDLE, CONNECTING, PROJECTING }

data class AutoUiState(val phase: AutoUiPhase = AutoUiPhase.IDLE, val detailText: String = "")

/**
 * Real Android Auto wiring: this screen doesn't own a connection of its own, it just observes the
 * app's actual [CommManager.connectionState] and fires the same real [AapService] actions
 * `HomeFragment` already uses. On success it hands off to the existing, battle-tested
 * [AapProjectionActivity] full-screen rather than re-implementing the projection SurfaceView
 * inside Compose (that code has real surface-ownership subtleties around singleTask relaunches
 * that aren't worth risking a second implementation of).
 */
class AutoViewModel(context: Context, container: HuContainer) : ViewModel() {

    // applicationContext only: this ViewModel can outlive the Activity that created it across a
    // BaseActivity-driven recreate() (theme/locale change), so it must never hold onto that
    // Activity instance directly.
    private val context: Context = context.applicationContext

    private val commManager = container.commManager

    val state: StateFlow<AutoUiState> = commManager.connectionState.map { toUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoUiState())

    private fun toUiState(cs: CommManager.ConnectionState): AutoUiState = when (cs) {
        is CommManager.ConnectionState.Disconnected -> AutoUiState(AutoUiPhase.IDLE)
        is CommManager.ConnectionState.Connecting,
        is CommManager.ConnectionState.Connected,
        is CommManager.ConnectionState.StartingTransport,
        -> AutoUiState(AutoUiPhase.CONNECTING, "Handing the screen over to your phone")
        is CommManager.ConnectionState.HandshakeComplete,
        is CommManager.ConnectionState.TransportStarted,
        -> AutoUiState(AutoUiPhase.PROJECTING, "Wireless/USB · audio routed to head unit")
        is CommManager.ConnectionState.Error -> AutoUiState(AutoUiPhase.IDLE, cs.message)
    }

    /** Called by the composable's LaunchedEffect exactly when [state] transitions into PROJECTING. */
    fun bringProjectionToFront() {
        val intent = AapProjectionActivity.intent(context).apply {
            putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
            addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK,
            )
        }
        context.startActivity(intent)
    }

    fun startWireless() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AapService::class.java).apply { action = AapService.ACTION_START_WIRELESS_SCAN },
        )
    }

    fun startUsb() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AapService::class.java).apply { action = AapService.ACTION_CHECK_USB },
        )
    }

    fun disconnect() {
        viewModelScope.launch {
            commManager.disconnect()
        }
    }
}
