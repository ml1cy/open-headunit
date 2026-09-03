package com.andrerinas.openheadunit.newui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrerinas.openheadunit.newui.components.EyebrowLabel
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.components.HuIconCircleButton
import com.andrerinas.openheadunit.newui.components.HuPillButton
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.media.NpSource
import com.andrerinas.openheadunit.newui.theme.HuTheme
import com.andrerinas.openheadunit.newui.theme.HuType

data class HomeActions(
    val goRadio: () -> Unit,
    val goAuto: () -> Unit,
    val goBt: () -> Unit,
    val goPhone: () -> Unit,
    val goNav: () -> Unit,
    val goCam: () -> Unit,
    val goVehicle: () -> Unit,
    val goSettingsConnectivity: () -> Unit,
    val prev: () -> Unit,
    val next: () -> Unit,
    val togglePlay: () -> Unit,
)

private val stationArt = Brush.linearGradient(listOf(Color(0xFFFF7A18), Color(0xFFB3350C), Color(0xFF3A1405)))

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                NowPlayingHero(state, actions, modifier = Modifier.weight(1f))
                RecentsRow(state, actions)
            }
            TileGrid(state, actions, modifier = Modifier.width(716.dp).fillMaxHeight())
        }
        VehicleStrip(state, actions)
    }
}

@Composable
private fun NowPlayingHero(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    HuCard(modifier = modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(30.dp)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(272.dp).clip(RoundedCornerShape(20.dp)).background(stationArt),
            )
            Box(Modifier.width(30.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sourceLabel(state.nowPlaying.source).uppercase(),
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                    )
                    Box(Modifier.width(10.dp))
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(colors.faint))
                    Box(Modifier.width(10.dp))
                    Text("NOW PLAYING", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                }
                Box(Modifier.height(14.dp))
                Text(state.nowPlaying.title, style = HuType.heroTitle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.height(10.dp))
                Text(state.nowPlaying.subtitle, fontSize = 22.sp, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Box(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HuIconCircleButton(HuIcons.prevTrack, diameter = 64.dp, onClick = actions.prev)
                    Box(Modifier.width(14.dp))
                    HuIconCircleButton(
                        icon = if (state.nowPlaying.isPlaying) HuIcons.pause else HuIcons.play,
                        diameter = 78.dp,
                        background = colors.accent,
                        tint = colors.onAccent,
                        onClick = actions.togglePlay,
                    )
                    Box(Modifier.width(14.dp))
                    HuIconCircleButton(HuIcons.nextTrack, diameter = 64.dp, onClick = actions.next)
                    Box(Modifier.width(20.dp))
                    Box(Modifier.width(1.dp).height(44.dp).background(colors.line))
                    Box(Modifier.width(20.dp))
                    HuPillButton(text = "Open source", height = 56.dp, onClick = { openSource(state, actions) })
                    Box(Modifier.weight(1f))
                    Icon(HuIcons.volume, contentDescription = null, tint = colors.muted, modifier = Modifier.size(22.dp))
                    Box(Modifier.width(12.dp))
                    Text("${state.volume}", color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun sourceLabel(source: NpSource): String = when (source) {
    NpSource.FM -> "FM Radio"
    NpSource.AM -> "AM Radio"
    NpSource.DAB -> "DAB+"
    NpSource.NET -> "Internet radio"
    NpSource.BT -> "Bluetooth"
    NpSource.NONE -> "Idle"
}

private fun openSource(state: HomeUiState, actions: HomeActions) {
    when (state.nowPlaying.source) {
        NpSource.BT -> actions.goBt()
        NpSource.NONE -> actions.goRadio()
        else -> actions.goRadio()
    }
}

@Composable
private fun RecentsRow(state: HomeUiState, actions: HomeActions) {
    Row(modifier = Modifier.height(172.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        HuCard(modifier = Modifier.weight(1f).fillMaxHeight(), radius = 22.dp, onClick = actions.goRadio) {
            Column(modifier = Modifier.fillMaxSize().padding(22.dp, 22.dp, 24.dp, 22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                EyebrowLabel("Last station")
                Column {
                    val station = state.lastStation
                    Text(station?.primary ?: "No stations tuned yet", style = HuType.cardTitle, color = HuTheme.colors.text)
                    Text(station?.secondary ?: "Open Radio to get started", color = HuTheme.colors.muted, fontSize = 16.sp)
                }
            }
        }
        HuCard(modifier = Modifier.weight(1f).fillMaxHeight(), radius = 22.dp, onClick = actions.goSettingsConnectivity) {
            Column(modifier = Modifier.fillMaxSize().padding(22.dp, 22.dp, 24.dp, 22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    EyebrowLabel("Recently paired")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(HuTheme.colors.good))
                        Box(Modifier.width(7.dp))
                        Text("Connected", color = HuTheme.colors.good, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column {
                    Text("Paired device", style = HuType.cardTitle, color = HuTheme.colors.text)
                    Text("Bluetooth · Media, calls, contacts", color = HuTheme.colors.muted, fontSize = 16.sp)
                }
            }
        }
    }
}

private data class Tile(val icon: ImageVector, val title: String, val subtitle: String, val accent: Boolean, val onClick: (HomeActions) -> Unit)

@Composable
private fun TileGrid(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val tiles = listOf(
        Tile(HuIcons.auto, "Android Auto", state.autoStatusShort, true) { it.goAuto() },
        Tile(HuIcons.radio, "Radio", "FM · AM · DAB+", false) { it.goRadio() },
        Tile(HuIcons.mediaBt, "Media", "Bluetooth · USB", false) { it.goBt() },
        Tile(HuIcons.phone, "Phone", if (state.missedCallsCount > 0) "${state.missedCallsCount} missed calls" else "No missed calls", false) { it.goPhone() },
        Tile(HuIcons.navigationPin, "Navigation", state.navTopDestinationLabel, false) { it.goNav() },
        Tile(HuIcons.camera, "Camera", "Rear · dashcam", false) { it.goCam() },
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            tiles.subList(0, 3).forEach { TileCard(it, actions, Modifier.weight(1f).fillMaxHeight()) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            tiles.subList(3, 6).forEach { TileCard(it, actions, Modifier.weight(1f).fillMaxHeight()) }
        }
    }
}

@Composable
private fun TileCard(tile: Tile, actions: HomeActions, modifier: Modifier) {
    val colors = HuTheme.colors
    HuCard(modifier = modifier, radius = 22.dp, onClick = { tile.onClick(actions) }) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(17.dp)).background(if (tile.accent) colors.soft else colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, contentDescription = null, tint = if (tile.accent) colors.accent else colors.text, modifier = Modifier.size(30.dp))
            }
            Column {
                Text(tile.title, style = HuType.tileTitle, color = colors.text)
                Box(Modifier.height(4.dp))
                Text(tile.subtitle, color = colors.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun VehicleStrip(state: HomeUiState, actions: HomeActions) {
    val colors = HuTheme.colors
    val v = state.vehicle
    HuCard(modifier = Modifier.fillMaxWidth().height(132.dp), radius = 22.dp, onClick = actions.goVehicle) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            StripMetric("Speed", v.speedKmh.toString(), "km/h", Modifier.weight(1f))
            StripMetric("Battery", "%.1f".format(v.batteryV), "V", Modifier.weight(1f))
            StripMetric("Coolant", v.coolantC.toString(), "°C", Modifier.weight(1f))
            StripMetric("Fuel", v.fuelPercent.toString(), "% · ${v.rangeKm} km", Modifier.weight(1f))
            StripMetric("Trip B", "%.0f".format(v.tripBKm), "km · %.1f L".format(v.tripBLPer100km), Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("All vehicle data", color = colors.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.width(10.dp))
                Icon(HuIcons.chevronRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StripMetric(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    Column(modifier = modifier) {
        EyebrowLabel(label)
        Row(verticalAlignment = Alignment.LastBaseline, modifier = Modifier.padding(top = 8.dp)) {
            Text(value, style = HuType.vehicleStripValue, color = colors.text)
            Box(Modifier.width(8.dp))
            Text(unit, color = colors.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
