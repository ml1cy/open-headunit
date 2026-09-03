package com.andrerinas.openheadunit.newui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrerinas.openheadunit.newui.HuContainer
import com.andrerinas.openheadunit.newui.components.EyebrowLabel
import com.andrerinas.openheadunit.newui.components.HuCard
import com.andrerinas.openheadunit.newui.icons.HuIcons
import com.andrerinas.openheadunit.newui.telephony.CallLogRow
import com.andrerinas.openheadunit.newui.telephony.CallType
import com.andrerinas.openheadunit.newui.theme.HuTheme
import com.andrerinas.openheadunit.newui.theme.HuType

private data class DialKey(val digit: String, val letters: String)

private val dialKeys = listOf(
    DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF"),
    DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
    DialKey("*", ""), DialKey("0", "+"), DialKey("#", ""),
)

private val OnGoodColor = Color(0xFF06240F)

@Composable
fun PhoneScreen(container: HuContainer, modifier: Modifier = Modifier) {
    val repo = container.phoneRepository
    val state by repo.state.collectAsStateWithLifecycle()
    val colors = HuTheme.colors

    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        HuCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EyebrowLabel("Recent calls")
                    Box(Modifier.weight(1f))
                    val subtitle = when {
                        !state.hasCallLogPermission -> "Call log permission not granted"
                        state.recentCalls.isEmpty() -> "No calls yet"
                        else -> "This device's call history"
                    }
                    Text(subtitle, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 18.dp)) {
                    items(state.recentCalls) { row -> CallRow(row, onClick = { repo.callRow(row) }) }
                }
            }
        }

        HuCard(modifier = Modifier.width(560.dp).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.dialed.isEmpty()) "Enter a number" else state.dialed,
                        style = HuType.dialpadReadout,
                        color = if (state.dialed.isEmpty()) colors.muted else colors.text,
                    )
                }
                Box(Modifier.height(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    dialKeys.chunked(3).forEach { rowKeys ->
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            rowKeys.forEach { key ->
                                DialpadKey(
                                    key = key,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = { repo.appendDigit(key.digit[0]) },
                                )
                            }
                        }
                    }
                }
                Box(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(78.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface2)
                            .clickable(onClick = repo::backspace),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(HuIcons.backspace, contentDescription = "Backspace", tint = colors.text, modifier = Modifier.width(28.dp).height(28.dp))
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.good)
                            .clickable(onClick = { repo.call() }),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(HuIcons.phone, contentDescription = null, tint = OnGoodColor, modifier = Modifier.width(26.dp).height(26.dp))
                        Box(Modifier.width(14.dp))
                        Text("Call", color = OnGoodColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRow(row: CallLogRow, onClick: () -> Unit) {
    val colors = HuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(54.dp).height(54.dp).clip(CircleShape).background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(row.initial, color = colors.muted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.name, color = colors.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                callTypeLabel(row.type),
                color = if (row.type == CallType.MISSED) colors.missedCall else colors.muted,
                fontSize = 15.sp,
            )
        }
        Text(row.whenText, color = colors.faint, fontSize = 15.sp)
    }
}

private fun callTypeLabel(type: CallType): String = when (type) {
    CallType.INCOMING -> "Incoming"
    CallType.OUTGOING -> "Outgoing"
    CallType.MISSED -> "Missed"
}

@Composable
private fun DialpadKey(key: DialKey, modifier: Modifier, onClick: () -> Unit) {
    val colors = HuTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface2)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(key.digit, color = colors.text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.height(2.dp))
        Text(key.letters, color = colors.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
    }
}
