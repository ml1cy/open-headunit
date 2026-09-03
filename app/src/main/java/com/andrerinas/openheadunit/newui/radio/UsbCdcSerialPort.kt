package com.andrerinas.openheadunit.newui.radio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Minimal USB CDC-ACM serial transport, hand-rolled in the same style as this app's existing raw
 * USB code (see `connection/projection/StandardUsbProjectionConnection.kt`) rather than pulling
 * in a third-party serial library. CDC-ACM is how most Arduino/ESP32-class boards — the DIY
 * FM/AM tuner modules ANDROID_IMPLEMENTATION.md describes (TEF6686/Si4735 behind a USB-serial
 * bridge) — enumerate over USB, so this covers that hardware without depending on an unverifiable
 * external Maven artifact.
 *
 * Only the data interface's bulk in/out endpoints are used; the (optional) control interface for
 * line coding/DTR is set best-effort and ignored if the device doesn't expose one, since most
 * clone boards work fine without it.
 */
class UsbCdcSerialPort(private val context: Context, private val device: UsbDevice) {

    private var connection: UsbDeviceConnection? = null
    private var dataInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    val isOpen: Boolean get() = connection != null

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        if (!usbManager.hasPermission(device)) {
            val granted = requestPermission(usbManager, device)
            if (!granted) return@withContext false
        }

        val iface = findDataInterface(device) ?: run {
            AppLog.w("UsbCdcSerialPort: no bulk in/out interface found on ${device.deviceName}")
            return@withContext false
        }
        val conn = usbManager.openDevice(device) ?: run {
            AppLog.w("UsbCdcSerialPort: openDevice failed for ${device.deviceName}")
            return@withContext false
        }
        if (!conn.claimInterface(iface, true)) {
            conn.close()
            return@withContext false
        }

        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
            if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep else outEp = ep
        }
        if (inEp == null || outEp == null) {
            conn.releaseInterface(iface)
            conn.close()
            return@withContext false
        }

        connection = conn
        dataInterface = iface
        endpointIn = inEp
        endpointOut = outEp

        // Best-effort CDC-ACM line coding: 115200 8N1. Ignored (device still usable) if the
        // adapter doesn't implement the ACM control requests.
        try {
            val lineCoding = byteArrayOf(
                0x00, 0xC2.toByte(), 0x01, 0x00, // 115200 baud, little-endian
                0x00, // 1 stop bit
                0x00, // no parity
                0x08, // 8 data bits
            )
            conn.controlTransfer(0x21, 0x20, 0, iface.id, lineCoding, lineCoding.size, 200)
            conn.controlTransfer(0x21, 0x22, 0x01, iface.id, null, 0, 200) // DTR+RTS on
        } catch (_: Exception) {
        }

        true
    }

    fun close() {
        try {
            dataInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (_: Exception) {
        }
        connection = null
        dataInterface = null
        endpointIn = null
        endpointOut = null
    }

    suspend fun writeLine(line: String): Boolean = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext false
        val ep = endpointOut ?: return@withContext false
        val bytes = (line + "\n").toByteArray(Charsets.US_ASCII)
        val sent = conn.bulkTransfer(ep, bytes, bytes.size, 500)
        sent >= 0
    }

    /** Reads one newline-terminated response, or null on timeout/no data within [timeoutMs]. */
    suspend fun readLine(timeoutMs: Int = 400): String? = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext null
        val ep = endpointIn ?: return@withContext null
        val buffer = ByteArray(256)
        val builder = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val remaining = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1)
            val read = conn.bulkTransfer(ep, buffer, buffer.size, remaining.coerceAtMost(300))
            if (read > 0) {
                for (i in 0 until read) {
                    val c = buffer[i].toInt().toChar()
                    if (c == '\n') return@withContext builder.toString().trim()
                    if (c != '\r') builder.append(c)
                }
            }
        }
        null
    }

    private suspend fun requestPermission(usbManager: UsbManager, device: UsbDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            val action = "com.andrerinas.openheadunit.newui.USB_TUNER_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != action) return
                    context.unregisterReceiver(this)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (cont.isActive) cont.resume(granted)
                }
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(action), flags)
            ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
            usbManager.requestPermission(device, pendingIntent)
        }

    private fun findDataInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            val hasBulkIn = (0 until iface.endpointCount).any {
                val ep = iface.getEndpoint(it)
                ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_IN
            }
            val hasBulkOut = (0 until iface.endpointCount).any {
                val ep = iface.getEndpoint(it)
                ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT
            }
            if (hasBulkIn && hasBulkOut) return iface
        }
        return null
    }

    companion object {
        /** First attached USB device that looks like a serial adapter (has a bulk in/out interface), or null. */
        fun findCandidateDevice(context: Context): UsbDevice? {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            return usbManager.deviceList.values.firstOrNull { device ->
                (0 until device.interfaceCount).any { i ->
                    val iface = device.getInterface(i)
                    (0 until iface.endpointCount).any { e -> iface.getEndpoint(e).type == UsbConstants.USB_ENDPOINT_XFER_BULK }
                }
            }
        }
    }
}
