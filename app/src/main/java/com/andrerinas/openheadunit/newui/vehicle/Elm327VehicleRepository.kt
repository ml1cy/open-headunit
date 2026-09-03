package com.andrerinas.openheadunit.newui.vehicle

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Real ELM327 client over Bluetooth SPP (RFCOMM), per ANDROID_IMPLEMENTATION.md "Vehicle data".
 * ELM327's AT-command set and the OBD-II Mode 01 PIDs used here (0C RPM — unused in this UI, 0D
 * speed, 05 coolant, 2F fuel level, 42 control module voltage) are a real, stable, long-standing
 * standard, independent of vehicle make. Polls at ~2.5Hz per the doc's guidance not to exceed
 * 2-5Hz against cheap clone adapters.
 *
 * Tyre pressure has no standard PID (vehicle-specific or a separate TPMS receiver) and Trip B has
 * no odometer PID to read either — both are therefore *not* live OBD reads: tyres stay at the
 * snapshot's zeroed defaults (a manufacturer-specific driver would need to be added per vehicle),
 * and Trip B is integrated from polled speed x elapsed time while connected, which is what
 * [VehicleRepository]'s KDoc calls out.
 */
class Elm327VehicleRepository(private val context: Context) : VehicleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(VehicleSnapshot())
    override val state: StateFlow<VehicleSnapshot> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var socket: BluetoothSocket? = null

    private var tripAccumKm = 0f
    private var tripStartTimeMs = 0L
    private var lastSampleTimeMs = 0L

    override fun connect() {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch { runSession() }
    }

    override fun disconnect() {
        sessionJob?.cancel()
        sessionJob = null
        closeSocket()
        _state.update { it.copy(connected = false, sourceNote = "OBD-II adapter not connected") }
    }

    override fun resetTripB() {
        tripAccumKm = 0f
        tripStartTimeMs = System.currentTimeMillis()
        lastSampleTimeMs = tripStartTimeMs
    }

    private fun hasBluetoothPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun findAdapterDevice(): BluetoothDevice? {
        if (!hasBluetoothPermission()) return null
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return null
        if (!adapter.isEnabled) return null
        val bonded = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            AppLog.w("Elm327VehicleRepository: bondedDevices denied: ${e.message}")
            return null
        }
        val nameHints = listOf("obd", "elm327", "elm 327", "vlink", "vgate", "obdii", "obd-ii", "obdlink")
        return bonded.firstOrNull { device ->
            val name = try { device.name } catch (_: SecurityException) { null }
            name != null && nameHints.any { name.lowercase().contains(it) }
        }
    }

    private suspend fun runSession() {
        val device = findAdapterDevice()
        if (device == null) {
            _state.update {
                it.copy(
                    connected = false,
                    sourceNote = if (!hasBluetoothPermission()) "Bluetooth permission not granted" else "No OBD-II adapter paired",
                )
            }
            return
        }
        try {
            val (input, output) = openSocket(device)
            initElm(input, output)
            resetTripB()
            _state.update { it.copy(connected = true, sourceNote = "OBD-II · ${safeName(device)}") }

            while (true) {
                val speed = queryPidBytes(input, output, "010D")?.let { it.getOrNull(0) }
                val coolant = queryPidBytes(input, output, "0105")?.let { (it.getOrNull(0) ?: 0) - 40 }
                val fuel = queryPidBytes(input, output, "012F")?.let { ((it.getOrNull(0) ?: 0) * 100) / 255 }
                val voltage = queryPidBytes(input, output, "0142")?.let { bytes ->
                    val a = bytes.getOrNull(0) ?: 0
                    val b = bytes.getOrNull(1) ?: 0
                    ((a * 256) + b) / 1000f
                }

                val now = System.currentTimeMillis()
                val dtHours = (now - lastSampleTimeMs).coerceAtLeast(0) / 3_600_000f
                if (speed != null) tripAccumKm += speed * dtHours
                lastSampleTimeMs = now

                _state.update { prev ->
                    val resolvedSpeed = speed ?: prev.speedKmh
                    prev.copy(
                        connected = true,
                        speedKmh = resolvedSpeed,
                        speedPercent = (resolvedSpeed / 220f).coerceIn(0f, 1f),
                        speedNote = if (resolvedSpeed == 0) "Parked · handbrake on" else "Driving",
                        coolantC = coolant ?: prev.coolantC,
                        coolantPercent = (((coolant ?: prev.coolantC) + 40) / 170f).coerceIn(0f, 1f),
                        batteryV = voltage ?: prev.batteryV,
                        batteryPercent = (((voltage ?: prev.batteryV) - 10f) / 4f).coerceIn(0f, 1f),
                        batteryNote = if ((voltage ?: prev.batteryV) > 13.2f) "Alternator charging" else "On battery",
                        fuelPercent = fuel ?: prev.fuelPercent,
                        rangeKm = ((fuel ?: prev.fuelPercent) * 7),
                        tripBKm = tripAccumKm,
                        tripBDuration = formatDuration(now - tripStartTimeMs),
                    )
                }
                delay(400)
            }
        } catch (e: IOException) {
            AppLog.w("Elm327VehicleRepository: session ended: ${e.message}")
            _state.update { it.copy(connected = false, sourceNote = "OBD-II adapter disconnected") }
        } finally {
            closeSocket()
        }
    }

    private fun safeName(device: BluetoothDevice): String = try { device.name ?: "OBD-II adapter" } catch (_: SecurityException) { "OBD-II adapter" }

    @Throws(IOException::class)
    private suspend fun openSocket(device: BluetoothDevice): Pair<InputStream, OutputStream> {
        if (!hasBluetoothPermission()) throw IOException("Bluetooth permission not granted")
        BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
        val sock = try {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: SecurityException) {
            throw IOException("Bluetooth permission not granted", e)
        }
        socket = sock
        sock.connect()
        return sock.inputStream to sock.outputStream
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socket = null
    }

    /** Sends `ATZ`/`ATE0`/`ATL0`/`ATSP0` to reset, silence echo, drop line feeds, and auto-detect the protocol. */
    private suspend fun initElm(input: InputStream, output: OutputStream) {
        sendCommand(input, output, "ATZ")
        delay(300)
        sendCommand(input, output, "ATE0")
        sendCommand(input, output, "ATL0")
        sendCommand(input, output, "ATSP0")
    }

    /** Runs a Mode 01 PID query (e.g. "010D") and returns the data bytes after the "41 <PID>" echo, or null on no/garbled reply. */
    private suspend fun queryPidBytes(input: InputStream, output: OutputStream, pid: String): List<Int>? {
        val response = sendCommand(input, output, pid) ?: return null
        val hexTokens = response.uppercase().split(Regex("[^0-9A-F]+")).filter { it.isNotBlank() }
        // Expect "41 <pid-without-mode> <data...>"; be lenient about ELM327 clones that omit spaces.
        val modeIndex = hexTokens.indexOf("41")
        if (modeIndex < 0 || modeIndex + 2 > hexTokens.lastIndex) return null
        return hexTokens.drop(modeIndex + 2).mapNotNull { it.toIntOrNull(16) }
    }

    private suspend fun sendCommand(input: InputStream, output: OutputStream, command: String): String? {
        return withTimeoutOrNull(TimeUnit.SECONDS.toMillis(2)) {
            try {
                output.write((command + "\r").toByteArray())
                output.flush()
                readUntilPrompt(input)
            } catch (e: IOException) {
                AppLog.w("Elm327VehicleRepository: command '$command' failed: ${e.message}")
                null
            }
        }
    }

    /** ELM327 terminates every response with a bare '>' prompt character. */
    private fun readUntilPrompt(input: InputStream): String {
        val buffer = StringBuilder()
        val byteBuf = ByteArray(1)
        while (true) {
            val read = input.read(byteBuf)
            if (read <= 0) break
            val c = byteBuf[0].toInt().toChar()
            if (c == '>') break
            buffer.append(c)
        }
        return buffer.toString()
    }

    private fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "$hours:${minutes.toString().padStart(2, '0')} moving"
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
