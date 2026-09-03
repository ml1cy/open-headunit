package com.andrerinas.openheadunit.newui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.components.EyebrowLabel
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.components.HuIconCircleButton
import com.andrerinas.openheadunit.newui.components.HuPillButton
import com.andrerinas.openheadunit.newui.components.SignalBars
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.radio.PresetSlot
import com.andrerinas.openheadunit.newui.radio.RadioSource
import com.andrerinas.openheadunit.newui.radio.RadioUiState
import com.andrerinas.openheadunit.newui.radio.StationEntry
import com.andrerinas.openheadunit.newui.theme.HuTheme
import com.andrerinas.openheadunit.newui.theme.HuType
import kotlin.math.roundToInt

private val stationArt = Brush.linearGradient(listOf(Color(0xFFFF7A18), Color(0xFFB3350C), Color(0xFF3A1405)))

private val sourceTabs = listOf(
    RadioSource.FM to "FM",
    RadioSource.AM to "AM",
    RadioSource.DAB to "DAB+",
    RadioSource.NET to "Internet",
    RadioSource.BT to "Bluetooth",
)

@Composable
fun RadioScreen(container: HuContainer, modifier: Modifier = Modifier) {
    val repo = container.radioRepository
    val s by repo.state.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            sourceTabs.forEach { (src, label) ->
                SourceTab(label = label, active = s.source == src, onClick = { repo.selectSource(src) })
                Box(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f))
            if (s.tunable) {
                HuPillButton(text = if (s.scanning) "Scanning..." else "Scan", icon = HuIcons.scan, onClick = repo::scan)
                Box(Modifier.width(10.dp))
            }
            HuPillButton(text = "Auto-store", onClick = repo::autoStore)
        }

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                HeroCard(s)
                TuningCard(s, repo)
                PresetsCard(s, repo, modifier = Modifier.weight(1f))
            }
            StationRail(s, repo, modifier = Modifier.width(430.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun SourceTab(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(27.dp)
    val bg = if (active) colors.accent else colors.surface
    val fg = if (active) colors.onAccent else colors.text
    Box(
        modifier = Modifier
            .height(54.dp)
            .clip(shape)
            .background(bg)
            .then(if (!active) Modifier.border(1.dp, colors.line, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun sourceLabel(source: RadioSource): String = when (source) {
    RadioSource.FM -> "FM"
    RadioSource.AM -> "AM"
    RadioSource.DAB -> "DAB+"
    RadioSource.NET -> "Internet radio"
    RadioSource.BT -> "This device"
}

private fun bigReadout(s: RadioUiState): Pair<String, String> = when (s.source) {
    RadioSource.FM -> "%.1f".format(s.freqFm) to "MHz"
    RadioSource.AM -> "${s.freqAm}" to "kHz"
    RadioSource.DAB -> (s.list.getOrNull(s.pick)?.primary ?: "--") to "SERVICE"
    RadioSource.NET -> "${s.pick + 1}/${s.list.size.coerceAtLeast(1)}" to "STREAM"
    RadioSource.BT -> "${s.pick + 1}/${s.list.size.coerceAtLeast(1)}" to "QUEUE"
}

@Composable
private fun HeroCard(s: RadioUiState) {
    val colors = HuTheme.colors
    val current = s.list.getOrNull(s.pick)
    val title = if (s.tunable) (currentTunedEntry(s)?.primary ?: "No station") else (current?.primary ?: "Nothing selected")
    val subtitle = if (s.tunable) (currentTunedEntry(s)?.secondary ?: s.statusNote ?: "") else (current?.secondary ?: s.statusNote ?: "")
    val (readout, unit) = bigReadout(s)

    HuCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(26.dp, 26.dp, 30.dp, 26.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(150.dp).height(150.dp).clip(RoundedCornerShape(18.dp)).background(stationArt))
            Box(Modifier.width(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sourceLabel(s.source).uppercase(), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Box(Modifier.height(10.dp))
                Text(title, style = HuType.radioHeroTitle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.height(7.dp))
                Text(subtitle, color = colors.muted, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(readout, style = HuType.frequencyReadout, color = colors.text)
                Box(Modifier.height(8.dp))
                Text(unit, color = colors.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
            }
        }
    }
}

private fun currentTunedEntry(s: RadioUiState): StationEntry? = when (s.source) {
    RadioSource.FM -> s.list.firstOrNull { it.id == "fm:${"%.1f".format(s.freqFm)}" }
    RadioSource.AM -> s.list.firstOrNull { it.id == "am:${s.freqAm}" }
    else -> null
}

@Composable
private fun TuningCard(s: RadioUiState, repo: com.andrerinas.openheadunit.newui.radio.RadioRepository) {
    val colors = HuTheme.colors
    HuCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(30.dp, 24.dp, 30.dp, 26.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                EyebrowLabel(if (s.tunable) "Tuning" else "Now playing", modifier = Modifier.weight(1f))
                Text("Signal", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.width(8.dp))
                SignalBars(level = s.signal)
            }
            Box(Modifier.height(10.dp))
            if (s.tunable) {
                TuningRuler(s = s, onTune = { if (s.source == RadioSource.FM) repo.tuneTo(it) else repo.tuneToAm(it.roundToInt()) })
            } else {
                StreamingProgress(s)
            }
            Box(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HuIconCircleButton(HuIcons.prevItem, diameter = 66.dp, onClick = repo::prevItem)
                Box(Modifier.width(14.dp))
                if (s.tunable) {
                    StepButton("-", onClick = repo::stepDown)
                    Box(Modifier.width(14.dp))
                    StepButton("+", onClick = repo::stepUp)
                } else {
                    HuIconCircleButton(
                        icon = if (s.playing) HuIcons.pause else HuIcons.play,
                        diameter = 66.dp,
                        background = colors.accent,
                        tint = colors.onAccent,
                        onClick = repo::togglePlay,
                    )
                }
                Box(Modifier.width(14.dp))
                HuIconCircleButton(HuIcons.nextItem, diameter = 66.dp, onClick = repo::nextItem)
                Box(Modifier.weight(1f))
                val isFav = (currentTunedEntry(s) ?: s.list.getOrNull(s.pick))?.isFavorite == true
                HuPillButton(
                    text = if (isFav) "In favourites" else "Add favourite",
                    icon = if (isFav) HuIcons.starFilled else HuIcons.star,
                    height = 66.dp,
                    onClick = repo::toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier.width(66.dp).height(66.dp).clip(shape).background(colors.surface2).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StreamingProgress(s: RadioUiState) {
    val colors = HuTheme.colors
    val fraction = if (s.durationMs > 0) (s.elapsedMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.height(96.dp), verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surface2)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(4.dp)).background(colors.accent))
        }
        Box(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(s.elapsedMs), color = colors.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(if (s.durationMs > 0) formatMs(s.durationMs) else "Live", color = colors.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
}

@Composable
private fun TuningRuler(s: RadioUiState, onTune: (Float) -> Unit) {
    val colors = HuTheme.colors
    val isFm = s.source == RadioSource.FM
    val center = if (isFm) s.freqFm else s.freqAm.toFloat()
    val halfWindow = if (isFm) 3.2f else 108f
    val stepMinor = if (isFm) 0.2f else 27f
    val stepMajor = if (isFm) 1f else 100f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .pointerInput(s.source) {
                detectTapGestures { offset ->
                    val ratio = offset.x / size.width
                    val freq = center - halfWindow + ratio * 2 * halfWindow
                    onTune(freq)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minFreq = center - halfWindow
            val maxFreq = center + halfWindow
            fun xFor(freq: Float) = ((freq - minFreq) / (maxFreq - minFreq)) * w

            var tick = (minFreq / stepMinor).roundToInt() * stepMinor
            while (tick <= maxFreq) {
                val x = xFor(tick)
                val isMajor = kotlin.math.abs(tick % stepMajor) < 0.001f || kotlin.math.abs(tick % stepMajor - stepMajor) < 0.001f
                val tickHeight = if (isMajor) 40f else 20f
                drawLine(colors.text.copy(alpha = if (isMajor) 0.55f else 0.18f), androidx.compose.ui.geometry.Offset(x, h - 26f), androidx.compose.ui.geometry.Offset(x, h - 26f - tickHeight), strokeWidth = 2f)
                tick += stepMinor
            }

            val knownFreqs = if (isFm) {
                listOf(88.6f, 90.9f, 94.3f, 98.1f, 101.5f, 103.2f, 105.8f, 106.4f)
            } else {
                listOf(540f, 720f, 1044f, 1350f)
            }
            knownFreqs.filter { it in minFreq..maxFreq }.forEach { f ->
                val x = xFor(f)
                val tuned = kotlin.math.abs(f - center) < stepMinor / 2
                drawCircle(colors.accent.copy(alpha = if (tuned) 1f else 0.38f), radius = 5f, center = androidx.compose.ui.geometry.Offset(x, h - 70f))
            }

            // needle, fixed at centre; the band scrolls beneath it
            drawLine(colors.accent, androidx.compose.ui.geometry.Offset(w / 2, 6f), androidx.compose.ui.geometry.Offset(w / 2, h - 26f), strokeWidth = 3f)
        }
    }
}

@Composable
private fun PresetsCard(s: RadioUiState, repo: com.andrerinas.openheadunit.newui.radio.RadioRepository, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    val label = when (s.source) {
        RadioSource.FM -> "FM presets"
        RadioSource.AM -> "AM presets"
        RadioSource.DAB -> "DAB services"
        RadioSource.NET -> "Saved streams"
        RadioSource.BT -> "On this device"
    }
    HuCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize().padding(26.dp, 22.dp, 26.dp, 22.dp)) {
            EyebrowLabel(label)
            Box(Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                items(s.presets) { slot -> PresetCell(slot, active = slot.entry != null && slot.entry.id == (currentTunedEntry(s) ?: s.list.getOrNull(s.pick))?.id, onClick = { repo.pickPreset(s.presets.indexOf(slot)) }) }
            }
        }
    }
}

@Composable
private fun PresetCell(slot: PresetSlot, active: Boolean, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val bg = if (active) colors.soft else colors.surface2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (!active) Modifier.border(1.dp, colors.line, shape) else Modifier)
            .clickable(enabled = slot.entry != null, onClick = onClick)
            .padding(16.dp, 16.dp, 18.dp, 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("P${slot.index}", color = colors.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            if (slot.entry?.isFavorite == true) {
                Box(modifier = Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.accent))
            }
        }
        Column {
            Text(slot.entry?.primary ?: "--", style = HuType.presetPrimary, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(slot.entry?.secondary ?: "Empty", color = colors.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StationRail(s: RadioUiState, repo: com.andrerinas.openheadunit.newui.radio.RadioRepository, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    val label = when (s.source) {
        RadioSource.FM, RadioSource.AM -> "All stations"
        RadioSource.DAB -> "DAB services"
        RadioSource.NET -> "Streams"
        RadioSource.BT -> "Library"
    }
    HuCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 22.dp, bottom = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                EyebrowLabel(label)
                Text("${s.list.size}", color = colors.faint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.height(14.dp))
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                itemsIndexed(s.list) { index, entry ->
                    StationRow(
                        entry = entry,
                        active = if (s.tunable) entry.id == currentTunedEntry(s)?.id else index == s.pick,
                        onClick = { repo.selectListIndex(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StationRow(entry: StationEntry, active: Boolean, onClick: () -> Unit) {
    val colors = HuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) colors.soft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(52.dp).height(52.dp).clip(RoundedCornerShape(14.dp)).background(stationArt))
        Box(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.primary, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.secondary, color = colors.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SignalBars(level = entry.signal)
    }
}
