package com.andrerinas.openheadunit.newui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrerinas.openheadunit.app.BaseActivity
import com.andrerinas.openheadunit.newui.components.Dock
import com.andrerinas.openheadunit.newui.components.QuickSettingsActions
import com.andrerinas.openheadunit.newui.components.QuickSettingsData
import com.andrerinas.openheadunit.newui.components.QuickSettingsDrawer
import com.andrerinas.openheadunit.newui.components.StatusBar
import com.andrerinas.openheadunit.newui.components.StatusBarInfo
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.screens.AutoScreen
import com.andrerinas.openheadunit.newui.screens.AutoViewModel
import com.andrerinas.openheadunit.newui.screens.CameraScreen
import com.andrerinas.openheadunit.newui.screens.ComingSoonScreen
import com.andrerinas.openheadunit.newui.screens.HomeActions
import com.andrerinas.openheadunit.newui.screens.HomeScreen
import com.andrerinas.openheadunit.newui.screens.HomeViewModel
import com.andrerinas.openheadunit.newui.screens.NavigationScreen
import com.andrerinas.openheadunit.newui.screens.PhoneScreen
import com.andrerinas.openheadunit.newui.screens.ProfilesScreen
import com.andrerinas.openheadunit.newui.screens.VehicleScreen
import com.andrerinas.openheadunit.newui.settings.SystemToggles
import com.andrerinas.openheadunit.newui.state.HuScreen
import com.andrerinas.openheadunit.newui.state.SettingsTab
import com.andrerinas.openheadunit.newui.state.ShellViewModel
import com.andrerinas.openheadunit.newui.theme.HeadUnitTheme
import com.andrerinas.openheadunit.newui.theme.HuTheme

class NewUiActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = HuContainer.get(this)

        setContent {
            val shellViewModel: ShellViewModel = viewModel(
                factory = huViewModelFactory {
                    ShellViewModel(applicationContext, container.settings, container.suExecutor, container.profileRepository)
                },
            )
            val shellState by shellViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(shellState.brightness) {
                val attrs = window.attributes
                attrs.screenBrightness = SystemToggles.brightnessPercentToWindow(shellState.brightness)
                window.attributes = attrs
            }

            HeadUnitTheme(mode = shellState.themeMode) {
                NewUiRoot(container = container, shellViewModel = shellViewModel)
            }
        }
    }
}

@Composable
private fun NewUiRoot(container: HuContainer, shellViewModel: ShellViewModel) {
    val shellState by shellViewModel.state.collectAsStateWithLifecycle()
    val activeProfile by shellViewModel.activeProfile.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Dock(current = shellState.screen, onNavigate = shellViewModel::navigate)

            Column(modifier = Modifier.weight(1f).padding(start = 32.dp, end = 32.dp, top = 22.dp, bottom = 26.dp)) {
                StatusBar(
                    info = StatusBarInfo(
                        clock = shellState.clockText,
                        date = shellState.dateText,
                        outsideTempText = null,
                        gpsText = null,
                        wifiOn = shellState.wifiOn,
                        btOn = shellState.btOn,
                        signalLevel = 3,
                        profileName = activeProfile.name,
                        profileInitial = activeProfile.initial,
                    ),
                    onProfileClick = shellViewModel::toggleDrawer,
                )
                Box(Modifier.height(20.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ScreenHost(container = container, screen = shellState.screen, shellViewModel = shellViewModel)
                }
            }
        }

        QuickSettingsDrawer(
            open = shellState.drawerOpen,
            data = QuickSettingsData(
                profileName = activeProfile.name,
                profileInitial = activeProfile.initial,
                volume = shellState.volume,
                brightness = shellState.brightness,
                themeMode = shellState.themeMode,
                wifiOn = shellState.wifiOn,
                btOn = shellState.btOn,
                dnd = shellState.dnd,
            ),
            actions = QuickSettingsActions(
                onClose = shellViewModel::closeDrawer,
                onSwitchProfile = { shellViewModel.navigate(HuScreen.PROFILES) },
                onVolumeStep = shellViewModel::changeVolume,
                onBrightnessStep = shellViewModel::changeBrightness,
                onToggleTheme = shellViewModel::toggleTheme,
                onToggleWifi = shellViewModel::toggleWifi,
                onToggleBt = shellViewModel::toggleBt,
                onToggleDnd = shellViewModel::toggleDnd,
            ),
        )
    }
}

@Composable
private fun ScreenHost(container: HuContainer, screen: HuScreen, shellViewModel: ShellViewModel) {
    val context = LocalContext.current

    when (screen) {
        HuScreen.HOME -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = huViewModelFactory { HomeViewModel(container) { shellViewModel.state.value.volume } },
            )
            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = homeState,
                actions = HomeActions(
                    goRadio = { shellViewModel.navigate(HuScreen.RADIO) },
                    goAuto = { shellViewModel.navigate(HuScreen.AUTO) },
                    goBt = { shellViewModel.navigate(HuScreen.RADIO) },
                    goPhone = { shellViewModel.navigate(HuScreen.PHONE) },
                    goNav = { shellViewModel.navigate(HuScreen.NAV) },
                    goCam = { shellViewModel.navigate(HuScreen.CAMERA) },
                    goVehicle = { shellViewModel.navigate(HuScreen.VEHICLE) },
                    goSettingsConnectivity = { shellViewModel.openSettingsTab(SettingsTab.CONNECTIVITY) },
                    prev = { container.nowPlayingRepository.prev() },
                    next = { container.nowPlayingRepository.next() },
                    togglePlay = { container.nowPlayingRepository.togglePlay() },
                ),
            )
        }

        HuScreen.AUTO -> {
            val autoViewModel: AutoViewModel = viewModel(
                factory = huViewModelFactory { AutoViewModel(context, container) },
            )
            AutoScreen(viewModel = autoViewModel)
        }

        HuScreen.VEHICLE -> VehicleScreen(container = container)
        HuScreen.PHONE -> PhoneScreen(container = container)
        HuScreen.NAV -> NavigationScreen(container = container)
        HuScreen.PROFILES -> ProfilesScreen(container = container)
        HuScreen.CAMERA -> CameraScreen()

        HuScreen.RADIO, HuScreen.SETTINGS -> {
            ComingSoonScreen(
                icon = HuIcons.settingsGear,
                title = screen.name.lowercase().replaceFirstChar { it.uppercase() },
                note = "This screen is being built out in a later step.",
            )
        }
    }
}
