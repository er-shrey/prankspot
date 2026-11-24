package com.shrey.prankspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.session.Phase
import com.shrey.prankspot.ui.AppViewModel
import com.shrey.prankspot.ui.Bubblegum
import com.shrey.prankspot.ui.Grape
import com.shrey.prankspot.ui.Mint
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onCreate: () -> Unit,
    onCategory: (String) -> Unit,
    onActive: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
) {
    val session by vm.session.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val live = session.phase != Phase.IDLE

    Screen(
        title = "PrankSpot",
        onBack = null,
        actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") } },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                        .background(Brush.linearGradient(listOf(Grape, Bubblegum)))
                        .padding(20.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(12.dp).clip(CircleShape)
                                    .background(if (session.phase == Phase.RUNNING) Mint else Color.White.copy(alpha = .5f)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (session.phase) {
                                    Phase.RUNNING -> "Hotspot is live"
                                    Phase.STARTING -> "Starting…"
                                    Phase.WAITING_FOR_SYSTEM_HOTSPOT -> "Waiting for your phone's hotspot"
                                    Phase.FAILED -> "Couldn't start"
                                    Phase.IDLE -> "No prank running"
                                },
                                color = Color.White, fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (live) session.info?.ssid ?: session.config?.ssid.orEmpty() else "Create a funny network.\nLet someone connect.\nSurprise them.",
                            color = Color.White, fontSize = if (live) 26.sp else 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp,
                        )
                        if (session.phase == Phase.RUNNING) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${session.activePrank?.emoji.orEmpty()} ${session.activePrank?.name.orEmpty()} · ${session.visitors.size} pranked",
                                color = Color.White.copy(alpha = .9f),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        if (live) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onActive, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Grape)) {
                                    Text("Open controls")
                                }
                                OutlinedButton(onClick = vm::stop, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                                    Text("Stop")
                                }
                            }
                        } else {
                            Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Grape)) {
                                Icon(Icons.Outlined.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create Hotspot", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Text("Quick themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vm.categories, key = { it.id }) { c ->
                        Card(
                            modifier = Modifier.width(104.dp).clickable(enabled = !live) { onCategory(c.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(c.emoji, fontSize = 32.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(c.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onLibrary, modifier = Modifier.weight(1f)) { Text("🎭 Prank pages") }
                    OutlinedButton(onClick = onCreate, modifier = Modifier.weight(1f), enabled = !live) { Text("🎨 All themes") }
                }
            }
            if (history.isNotEmpty()) {
                item { Text("Recent pranks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(history.take(5), key = { it.id }) { h ->
                    val cat = vm.category(h.categoryId)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.ssid, fontWeight = FontWeight.SemiBold)
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(h.startedAt)) +
                                    " · ${(h.endedAt - h.startedAt) / 60_000} min",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text("${h.visitorCount} 😂")
                    }
                }
            }
        }
    }
}
