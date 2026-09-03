package com.andrerinas.openheadunit.newui.settings

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.SUExecutor

/**
 * Real Wi-Fi / Bluetooth radio control for the quick-settings drawer and Settings ▸ Connectivity.
 *
 * Android forbids apps from silently toggling these radios from API 29 (Wi-Fi) / effectively from
 * API 33 (Bluetooth, `BluetoothAdapter.enable/disable` deprecated and unreliable) — `Settings`
 * search itself calls this out as a platform restriction, not a choice made here. Where the app
 * already has a privileged shell available (this project's existing [SUExecutor] — root or
 * Shizuku, exactly the mechanism a dedicated head unit install is expected to set up once), the
 * toggle is applied for real via `svc`. Without it, the only honest option is to hand the user to
 * the system panel rather than pretend a toggle worked when the OS silently ignored it.
 */
object SystemToggles {

    enum class ToggleResult { APPLIED, APPLIED_VIA_SHELL, OPENED_SYSTEM_PANEL, FAILED }

    fun isWifiEnabled(context: Context): Boolean {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wm?.isWifiEnabled == true
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val bm = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bm?.adapter?.isEnabled == true
    }

    fun setWifiEnabled(context: Context, suExecutor: SUExecutor, enabled: Boolean): ToggleResult {
        if (suExecutor.checkPermission()) {
            val cmd = if (enabled) "svc wifi enable" else "svc wifi disable"
            if (suExecutor.execShell(cmd) == 0) return ToggleResult.APPLIED_VIA_SHELL
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            if (wm?.setWifiEnabled(enabled) == true) return ToggleResult.APPLIED
        }
        openWifiPanel(context)
        return ToggleResult.OPENED_SYSTEM_PANEL
    }

    fun setBluetoothEnabled(context: Context, suExecutor: SUExecutor, enabled: Boolean): ToggleResult {
        if (suExecutor.checkPermission()) {
            val cmd = if (enabled) "svc bluetooth enable" else "svc bluetooth disable"
            if (suExecutor.execShell(cmd) == 0) return ToggleResult.APPLIED_VIA_SHELL
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                val bm = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bm?.adapter
                @Suppress("DEPRECATION", "MissingPermission")
                val ok = if (adapter != null) {
                    if (enabled) adapter.enable() else adapter.disable()
                } else false
                if (ok) return ToggleResult.APPLIED
            } catch (e: SecurityException) {
                AppLog.w("SystemToggles: bluetooth enable/disable denied: ${e.message}")
            }
        }
        openBluetoothPanel(context)
        return ToggleResult.OPENED_SYSTEM_PANEL
    }

    fun openWifiPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(context, intent, null)
    }

    fun openBluetoothPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_BLUETOOTH)
        } else {
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(context, intent, null)
    }

    /** Screen brightness as 10-100, mapped onto the window's 0..1 [android.view.WindowManager.LayoutParams.screenBrightness]. */
    fun brightnessPercentToWindow(percent: Int): Float = (percent.coerceIn(10, 100) / 100f)
}
