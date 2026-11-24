package com.shrey.prankspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.domain.HotspotMode
import com.shrey.prankspot.domain.Security
import com.shrey.prankspot.domain.WifiQr
import com.shrey.prankspot.hotspot.SystemHotspotManager
import com.shrey.prankspot.session.Phase
import com.shrey.prankspot.ui.AppViewModel
import com.shrey.prankspot.ui.InfoNote
import com.shrey.prankspot.ui.QrCode
import com.shrey.prankspot.ui.SectionCard
import com.shrey.prankspot.ui.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onDevices: () -> Unit,
    onLibrary: () -> Unit,
    onPreview: (String) -> Unit,
    onDone: () -> Unit,
) {
    val s by vm.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPass by rememberSaveable { mutableStateOf(false) }
    var qrTab by rememberSaveable { mutableStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }

    fun copy(label: String, text: String) {
        clipboard.setText(AnnotatedString(text))
        scope.launch { snackbar.showSnackbar("$label copied") }
    }

    Screen("Your prank hotspot", onBack, snackbar) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (s.phase) {
                Phase.IDLE -> {
                    Text("😴", fontSize = 64.sp)
                    Text(s.error ?: "No prank running right now.", textAlign = TextAlign.Center)
                    Button(onClick = { vm.dismissError(); onDone() }) { Text("Back home") }
                }
                Phase.FAILED -> {
                    Text("🙈", fontSize = 64.sp)
                    Text("Couldn't start the hotspot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(s.error.orEmpty(), textAlign = TextAlign.Center)
                    Button(onClick = { vm.dismissError(); onBack() }) { Text("Back to setup") }
                }
                Phase.STARTING -> {
                    CircularProgressIndicator()
                    Text("Asking Android to start the hotspot…")
                }
                Phase.WAITING_FOR_SYSTEM_HOTSPOT -> {
                    CircularProgressIndicator()
                    Text("Waiting for your phone's hotspot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    InfoNote("Turn on Hotspot & tethering in Settings. Use this name and password so guests see the joke.", "🌐")
                    s.config?.let { c ->
                        SectionCard {
                            CopyRow("Name", c.ssid) { copy("Name", c.ssid) }
                            if (c.security == Security.WPA2) CopyRow("Password", c.password) { copy("Password", c.password) }
                        }
                    }
                    Button(onClick = { SystemHotspotManager.openSettings(context) }) { Text("Open hotspot settings") }
                    TextButton(onClick = vm::stop) { Text("Cancel") }
                }
                Phase.RUNNING -> Running(vm, s, now, showPass, { showPass = !showPass }, qrTab, { qrTab = it }, ::copy, onDevices, onLibrary, onPreview)
            }
        }
    }
}

@Composable
private fun CopyRow(label: String, value: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "Copy $label") }
    }
}

@Composable
private fun Running(
    vm: AppViewModel,
    s: com.shrey.prankspot.session.SessionState,
    now: Long,
    showPass: Boolean,
    togglePass: () -> Unit,
    qrTab: Int,
    setQrTab: (Int) -> Unit,
    copy: (String, String) -> Unit,
    onDevices: () -> Unit,
    onLibrary: () -> Unit,
    onPreview: (String) -> Unit,
) {
    val cfg = s.config ?: return
    val info = s.info
    val system = cfg.mode == HotspotMode.SYSTEM_HOTSPOT
    // In system mode Android won't tell apps the hotspot's name; show what the user meant to set.
    val ssid = info?.ssid ?: cfg.ssid
    val password = info?.password ?: cfg.password.takeIf { cfg.security == Security.WPA2 }
    val security = info?.security ?: cfg.security

    Text("🟢 Live", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Text(ssid, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    if (s.endsAt > 0) {
        Text("Switches off in ${formatDuration(s.endsAt - now)}", style = MaterialTheme.typography.bodyMedium)
    } else {
        Text("Running for ${formatDuration(now - s.startedAt)}", style = MaterialTheme.typography.bodyMedium)
    }
    if (info != null && !info.customApplied && !system && cfg.ssid.isNotBlank()) {
        InfoNote("Android chose this name and password for the hotspot, so your funny name isn't shown on this phone.", "ℹ️")
    }
    if (system) InfoNote("Using your phone's own hotspot. If you set a different name or password there, guests will see that one.", "🌐")

    SectionCard("Wi-Fi details") {
        CopyRow("Network name", ssid) { copy("Name", ssid) }
        if (security == Security.WPA2 && password != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Password", style = MaterialTheme.typography.labelMedium)
                    Text(if (showPass) password else "•".repeat(password.length.coerceAtMost(12)), fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                }
                IconButton(onClick = togglePass) { Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show password") }
                IconButton(onClick = { copy("Password", password) }) { Icon(Icons.Default.ContentCopy, "Copy password") }
            }
        } else {
            Text("Open network: no password", style = MaterialTheme.typography.bodySmall)
        }
    }

    SectionCard("Share") {
        val portal = s.portalUrl
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = qrTab == 0, onClick = { setQrTab(0) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("1. Join Wi-Fi") }
            SegmentedButton(selected = qrTab == 1, onClick = { setQrTab(1) }, shape = SegmentedButtonDefaults.itemShape(1, 2), enabled = portal != null) { Text("2. Open prank") }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (qrTab == 0) {
                QrCode(WifiQr.joinPayload(ssid, password, security))
                Spacer(Modifier.height(6.dp))
                Text("Guests scan this with their camera to join.", style = MaterialTheme.typography.bodySmall)
            } else if (portal != null) {
                QrCode(portal)
                Spacer(Modifier.height(6.dp))
                Text("Once joined, scan this (or open the address below).", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            !cfg.portalEnabled -> Text("Prank page is off for this session.", style = MaterialTheme.typography.bodySmall)
            portal != null -> CopyRow("Prank page address", portal) { copy("Address", portal) }
            s.portalError != null -> {
                Text(s.portalError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = vm::retryPortal) { Text("Retry") }
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Starting the prank page…", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (cfg.portalEnabled) {
        SectionCard("Prank page") {
            val p = s.activePrank
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (cfg.randomPrank) "🎲" else p?.emoji.orEmpty(), fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Text(if (cfg.randomPrank) "Random on every visit" else p?.name.orEmpty(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Row {
                TextButton(onClick = onLibrary) { Text("Switch page") }
                if (p != null && !cfg.randomPrank) TextButton(onClick = { onPreview(p.id) }) { Text("Preview") }
            }
            Text("Guests see a new page next time they refresh.", style = MaterialTheme.typography.bodySmall)
        }
        SectionCard("Guests") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${s.visitors.size}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text(if (s.visitors.size == 1) "device opened the prank page" else "devices opened the prank page", Modifier.weight(1f))
                TextButton(onClick = onDevices) { Text("See all") }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (s.endsAt > 0) OutlinedButton(onClick = { vm.extend(15) }) { Text("+15 min") }
        Button(onClick = vm::stop, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("Stop Hotspot", fontWeight = FontWeight.Bold)
        }
    }
}
