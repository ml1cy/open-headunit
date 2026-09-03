package com.andrerinas.openheadunit.newui.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Real backing per [PhoneRepository]'s KDoc: this device's own [CallLog.Calls] for recents and
 * `Intent.ACTION_CALL`/`ACTION_DIAL` to place calls. There is no HFP client here — see the
 * interface doc for why that needs a privileged system build this project doesn't assume.
 */
class CallLogPhoneRepository(private val context: Context) : PhoneRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PhoneUiState())
    override val state: StateFlow<PhoneUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        val hasLog = hasPermission(Manifest.permission.READ_CALL_LOG)
        val hasCall = hasPermission(Manifest.permission.CALL_PHONE)
        _state.update { it.copy(hasCallLogPermission = hasLog, hasCallPermission = hasCall) }
        if (!hasLog) return

        scope.launch {
            val rows = try {
                readCallLog()
            } catch (e: SecurityException) {
                AppLog.w("CallLogPhoneRepository: read denied: ${e.message}")
                emptyList()
            }
            _state.update { it.copy(recentCalls = rows, missedCount = rows.count { r -> r.type == CallType.MISSED }) }
        }
    }

    override fun appendDigit(digit: Char) {
        _state.update { if (it.dialed.length < 14) it.copy(dialed = it.dialed + digit) else it }
    }

    override fun backspace() {
        _state.update { if (it.dialed.isNotEmpty()) it.copy(dialed = it.dialed.dropLast(1)) else it }
    }

    override fun call(number: String) {
        val target = number.ifBlank { _state.value.dialed }
        if (target.isBlank()) return
        placeCall(target)
        _state.update { it.copy(dialed = "") }
    }

    override fun callRow(row: CallLogRow) {
        placeCall(row.number)
    }

    private fun placeCall(number: String) {
        val hasCall = hasPermission(Manifest.permission.CALL_PHONE)
        val intent = Intent(if (hasCall) Intent.ACTION_CALL else Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: SecurityException) {
            AppLog.w("CallLogPhoneRepository: call denied: ${e.message}")
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun readCallLog(): List<CallLogRow> {
        val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION)
        val rows = mutableListOf<CallLogRow>()
        context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC LIMIT 30")?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx) ?: ""
                val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }
                val type = when (cursor.getInt(typeIdx)) {
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    else -> CallType.INCOMING
                }
                rows += CallLogRow(
                    id = cursor.getLong(idIdx),
                    name = name,
                    number = number,
                    initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    type = type,
                    whenText = relativeTime(cursor.getLong(dateIdx)),
                )
            }
        }
        return rows
    }

    private fun relativeTime(epochMs: Long): String {
        val diff = System.currentTimeMillis() - epochMs
        return when {
            diff < TimeUnit.HOURS.toMillis(20) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            else -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epochMs))
        }
    }
}
