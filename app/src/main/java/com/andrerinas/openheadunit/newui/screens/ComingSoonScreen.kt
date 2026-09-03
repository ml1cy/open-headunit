package com.andrerinas.openheadunit.newui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.theme.HuTheme

/** Placeholder for a screen not implemented yet in this pass — swapped out file by file. */
@Composable
fun ComingSoonScreen(icon: ImageVector, title: String, note: String, modifier: Modifier = Modifier) {
    val colors = HuTheme.colors
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        HuCard {
            Column(
                modifier = Modifier.padding(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(48.dp))
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text(note, fontSize = 16.sp, color = colors.muted, textAlign = TextAlign.Center)
            }
        }
    }
}
