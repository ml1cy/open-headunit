package com.andrerinas.openheadunit.newui.radio

import android.content.Context
import com.andrerinas.openheadunit.utils.AppLog

data class TunerStatus(val freqKHz: Int, val signal: Int)

/**
 * FM/AM tuning protocol over [UsbCdcSerialPort]. There is no universal wire protocol for these
 * DIY boards (ANDROID_IMPLEMENTATION.md: "you implement the command protocol... yourself"), so
 * this speaks a small, documented, plain-ASCII command set:
 *
 * ```
 * → T<kHz>      tune to an absolute frequency, e.g. "T101500" for 101.5 MHz FM or "T720" AM
 * → U / D       seek up / down to the next station
 * → S           request current status
 * ← F<kHz> S<0-4>   frequency + signal strength, sent in reply to any of the above
 * ```
 *
 * Swap [tune]/[seek]/[status]'s command strings here to match a specific tuner firmware (e.g. a
 * TEF6686/FM-DX-webserver-style board) without touching [SerialRadioRepository] or the UI.
 */
class UsbSerialFmAmTuner(private val context: Context) {

    private var port: UsbCdcSerialPort? = null

    val isConnected: Boolean get() = port?.isOpen == true

    suspend fun connect(): Boolean {
        val device = UsbCdcSerialPort.findCandidateDevice(context)
        if (device == null) {
            AppLog.i("UsbSerialFmAmTuner: no USB serial device attached")
            return false
        }
        val candidate = UsbCdcSerialPort(context, device)
        val opened = try {
            candidate.open()
        } catch (e: Exception) {
            AppLog.w("UsbSerialFmAmTuner: open failed: ${e.message}")
            false
        }
        if (opened) {
            port = candidate
        } else {
            candidate.close()
        }
        return opened
    }

    fun disconnect() {
        port?.close()
        port = null
    }

    suspend fun tune(freqKHz: Int): TunerStatus? = command("T$freqKHz")

    suspend fun seek(up: Boolean): TunerStatus? = command(if (up) "U" else "D", timeoutMs = 3000)

    suspend fun status(): TunerStatus? = command("S")

    private suspend fun command(cmd: String, timeoutMs: Int = 600): TunerStatus? {
        val p = port ?: return null
        if (!p.writeLine(cmd)) {
            disconnect()
            return null
        }
        val line = p.readLine(timeoutMs) ?: return null
        val freq = Regex("F(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val signal = Regex("S(\\d)").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return TunerStatus(freq, signal.coerceIn(0, 4))
    }
}
