package com.andrerinas.openheadunit.newui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.theme.HuTheme
import com.andrerinas.openheadunit.newui.theme.HuType

data class StatusBarInfo(
    val clock: String,
    val date: String,
    val outsideTempText: String? = null,
    val gpsText: String? = null,
    val wifiOn: Boolean = false,
    val btOn: Boolean = false,
    val signalLevel: Int = 3,
    val profileName: String,
    val profileInitial: String,
)

@Composable
fun StatusBar(info: StatusBarInfo, onProfileClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    Row(modifier = modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(info.clock, style = HuType.clock, color = colors.text)
            Box(Modifier.width(14.dp))
            Text(info.date, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.muted, modifier = Modifier.padding(bottom = 5.dp))
        }

        if (info.outsideTempText != null) {
            Divider(colors)
            IconLabel(HuIcons.thermometer, info.outsideTempText, colors.muted)
        }
        if (info.gpsText != null) {
            Divider(colors)
            IconLabel(HuIcons.gpsPin, info.gpsText, colors.muted)
        }

        Box(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(HuIcons.bluetooth, contentDescription = "Bluetooth", tint = if (info.btOn) colors.text else colors.faint, modifier = Modifier.size(20.dp))
            Box(Modifier.width(16.dp))
            Icon(HuIcons.wifi, contentDescription = "Wi-Fi", tint = if (info.wifiOn) colors.text else colors.faint, modifier = Modifier.size(20.dp))
            Box(Modifier.width(16.dp))
            SignalBars(level = info.signalLevel, activeColor = colors.muted, inactiveColor = colors.faint)
            Box(Modifier.width(16.dp))
        }

        ProfileChip(name = info.profileName, initial = info.profileInitial, onClick = onProfileClick)
    }
}

@Composable
private fun Divider(colors: com.andrerinas.openheadunit.newui.theme.HuColors) {
    Box(Modifier.width(1.dp).height(24.dp).background(colors.line).padding(horizontal = 0.dp))
    Box(Modifier.width(22.dp))
}

@Composable
private fun IconLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Box(Modifier.width(9.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun ProfileChip(name: String, initial: String, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .height(48.dp)
            .huPressable(shape = shape, normal = colors.surface, pressed = colors.surface2, onClick = onClick)
            .then(androidx.compose.ui.Modifier)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.width(12.dp))
        Text(name, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.width(8.dp))
        Icon(HuIcons.chevronDown, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
    }
}
