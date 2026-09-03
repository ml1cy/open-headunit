package com.andrerinas.openheadunit.newui.state

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrerinas.openheadunit.newui.profiles.ActiveProfileRepository
import com.andrerinas.openheadunit.newui.settings.SystemToggles
import com.andrerinas.openheadunit.newui.theme.HuThemeMode
import com.andrerinas.openheadunit.utils.SUExecutor
import com.andrerinas.openheadunit.utils.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShellUiState(
    val screen: HuScreen = HuScreen.HOME,
    val settingsTab: SettingsTab = SettingsTab.DISPLAY,
    val drawerOpen: Boolean = false,
    val volume: Int = 62,
    val brightness: Int = 78,
    val themeMode: HuThemeMode = HuThemeMode.NIGHT,
    val wifiOn: Boolean = false,
    val btOn: Boolean = false,
    val dnd: Boolean = false,
    val clockText: String = "",
    val dateText: String = "",
)

/**
 * Shell-level state shared by every screen: current route, the quick-settings drawer, and the
 * rows in it that have a real backing today (volume via [AudioManager], brightness applied to the
 * window by NewUiActivity, night theme via the app's existing [Settings.nightMode], Wi-Fi/
 * Bluetooth via [SystemToggles]). DND is app-local (silences in-app call/notification sounds);
 * there is no public API to flip the system's Do Not Disturb without notification-policy access,
 * which would need its own separate one-time grant.
 */
class ShellViewModel(
    private val context: Context,
    private val settings: Settings,
    private val suExecutor: SUExecutor,
    private val activeProfileRepository: ActiveProfileRepository,
) : ViewModel() {

    private val audioManager: AudioManager? =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    val activeProfile = activeProfileRepository.active

    private val _state = MutableStateFlow(
        ShellUiState(
            volume = currentVolumePercent(),
            brightness = 78,
            themeMode = if (settings.nightMode == Settings.NightMode.DAY) HuThemeMode.DAY else HuThemeMode.NIGHT,
            wifiOn = SystemToggles.isWifiEnabled(context),
            btOn = SystemToggles.isBluetoothEnabled(context),
        ),
    )
    val state: StateFlow<ShellUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                updateClock()
                delay(1000L)
            }
        }
    }

    private fun updateClock() {
        val now = Date()
        _state.value = _state.value.copy(
            clockText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
            dateText = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now),
        )
    }

    private fun currentVolumePercent(): Int {
        val am = audioManager ?: return 62
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((cur * 100f) / max).toInt().coerceIn(0, 100)
    }

    fun navigate(screen: HuScreen) {
        _state.value = _state.value.copy(screen = screen, drawerOpen = false)
    }

    fun openSettingsTab(tab: SettingsTab) {
        _state.value = _state.value.copy(screen = HuScreen.SETTINGS, settingsTab = tab, drawerOpen = false)
    }

    fun toggleDrawer() {
        _state.value = _state.value.copy(drawerOpen = !_state.value.drawerOpen)
    }

    fun closeDrawer() {
        _state.value = _state.value.copy(drawerOpen = false)
    }

    fun changeVolume(deltaSteps: Int) {
        setVolume(_state.value.volume + deltaSteps * 4)
    }

    fun setVolume(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _state.value = _state.value.copy(volume = clamped)
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val index = ((clamped / 100f) * max).toInt().coerceIn(0, max)
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        } catch (_: SecurityException) {
            // Some head units restrict volume changes while a call/nav prompt is active; the UI
            // still reflects the requested value even if the platform declines to apply it.
        }
    }

    fun changeBrightness(deltaSteps: Int) {
        setBrightness(_state.value.brightness + deltaSteps * 6)
    }

    fun setBrightness(percent: Int) {
        _state.value = _state.value.copy(brightness = percent.coerceIn(10, 100))
    }

    fun toggleTheme() {
        val next = if (_state.value.themeMode == HuThemeMode.NIGHT) HuThemeMode.DAY else HuThemeMode.NIGHT
        _state.value = _state.value.copy(themeMode = next)
        settings.nightMode = if (next == HuThemeMode.DAY) Settings.NightMode.DAY else Settings.NightMode.NIGHT
    }

    fun toggleWifi() {
        val target = !_state.value.wifiOn
        viewModelScope.launch(Dispatchers.IO) {
            SystemToggles.setWifiEnabled(context, suExecutor, target)
            delay(400)
            _state.value = _state.value.copy(wifiOn = SystemToggles.isWifiEnabled(context))
        }
    }

    fun toggleBt() {
        val target = !_state.value.btOn
        viewModelScope.launch(Dispatchers.IO) {
            SystemToggles.setBluetoothEnabled(context, suExecutor, target)
            delay(400)
            _state.value = _state.value.copy(btOn = SystemToggles.isBluetoothEnabled(context))
        }
    }

    fun toggleDnd() {
        _state.value = _state.value.copy(dnd = !_state.value.dnd)
    }
}
