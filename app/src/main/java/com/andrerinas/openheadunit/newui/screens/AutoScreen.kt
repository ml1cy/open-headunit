package com.andrerinas.openheadunit.newui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.newui.components.HuPillButton
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.theme.HuTheme

@Composable
fun AutoScreen(viewModel: AutoViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.phase) {
        if (state.phase == AutoUiPhase.PROJECTING) viewModel.bringProjectionToFront()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state.phase) {
            AutoUiPhase.IDLE -> IdleCard(onWireless = viewModel::startWireless, onUsb = viewModel::startUsb)
            AutoUiPhase.CONNECTING -> ConnectingSpinner(detail = state.detailText)
            AutoUiPhase.PROJECTING -> ProjectingStatus(detail = state.detailText, onDisconnect = viewModel::disconnect)
        }
    }
}

@Composable
private fun BoxScopeCenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun IdleCard(onWireless: () -> Unit, onUsb: () -> Unit) {
    val colors = HuTheme.colors
    BoxScopeCenterBox {
        Column(
            modifier = Modifier
                .width(820.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .padding(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(colors.soft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(HuIcons.auto, contentDescription = null, tint = colors.accent, modifier = Modifier.size(48.dp))
            }
            Box(Modifier.height(28.dp))
            Text("Connect your phone", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Box(Modifier.height(12.dp))
            Text(
                "Plug in over USB, or start a wireless session with a phone already paired to this head unit.",
                fontSize = 19.sp,
                color = colors.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HuPillButton(text = "Start wireless", filled = true, height = 70.dp, onClick = onWireless)
                HuPillButton(text = "Use USB cable", height = 70.dp, onClick = onUsb)
            }
        }
    }
}

@Composable
private fun ConnectingSpinner(detail: String) {
    val colors = HuTheme.colors
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "angle",
    )
    BoxScopeCenterBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(110.dp)) {
                val stroke = Stroke(width = 4.dp.toPx())
                drawArc(
                    color = colors.line,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2),
                )
                drawArc(
                    color = colors.accent,
                    startAngle = angle,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    topLeft = androidx.compose.ui.geometry.Offset(stroke.width / 2, stroke.width / 2),
                )
            }
            Box(Modifier.height(34.dp))
            Text("Connecting to your phone", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Box(Modifier.height(10.dp))
            Text(detail, fontSize = 18.sp, color = colors.muted)
        }
    }
}

@Composable
private fun ProjectingStatus(detail: String, onDisconnect: () -> Unit) {
    val colors = HuTheme.colors
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.soft)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.accent))
                Box(Modifier.width(11.dp))
                Text("Projecting", color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.width(16.dp))
            Text(detail, color = colors.muted, fontSize = 15.sp)
            Box(Modifier.weight(1f))
            HuPillButton(text = "Disconnect", height = 48.dp, onClick = onDisconnect)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PROJECTION SURFACE",
                    color = colors.faint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Box(Modifier.height(12.dp))
                Text("Handing off to the phone's screen…", color = colors.muted, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
