package com.andrerinas.openheadunit.newui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.state.HuScreen
import com.andrerinas.openheadunit.newui.theme.HuTheme

private data class DockItem(val screen: HuScreen, val icon: ImageVector, val label: String)

private val dockItems = listOf(
    DockItem(HuScreen.HOME, HuIcons.home, "Home"),
    DockItem(HuScreen.AUTO, HuIcons.auto, "Auto"),
    DockItem(HuScreen.RADIO, HuIcons.radio, "Radio"),
    DockItem(HuScreen.PHONE, HuIcons.phone, "Phone"),
    DockItem(HuScreen.CAMERA, HuIcons.camera, "Camera"),
)

@Composable
fun Dock(current: HuScreen, onNavigate: (HuScreen) -> Unit, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    Box(modifier = modifier.fillMaxHeight().width(132.dp)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(131.dp)
                .background(colors.surface)
                .padding(top = 26.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(HuIcons.brandCar, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(26.dp))
            }
            Box(Modifier.height(14.dp))

            dockItems.forEach { item ->
                DockButton(item = item, active = current == item.screen, onClick = { onNavigate(item.screen) })
                Box(Modifier.height(10.dp))
            }

            Box(Modifier.weight(1f))

            val settingsActive = current == HuScreen.SETTINGS || current == HuScreen.PROFILES
            DockButton(
                item = DockItem(HuScreen.SETTINGS, HuIcons.settingsGear, "Settings"),
                active = settingsActive,
                onClick = { onNavigate(HuScreen.SETTINGS) },
            )
        }
        // 1px right border, drawn as a hairline rather than Modifier.border (which would frame all four sides).
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(colors.line2),
        )
    }
}

@Composable
private fun DockButton(item: DockItem, active: Boolean, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val fg = if (active) colors.accent else colors.muted
    val bg = if (active) colors.soft else androidx.compose.ui.graphics.Color.Transparent
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(82.dp)
            .huPressable(shape = shape, normal = bg, pressed = if (active) colors.soft else colors.surface2, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-18).dp)
                    .width(5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                    .background(colors.accent),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(27.dp))
            Box(Modifier.height(6.dp))
            Text(
                item.label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
