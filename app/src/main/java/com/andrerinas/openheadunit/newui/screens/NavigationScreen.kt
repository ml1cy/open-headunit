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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.components.EyebrowLabel
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.navigation.NavDestination
import com.andrerinas.openheadunit.newui.theme.HuTheme

@Composable
fun NavigationScreen(container: HuContainer, modifier: Modifier = Modifier) {
    val repo = container.navRepository
    val state by repo.state.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface2),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "MAP PLACEHOLDER",
                    color = colors.faint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Box(Modifier.height(10.dp))
                Text("A real map view replaces this panel", color = colors.muted, fontSize = 16.sp)
            }

            val manoeuvre = state.activeManoeuvre
            if (manoeuvre != null) {
                HuCard(
                    modifier = Modifier
                        .padding(26.dp)
                        .align(Alignment.TopStart),
                ) {
                    Row(modifier = Modifier.padding(20.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(HuIcons.turnArrow, contentDescription = null, tint = colors.accent, modifier = Modifier.width(46.dp).height(46.dp))
                        Box(Modifier.width(18.dp))
                        Column {
                            Text(manoeuvre.distanceText, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            Text(manoeuvre.instruction, color = colors.muted, fontSize = 17.sp)
                        }
                    }
                }
            } else if (!state.listenerGranted) {
                HuCard(modifier = Modifier.padding(26.dp).align(Alignment.TopStart), onClick = repo::requestNotificationAccess) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.Start) {
                        Text("Grant notification access", fontWeight = FontWeight.Bold, color = colors.text, fontSize = 17.sp)
                        Text("To show turns from your nav app here", color = colors.muted, fontSize = 14.sp)
                    }
                }
            }
        }

        Column(modifier = Modifier.width(470.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SearchField(onSearch = repo::search)
            HuCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 22.dp, bottom = 14.dp)) {
                    EyebrowLabel("Recent destinations", modifier = Modifier.padding(horizontal = 26.dp))
                    Box(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        items(state.recents) { dest -> DestinationRow(dest, onClick = { repo.navigateTo(dest) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(onSearch: (String) -> Unit) {
    val colors = HuTheme.colors
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(HuIcons.search, contentDescription = null, tint = colors.muted, modifier = Modifier.width(24.dp).height(24.dp))
        Box(Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text("Search a destination", color = colors.muted, fontSize = 19.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 19.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(text); text = "" }),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DestinationRow(dest: NavDestination, onClick: () -> Unit) {
    val colors = HuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(androidx.compose.foundation.clickable(onClick = onClick))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(46.dp).height(46.dp).clip(RoundedCornerShape(14.dp)).background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(HuIcons.navigationPin, contentDescription = null, tint = colors.text, modifier = Modifier.width(22.dp).height(22.dp))
        }
        Box(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dest.name, color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dest.subtitle, color = colors.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (dest.etaText.isNotEmpty()) {
            Text(dest.etaText, color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
