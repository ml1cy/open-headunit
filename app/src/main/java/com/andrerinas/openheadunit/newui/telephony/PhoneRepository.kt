package com.andrerinas.openheadunit.newui.telephony

import kotlinx.coroutines.flow.StateFlow

enum class CallType { INCOMING, OUTGOING, MISSED }

data class CallLogRow(
    val id: Long,
    val name: String,
    val number: String,
    val initial: String,
    val type: CallType,
    val whenText: String,
)

data class PhoneUiState(
    val recentCalls: List<CallLogRow> = emptyList(),
    val missedCount: Int = 0,
    val hasCallLogPermission: Boolean = false,
    val hasCallPermission: Boolean = false,
    val dialed: String = "",
    /**
     * True hands-free car-kit mode (audio + call control from a *paired phone*, via HFP) needs
     * `BluetoothHeadsetClient`, a `@SystemApi` that a normal third-party APK cannot be granted on
     * stock Android — see ANDROID_IMPLEMENTATION.md "the privileged part". This repository's real
     * backing is instead this device's own telephony: [android.provider.CallLog] for recents and
     * `Intent.ACTION_CALL` to place calls, which is fully real when the head unit tablet itself
     * has a SIM or a calling app registered, and otherwise leaves the dialer honestly inert.
     */
    val hfpAvailable: Boolean = false,
)

interface PhoneRepository {
    val state: StateFlow<PhoneUiState>
    fun refresh()
    fun appendDigit(digit: Char)
    fun backspace()
    fun call(number: String = "")
    fun callRow(row: CallLogRow)
}
