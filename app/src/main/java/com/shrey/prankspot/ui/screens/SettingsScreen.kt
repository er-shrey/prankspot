package com.shrey.prankspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.BuildConfig
import com.shrey.prankspot.data.ThemeChoice
import com.shrey.prankspot.domain.HotspotMode
import com.shrey.prankspot.hotspot.Capabilities
import com.shrey.prankspot.ui.SectionCard
import com.shrey.prankspot.ui.AppViewModel
import com.shrey.prankspot.ui.durationLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    Screen("Settings", onBack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("Default hotspot mode") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vm.directSupported) {
                        FilterChip(s.defaultMode == HotspotMode.WIFI_DIRECT, { vm.saveSettings { it.copy(defaultMode = HotspotMode.WIFI_DIRECT) } }, { Text("🎭 Funny name") })
                    }
                    FilterChip(s.defaultMode == HotspotMode.SYSTEM_HOTSPOT, { vm.saveSettings { it.copy(defaultMode = HotspotMode.SYSTEM_HOTSPOT) } }, { Text("🌐 Share internet") })
                    FilterChip(s.defaultMode == HotspotMode.LOCAL_ONLY, { vm.saveSettings { it.copy(defaultMode = HotspotMode.LOCAL_ONLY) } }, { Text("⚡ Quick") })
                }
            }
            SectionCard("Default theme") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.categories.forEach { c ->
                        FilterChip(s.defaultCategory == c.id, { vm.saveSettings { it.copy(defaultCategory = c.id, defaultPrankId = "") } }, { Text("${c.emoji} ${c.name}") })
                    }
                }
            }
            SectionCard("Default prank page") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(s.defaultPrankId.isEmpty(), { vm.saveSettings { it.copy(defaultPrankId = "") } }, { Text("Match the theme") })
                    vm.pranks.forEach { p ->
                        FilterChip(s.defaultPrankId == p.id, { vm.saveSettings { it.copy(defaultPrankId = p.id) } }, { Text("${p.emoji} ${p.name}") })
                    }
                }
            }
            SectionCard("Session") {
                Toggle("Automatic shutdown", s.autoShutdown) { v -> vm.saveSettings { it.copy(autoShutdown = v) } }
                if (s.autoShutdown) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60, 120).forEach { d ->
                            FilterChip(s.defaultDurationMinutes == d, { vm.saveSettings { it.copy(defaultDurationMinutes = d) } }, { Text(durationLabel(d)) })
                        }
                    }
                }
                Toggle("Random prank page", s.randomPrank) { v -> vm.saveSettings { it.copy(randomPrank = v) } }
                Toggle("Sound effects on prank pages", s.soundEffects) { v -> vm.saveSettings { it.copy(soundEffects = v) } }
            }
            SectionCard("Appearance") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChoice.entries.forEach { t ->
                        FilterChip(s.theme == t, { vm.saveSettings { it.copy(theme = t) } }, { Text(t.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
            }
            SectionCard("History") {
                Text("${history.size} past sessions saved on this phone (name, theme, time, guest count).", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = vm::clearHistory, enabled = history.isNotEmpty()) { Text("Clear history") }
            }
            SectionCard("Privacy") {
                Text(
                    "PrankSpot works offline and has no account, cloud or analytics. The prank page is served from your phone only " +
                        "to devices on your hotspot. It never reads guests' browsing, never asks them for passwords or payments, " +
                        "and never touches secure (HTTPS) websites. Guest addresses live in memory and vanish when the hotspot stops.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SectionCard("About") {
                Text("PrankSpot ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text(Capabilities.androidLabel, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (vm.directSupported) "Funny names: Wi-Fi Direct supported" else "Funny names: use Share internet mode",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
