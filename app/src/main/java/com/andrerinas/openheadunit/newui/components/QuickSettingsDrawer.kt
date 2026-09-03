package com.andrerinas.openheadunit.newui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.theme.HuThemeMode
import com.andrerinas.openheadunit.newui.theme.HuTheme

data class QuickSettingsData(
    val profileName: String,
    val profileInitial: String,
    val volume: Int,
    val brightness: Int,
    val themeMode: HuThemeMode,
    val wifiOn: Boolean,
    val btOn: Boolean,
    val dnd: Boolean,
)

data class QuickSettingsActions(
    val onClose: () -> Unit,
    val onSwitchProfile: () -> Unit,
    val onVolumeStep: (Int) -> Unit,
    val onBrightnessStep: (Int) -> Unit,
    val onToggleTheme: () -> Unit,
    val onToggleWifi: () -> Unit,
    val onToggleBt: () -> Unit,
    val onToggleDnd: () -> Unit,
)

private val DrawerEasing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)

/**
 * Top-right pull-down drawer, 720px wide per the design. [open] also drives the scrim over the
 * rest of the canvas; both are meant to be composed as a full-screen overlay above the current
 * screen content (see NewUiActivity).
 */
@Composable
fun QuickSettingsDrawer(open: Boolean, data: QuickSettingsData, actions: QuickSettingsActions, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(260)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = actions.onClose),
            )
        }

        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = slideInVertically(tween(260, easing = DrawerEasing), initialOffsetY = { -(it * 1.15f).toInt() }) +
                fadeIn(tween(260, easing = DrawerEasing)),
            exit = slideOutVertically(tween(260, easing = DrawerEasing), targetOffsetY = { -(it * 1.15f).toInt() }) +
                fadeOut(tween(260, easing = DrawerEasing)),
        ) {
            Column(
                modifier = Modifier
                    .width(720.dp)
                    .background(colors.surface, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 34.dp),
            ) {
                ProfileRow(data, actions)
                Box(Modifier.height(30.dp))
                SliderRow(
                    icon = HuIcons.volume,
                    value = data.volume,
                    valueLabel = data.volume.toString(),
                    onStepDown = { actions.onVolumeStep(-1) },
                    onStepUp = { actions.onVolumeStep(1) },
                )
                Box(Modifier.height(20.dp))
                SliderRow(
                    icon = HuIcons.brightness,
                    value = ((data.brightness - 10) * 100 / 90),
                    valueLabel = "${data.brightness}%",
                    onStepDown = { actions.onBrightnessStep(-1) },
                    onStepUp = { actions.onBrightnessStep(1) },
                )
                Box(Modifier.height(28.dp))
                ToggleGrid(data, actions)
            }
        }
    }
}

@Composable
private fun ProfileRow(data: QuickSettingsData, actions: QuickSettingsActions) {
    val colors = HuTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(data.profileInitial, color = colors.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(data.profileName, color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Active profile", color = colors.muted, fontSize = 15.sp)
        }
        HuPillButton(text = "Switch", onClick = actions.onSwitchProfile, height = 48.dp)
        Box(Modifier.width(12.dp))
        Box(
            Modifier
                .size(48.dp)
                .huPressable(shape = CircleShape, normal = Color.Transparent, pressed = colors.surface2, onClick = actions.onClose)
                .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = colors.text, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SliderRow(icon: ImageVector, value: Int, valueLabel: String, onStepDown: () -> Unit, onStepUp: () -> Unit) {
    val colors = HuTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(26.dp))
        Box(Modifier.width(18.dp))
        StepSquareButton(text = "−", onClick = onStepDown)
        Box(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(colors.surface2),
        ) {
            Box(
                Modifier
                    .fillMaxWidthFraction(value / 100f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.accent),
            )
        }
        Box(Modifier.width(12.dp))
        StepSquareButton(text = "+", onClick = onStepUp)
        Box(Modifier.width(18.dp))
        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.CenterEnd) {
            Text(valueLabel, color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Modifier.fillMaxWidthFraction(fraction: Float): Modifier =
    this.then(androidx.compose.foundation.layout.fillMaxWidth(fraction.coerceIn(0f, 1f)))

@Composable
private fun StepSquareButton(text: String, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .huPressable(shape = shape, normal = colors.surface2, pressed = colors.surface3, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToggleGrid(data: QuickSettingsData, actions: QuickSettingsActions) {
    val isNight = data.themeMode == HuThemeMode.NIGHT
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        ToggleTile(
            icon = if (isNight) HuIcons.gpsPin else HuIcons.thermometer,
            label = if (isNight) "Night" else "Day",
            on = true,
            modifier = Modifier.weight(1f),
            onClick = actions.onToggleTheme,
        )
        ToggleTile(icon = HuIcons.wifi, label = "Wi-Fi", on = data.wifiOn, modifier = Modifier.weight(1f), onClick = actions.onToggleWifi)
        ToggleTile(icon = HuIcons.bluetooth, label = "Bluetooth", on = data.btOn, modifier = Modifier.weight(1f), onClick = actions.onToggleBt)
        ToggleTile(icon = HuIcons.info, label = "Do not disturb", on = data.dnd, modifier = Modifier.weight(1f), onClick = actions.onToggleDnd)
    }
}

@Composable
private fun ToggleTile(icon: ImageVector, label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val bg = if (on) colors.soft else Color.Transparent
    val fg = if (on) colors.accent else colors.muted
    Column(
        modifier = modifier
            .height(96.dp)
            .huPressable(shape = shape, normal = bg, pressed = if (on) colors.soft else colors.surface2, onClick = onClick)
            .then(if (!on) Modifier.border(1.dp, colors.line, shape) else Modifier)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(26.dp))
        Box(Modifier.height(8.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
