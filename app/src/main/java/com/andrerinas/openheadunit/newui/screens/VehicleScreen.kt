package com.andrerinas.openheadunit.newui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.components.EyebrowLabel
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.theme.HuTheme
import com.andrerinas.openheadunit.newui.theme.HuType
import com.andrerinas.openheadunit.newui.vehicle.TyrePressures
import com.andrerinas.openheadunit.newui.vehicle.VehicleSnapshot

@Composable
fun VehicleScreen(container: HuContainer, modifier: Modifier = Modifier) {
    val v by container.vehicleRepository.state.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (!v.connected) {
            HuCard(modifier = Modifier.fillMaxWidth()) {
                Text(v.sourceNote, color = HuTheme.colors.muted, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GaugeCard("Speed", v.speedKmh.toString(), "km/h", v.speedPercent, v.speedNote, HuTheme.colors.accent, Modifier.weight(1f).fillMaxHeight())
            GaugeCard("Coolant", v.coolantC.toString(), "°C", v.coolantPercent, "", HuTheme.colors.warning, Modifier.weight(1f).fillMaxHeight())
            GaugeCard("Battery", "%.1f".format(v.batteryV), "V", v.batteryPercent, v.batteryNote, HuTheme.colors.good, Modifier.weight(1f).fillMaxHeight())
            GaugeCard("Fuel", v.fuelPercent.toString(), "%", v.fuelPercent / 100f, "${v.rangeKm} km to empty", HuTheme.colors.accent, Modifier.weight(1f).fillMaxHeight())
        }
        Row(modifier = Modifier.height(300.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TyreCard(v.tyres, modifier = Modifier.weight(1f).fillMaxHeight())
            TripCard(v, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun GaugeCard(label: String, value: String, unit: String, fraction: Float, note: String, fillColor: Color, modifier: Modifier) {
    val colors = HuTheme.colors
    HuCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EyebrowLabel(label, modifier = Modifier.fillMaxWidth())
            Box(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.LastBaseline) {
                Text(value, style = HuType.gaugeValue, color = colors.text)
                Box(Modifier.width(6.dp))
                Text(unit, color = colors.muted, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.surface2),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(fillColor),
                )
            }
            Box(Modifier.height(12.dp))
            Text(note, color = colors.muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TyreCard(tyres: TyrePressures, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    HuCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EyebrowLabel("Tyre pressure")
            val rows = listOf(
                "Front left" to tyres.frontLeftBar,
                "Front right" to tyres.frontRightBar,
                "Rear left" to tyres.rearLeftBar,
                "Rear right" to tyres.rearRightBar,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        pair.forEach { (label, value) ->
                            val warn = value in 0.01f..tyres.warnBelowBar
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surface2)
                                    .padding(16.dp, 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, color = colors.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (value > 0f) "%.1f bar".format(value) else "—",
                                    color = if (warn) colors.warning else colors.text,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(v: VehicleSnapshot, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    HuCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EyebrowLabel("Trip B")
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TripFigure("%.0f".format(v.tripBKm), "km", Modifier.weight(1f))
                TripFigure("%.1f".format(v.tripBLPer100km), "L/100km", Modifier.weight(1f))
                TripFigure(v.tripBDuration.substringBefore(" "), "moving", Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface2)
                    .padding(16.dp),
            ) {
                Text(v.sourceNote, color = colors.muted, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun TripFigure(value: String, caption: String, modifier: Modifier) {
    val colors = HuTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = HuType.tripFigure, color = colors.text)
        Text(caption, color = colors.muted, fontSize = 15.sp)
    }
}
