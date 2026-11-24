package com.shrey.prankspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.ui.AppViewModel
import com.shrey.prankspot.ui.InfoNote
import com.shrey.prankspot.ui.SectionCard
import com.shrey.prankspot.ui.deviceKind
import com.shrey.prankspot.ui.formatDuration
import com.shrey.prankspot.ui.nickname
import kotlinx.coroutines.delay

@Composable
fun DevicesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.session.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }

    Screen("Pranked guests", onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                InfoNote(
                    "Android doesn't tell apps who joined a hotspot, so this lists devices that opened the prank page. " +
                        "Nicknames are just for fun. Nothing here is saved after the hotspot stops.",
                    "🔒",
                )
            }
            if (s.visitors.isEmpty()) item { Text("Nobody has opened the prank page yet. 👀", Modifier.padding(8.dp)) }
            items(s.visitors, key = { it.ip }) { v ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("😂", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nickname(v.ip), fontWeight = FontWeight.Bold)
                            Text("${deviceKind(v.userAgent)} · ${v.ip}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "First seen ${formatDuration(now - v.firstSeen)} ago · ${v.visits} visit${if (v.visits == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(if (now - v.lastSeen < 60_000) "🟢" else "⚪")
                    }
                }
            }
        }
    }
}
