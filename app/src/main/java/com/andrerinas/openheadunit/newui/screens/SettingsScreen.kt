package com.andrerinas.openheadunit.newui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.BuildConfig
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.components.HuSwitch
import com.andrerinas.openheadunit.newui.radio.RadioSource
import com.andrerinas.openheadunit.newui.state.HuScreen
import com.andrerinas.openheadunit.newui.state.SettingsTab
import com.andrerinas.openheadunit.newui.state.ShellViewModel
import com.andrerinas.openheadunit.newui.theme.HuThemeMode
import com.andrerinas.openheadunit.newui.theme.HuTheme

private sealed class Row2 {
    abstract val label: String
    abstract val sub: String

    data class Toggle(override val label: String, override val sub: String, val checked: Boolean, val onToggle: () -> Unit) : Row2()
    data class Value(override val label: String, override val sub: String, val value: String) : Row2()
    data class Navigate(override val label: String, override val sub: String, val value: String, val onClick: () -> Unit) : Row2()
}

private data class Category(val tab: SettingsTab, val title: String)

private val categories = listOf(
    Category(SettingsTab.DISPLAY, "Display"),
    Category(SettingsTab.SOUND, "Sound"),
    Category(SettingsTab.CONNECTIVITY, "Connectivity"),
    Category(SettingsTab.RADIO, "Radio"),
    Category(SettingsTab.VEHICLE, "Vehicle"),
    Category(SettingsTab.PROFILES, "Profiles"),
    Category(SettingsTab.SYSTEM, "System"),
)

@Composable
fun SettingsScreen(container: HuContainer, shellViewModel: ShellViewModel, modifier: Modifier = Modifier) {
    val shellState by shellViewModel.state.collectAsStateWithLifecycle()
    val vehicle by container.vehicleRepository.state.collectAsStateWithLifecycle()
    val radio by container.radioRepository.state.collectAsStateWithLifecycle()
    val activeProfile by shellViewModel.activeProfile.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        HuCard(modifier = Modifier.width(330.dp).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                categories.forEach { cat ->
                    CategoryRow(cat.title, selected = shellState.settingsTab == cat.tab, onClick = { shellViewModel.openSettingsTab(cat.tab) })
                    Box(Modifier.height(6.dp))
                }
                Box(Modifier.weight(1f))
                Text(
                    "Open Headunit 4.0 (preview)\nBuild ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = colors.faint,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
        }

        HuCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val rows: List<Row2> = when (shellState.settingsTab) {
                SettingsTab.DISPLAY -> listOf(
                    Row2.Value("Brightness", "Auto-dims with headlights", "${shellState.brightness}%"),
                    Row2.Toggle("Night theme", "Switches the whole palette", shellState.themeMode == HuThemeMode.NIGHT, shellViewModel::toggleTheme),
                    Row2.Value("Screen timeout", "", "5 min"),
                    Row2.Value("Accent colour", "", "Amber"),
                )
                SettingsTab.SOUND -> listOf(
                    Row2.Value("Master volume", "", "${shellState.volume}"),
                    Row2.Value("Equaliser", "", "Custom: Bass +3, Mid 0, Treble +2"),
                    Row2.Value("Balance & fader", "Front-left biased for the driver", "L2 - F1"),
                    Row2.Value("Startup volume", "", "40"),
                )
                SettingsTab.CONNECTIVITY -> listOf(
                    Row2.Toggle("Wi-Fi", "", shellState.wifiOn, shellViewModel::toggleWifi),
                    Row2.Toggle("Bluetooth", "", shellState.btOn, shellViewModel::toggleBt),
                    Row2.Value("Paired devices", "", "Managed in system Bluetooth settings"),
                    Row2.Value("USB mode", "", "Auto"),
                )
                SettingsTab.RADIO -> listOf(
                    Row2.Value(
                        "FM/AM tuner",
                        "USB-serial hardware",
                        if (radio.connected && (radio.source == RadioSource.FM || radio.source == RadioSource.AM)) "Connected" else "Not detected",
                    ),
                    Row2.Value("Region", "", "Europe - 0.05 MHz"),
                    Row2.Value("Preset slots", "", "8 per band"),
                    Row2.Navigate("Open Radio", "Tune, scan and manage presets", "", { shellViewModel.navigate(HuScreen.RADIO) }),
                )
                SettingsTab.VEHICLE -> listOf(
                    Row2.Value("OBD-II adapter", vehicle.sourceNote, if (vehicle.connected) "Connected" else "Not connected"),
                    Row2.Value("Units", "", "Metric"),
                    Row2.Value(
                        "Tyre pressure alerts",
                        "Warns below ${vehicle.tyres.warnBelowBar} bar",
                        "On",
                    ),
                    Row2.Navigate("Open Vehicle", "Gauges, tyres and Trip B", "", { shellViewModel.navigate(HuScreen.VEHICLE) }),
                )
                SettingsTab.PROFILES -> listOf(
                    Row2.Value("Active profile", "", activeProfile.name),
                    Row2.Navigate("Manage profiles", "Switch or review saved drivers", "", { shellViewModel.navigate(HuScreen.PROFILES) }),
                )
                SettingsTab.SYSTEM -> listOf(
                    Row2.Value("Software", "", "${BuildConfig.VERSION_NAME} - Android ${android.os.Build.VERSION.RELEASE}"),
                    Row2.Value("Application ID", "", BuildConfig.APPLICATION_ID),
                    Row2.Value("Build type", "", if (BuildConfig.DEBUG) "Debug" else "Release"),
                )
            }
            SettingsDetail(title = categories.first { it.tab == shellState.settingsTab }.title, rows = rows)
        }
    }
}

@Composable
private fun CategoryRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val fg = if (selected) colors.accent else colors.text
    val bg = if (selected) colors.soft else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(8.dp).height(8.dp).clip(CircleShape).background(if (selected) colors.accent else colors.faint),
        )
        Box(Modifier.width(16.dp))
        Text(title, color = fg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsDetail(title: String, rows: List<Row2>) {
    val colors = HuTheme.colors
    Column(modifier = Modifier.fillMaxSize().padding(34.dp, 30.dp, 34.dp, 30.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Box(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows) { row -> SettingsRowView(row) }
        }
    }
}

@Composable
private fun SettingsRowView(row: Row2) {
    val colors = HuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { m -> if (row is Row2.Navigate) m.clickable(onClick = row.onClick) else m }
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (row.sub.isNotEmpty()) {
                Box(Modifier.height(3.dp))
                Text(row.sub, color = colors.muted, fontSize = 15.sp)
            }
        }
        when (row) {
            is Row2.Toggle -> HuSwitch(checked = row.checked, onCheckedChange = { row.onToggle() })
            is Row2.Value -> Text(row.value, color = colors.muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            is Row2.Navigate -> Text("Open ›", color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line2))
}
