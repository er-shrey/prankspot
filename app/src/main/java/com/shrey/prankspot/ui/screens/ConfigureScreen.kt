package com.shrey.prankspot.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.domain.HotspotMode
import com.shrey.prankspot.domain.PasswordRules
import com.shrey.prankspot.domain.Security
import com.shrey.prankspot.domain.SsidRules
import com.shrey.prankspot.hotspot.Capabilities
import com.shrey.prankspot.ui.AppViewModel
import com.shrey.prankspot.ui.InfoNote
import com.shrey.prankspot.ui.SectionCard
import com.shrey.prankspot.ui.durationLabel
import kotlinx.coroutines.launch

private val DURATIONS = listOf(15, 30, 60, 120, 0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigureScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onChangeCategory: () -> Unit,
    onLibrary: () -> Unit,
    onPreview: (String) -> Unit,
    onStarted: () -> Unit,
) {
    val cfg by vm.draft.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val favorites by vm.favoriteSsids.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPass by rememberSaveable { mutableStateOf(false) }
    var personalize by rememberSaveable { mutableStateOf(cfg.customTitle.isNotBlank() || cfg.customMessage.isNotBlank()) }

    val category = vm.category(cfg.categoryId)
    val prank = vm.selectedPrank()
    val local = cfg.mode == HotspotMode.LOCAL_ONLY
    // On a local-only hotspot before Android 16 QPR2 the OS picks the name and password.
    val nameEditable = !local || Capabilities.canCustomizeLocalOnly
    val ssidError = if (nameEditable) SsidRules.validate(cfg.ssid) else null
    val passError = if (nameEditable) PasswordRules.validate(cfg.password, cfg.security) else null
    val canStart = ssidError == null && passError == null

    val hotspotPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
    fun begin() {
        vm.start()
        onStarted()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val ok = !local || result[hotspotPermission] == true ||
            ContextCompat.checkSelfPermission(context, hotspotPermission) == PackageManager.PERMISSION_GRANTED
        if (ok) begin() else scope.launch {
            snackbar.showSnackbar(
                if (Build.VERSION.SDK_INT >= 33) "PrankSpot needs the Nearby devices permission to create a hotspot"
                else "PrankSpot needs the Location permission to create a hotspot on this Android version",
            )
        }
    }
    fun requestAndStart() {
        val wanted = buildList {
            if (local) add(hotspotPermission)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isEmpty()) begin() else permLauncher.launch(wanted.toTypedArray())
    }

    Screen(
        "Set up your hotspot", onBack, snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    Button(onClick = ::requestAndStart, enabled = canStart, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(
                            if (local) "🚀 Start Hotspot" else "🚀 Start: open hotspot settings",
                            fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        )
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("Hotspot mode") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = local, onClick = { vm.edit { it.copy(mode = HotspotMode.LOCAL_ONLY) } },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("No internet") }
                    SegmentedButton(
                        selected = !local, onClick = { vm.edit { it.copy(mode = HotspotMode.SYSTEM_HOTSPOT) } },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Share internet") }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    local && Capabilities.canCustomizeLocalOnly -> InfoNote(
                        "Guests join a private network with just your phone and the prank page. No mobile data is shared.", "📡",
                    )
                    local -> InfoNote(
                        "On ${Capabilities.androidLabel}, Android picks the hotspot's name and password itself " +
                            "(custom names need Android 16 QPR2). You'll see them, with a join QR code, once it starts. " +
                            "Want your funny name? Choose Share internet and type it into your phone's hotspot settings.",
                        "ℹ️",
                    )
                    else -> InfoNote(
                        "Android doesn't let apps switch on internet sharing. Tap Start: Settings opens. " +
                            "Set your phone's hotspot name and password to the ones below, turn it on, then come back. " +
                            "PrankSpot serves the prank page as soon as it spots the hotspot.",
                        "🌐",
                    )
                }
            }

            SectionCard("Theme") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(category.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onChangeCategory) { Text("Change") }
                }
            }

            if (nameEditable) {
                SectionCard("Network name") {
                    val isFav = favorites.any { it.ssid == cfg.ssid }
                    OutlinedTextField(
                        value = cfg.ssid,
                        onValueChange = { v -> vm.edit { it.copy(ssid = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = ssidError != null,
                        supportingText = {
                            val bytes = cfg.ssid.toByteArray(Charsets.UTF_8).size
                            Text(
                                ssidError ?: if (SsidRules.hasCompatibilityRisk(cfg.ssid)) "Emoji/accents may look odd on some phones · $bytes/32"
                                else "$bytes/32 bytes",
                            )
                        },
                        trailingIcon = {
                            Row {
                                if (!local) IconButton(onClick = { clipboard.setText(AnnotatedString(cfg.ssid)) }) {
                                    Icon(Icons.Default.ContentCopy, "Copy name")
                                }
                                IconButton(onClick = { vm.toggleFavoriteSsid(cfg.ssid) }, enabled = ssidError == null) {
                                    Icon(if (isFav) Icons.Default.Star else Icons.Outlined.StarOutline, "Favorite")
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { s ->
                            FilterChip(selected = s == cfg.ssid, onClick = { vm.edit { it.copy(ssid = s) } }, label = { Text(s) })
                        }
                        AssistChip(onClick = vm::regenerate, label = { Text("More names") }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                    }
                    val favs = favorites.filter { it.categoryId == cfg.categoryId }
                    if (favs.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("⭐ Favorites", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            favs.forEach { f ->
                                FilterChip(selected = f.ssid == cfg.ssid, onClick = { vm.edit { it.copy(ssid = f.ssid) } }, label = { Text(f.ssid) })
                            }
                        }
                    }
                }

                SectionCard("Security") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = cfg.security == Security.WPA2, onClick = { vm.edit { it.copy(security = Security.WPA2) } },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("Password") }
                        SegmentedButton(
                            selected = cfg.security == Security.OPEN, onClick = { vm.edit { it.copy(security = Security.OPEN) } },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("Open") }
                    }
                    if (cfg.security == Security.WPA2) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = cfg.password,
                            onValueChange = { v -> vm.edit { it.copy(password = v) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Password") },
                            isError = passError != null,
                            supportingText = { passError?.let { Text(it) } },
                            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showPass = !showPass }) {
                                        Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show password")
                                    }
                                    if (!local) IconButton(onClick = { clipboard.setText(AnnotatedString(cfg.password)) }) {
                                        Icon(Icons.Default.ContentCopy, "Copy password")
                                    }
                                    IconButton(onClick = vm::generatePassword) { Icon(Icons.Default.Casino, "Generate password") }
                                }
                            },
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Anyone nearby can join. Some phones refuse open hotspots; if it fails, switch back to Password.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            SectionCard("Prank page") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = cfg.portalEnabled, onCheckedChange = { v -> vm.edit { it.copy(portalEnabled = v) } })
                    Spacer(Modifier.width(12.dp))
                    Text("Serve a prank page to guests", Modifier.weight(1f))
                }
                if (cfg.portalEnabled) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (cfg.randomPrank) "🎲" else prank.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (cfg.randomPrank) "Surprise me" else prank.name, fontWeight = FontWeight.Bold)
                            Text(
                                if (cfg.randomPrank) "A random prank on every visit" else prank.title,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = onLibrary) { Text("Change") }
                        TextButton(onClick = { onPreview(prank.id) }, enabled = !cfg.randomPrank) { Text("Preview") }
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Random", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(6.dp))
                            Switch(checked = cfg.randomPrank, onCheckedChange = { v -> vm.edit { it.copy(randomPrank = v) } })
                        }
                    }
                    if (!cfg.randomPrank) {
                        TextButton(onClick = { personalize = !personalize }) {
                            Text(if (personalize) "Hide personal touch" else "✏️ Add a personal touch")
                        }
                        if (personalize) {
                            OutlinedTextField(
                                value = cfg.customTitle, onValueChange = { v -> vm.edit { it.copy(customTitle = v.take(80)) } },
                                label = { Text("Heading") }, placeholder = { Text(prank.title) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = cfg.customMessage, onValueChange = { v -> vm.edit { it.copy(customMessage = v.take(240)) } },
                                label = { Text("Message") }, placeholder = { Text(prank.message) },
                                modifier = Modifier.fillMaxWidth(), minLines = 2,
                            )
                        }
                    }
                }
            }

            SectionCard("Auto shutdown") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATIONS.forEach { d ->
                        FilterChip(
                            selected = cfg.durationMinutes == d,
                            onClick = { vm.edit { it.copy(durationMinutes = d) } },
                            label = { Text(durationLabel(d)) },
                        )
                    }
                }
                Text(
                    if (cfg.durationMinutes == 0) "Runs until you stop it." else "Switches off by itself after ${durationLabel(cfg.durationMinutes)}.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
