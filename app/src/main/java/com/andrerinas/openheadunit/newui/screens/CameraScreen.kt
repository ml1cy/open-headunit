package com.andrerinas.openheadunit.newui.screens

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.newui.camera.CameraController
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.theme.HuTheme

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context) }
    val state by controller.state.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    DisposableEffect(Unit) {
        onDispose { controller.unbind() }
    }

    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0C0E)),
        ) {
            if (state.available) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        controller.bind(lifecycleOwner, preview)
                        previewView
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.statusNote, color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                }
            }

            if (state.guidesOn) {
                GuidelinesOverlay(modifier = Modifier.fillMaxSize())
            }

            if (state.recording) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(26.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(9.dp).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFFF4D3D)))
                    Box(Modifier.width(11.dp))
                    Text("REC · Dashcam loop", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                state.statusNote,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(26.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }

        Column(modifier = Modifier.width(340.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GuidelinesToggleRow(on = state.guidesOn, onToggle = controller::toggleGuides)

            if (state.lastSavedClip != null) {
                HuInfoCard("Last saved clip", state.lastSavedClip.orEmpty())
            }

            Box(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.surface2)
                    .clickable(onClick = controller::saveClip),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(HuIcons.save, contentDescription = null, tint = colors.text, modifier = Modifier.width(22.dp).height(22.dp))
                Box(Modifier.width(12.dp))
                Text("Save last 30 s", color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GuidelinesToggleRow(on: Boolean, onToggle: () -> Unit) {
    val colors = HuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .clickable(onClick = onToggle)
            .padding(22.dp, 22.dp, 22.dp, 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Dynamic guidelines", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text("Toggle the overlay", color = colors.muted, fontSize = 14.sp)
        }
        HuSwitch(checked = on, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun HuInfoCard(label: String, value: String) {
    val colors = HuTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(22.dp),
    ) {
        EyebrowLabel(label)
        Box(Modifier.height(8.dp))
        Text(value, color = colors.text, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun GuidelinesOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun pt(xFrac: Float, yFrac: Float) = Offset(w * xFrac, h * yFrac)

        drawLine(Color(0xFF3FD07A), pt(0.33f, 1f), pt(0.43f, 0.48f), strokeWidth = 5f)
        drawLine(Color(0xFF3FD07A), pt(0.57f, 0.48f), pt(0.67f, 1f), strokeWidth = 5f)
        drawLine(Color(0xFF3FD07A), pt(0.43f, 0.48f), pt(0.57f, 0.48f), strokeWidth = 5f)
        drawLine(Color(0xFF3FD07A).copy(alpha = 0.55f), pt(0.30f, 1f), pt(0.41f, 0.43f), strokeWidth = 4f)
        drawLine(Color(0xFF3FD07A).copy(alpha = 0.55f), pt(0.70f, 1f), pt(0.59f, 0.43f), strokeWidth = 4f)
        drawLine(Color(0xFF3FD07A), pt(0.35f, 0.89f), pt(0.65f, 0.89f), strokeWidth = 8f)
        drawLine(Color(0xFFFFC53D), pt(0.38f, 0.71f), pt(0.62f, 0.71f), strokeWidth = 8f)
        drawLine(Color(0xFFFF4D3D), pt(0.41f, 0.55f), pt(0.59f, 0.55f), strokeWidth = 8f)
    }
}
