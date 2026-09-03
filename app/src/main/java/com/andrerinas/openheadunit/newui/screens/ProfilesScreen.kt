package com.andrerinas.openheadunit.newui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.components.HuPillButton
import com.andrerinas.openheadunit.newui.profiles.DriverProfile
import com.andrerinas.openheadunit.newui.theme.HuTheme

@Composable
fun ProfilesScreen(container: HuContainer, modifier: Modifier = Modifier) {
    val repo = container.profileRepository
    val profiles by repo.profiles.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text("Driver profiles", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text("Seat, volume, theme and radio presets, saved per driver", color = colors.muted, fontSize = 17.sp)
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            profiles.forEach { profile ->
                ProfileCard(profile, onSwitch = { repo.switchTo(profile.id) }, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(profile: DriverProfile, onSwitch: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    val borderColor = if (profile.isActive) colors.accent else colors.line2
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(30.dp)) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(if (profile.isActive) colors.accent else colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.initial,
                    color = if (profile.isActive) colors.onAccent else colors.muted,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(Modifier.height(18.dp))
            Text(profile.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Box(Modifier.height(6.dp))
            Text(profile.lastDriveText, color = colors.muted, fontSize = 15.sp)
            Box(Modifier.height(18.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                profile.chips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface2)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(chip, color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(Modifier.height(18.dp))
            if (profile.isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Active profile", color = colors.muted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                HuPillButton(text = "Switch to ${profile.name}", filled = true, height = 64.dp, onClick = onSwitch, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
