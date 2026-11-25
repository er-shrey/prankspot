package com.shrey.prankspot.session

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.shrey.prankspot.data.CatalogRepository
import com.shrey.prankspot.data.PrankDao
import com.shrey.prankspot.data.SessionRecord
import com.shrey.prankspot.data.SettingsRepository
import com.shrey.prankspot.domain.HotspotConfig
import com.shrey.prankspot.domain.HotspotInfo
import com.shrey.prankspot.domain.HotspotMode
import com.shrey.prankspot.domain.HotspotResult
import com.shrey.prankspot.domain.PrankTemplate
import com.shrey.prankspot.domain.Visitor
import com.shrey.prankspot.hotspot.HotspotInterfaces
import com.shrey.prankspot.hotspot.HotspotStatus
import com.shrey.prankspot.hotspot.LocalOnlyHotspotManager
import com.shrey.prankspot.hotspot.SystemHotspotManager
import com.shrey.prankspot.hotspot.WifiDirectHotspotManager
import com.shrey.prankspot.portal.PageRenderer
import com.shrey.prankspot.portal.PortalServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Phase { IDLE, STARTING, WAITING_FOR_SYSTEM_HOTSPOT, RUNNING, FAILED }

data class SessionState(
    val phase: Phase = Phase.IDLE,
    val config: HotspotConfig? = null,
    val info: HotspotInfo? = null,
    val portalUrl: String? = null,
    val portalError: String? = null,
    val startedAt: Long = 0,
    /** 0 = no auto shutdown. */
    val endsAt: Long = 0,
    val error: String? = null,
    val activePrank: PrankTemplate? = null,
    val visitors: List<Visitor> = emptyList(),
)

/**
 * Owns one prank session end to end: the hotspot, the web server, the shutdown timer and the
 * history row. Lives as long as the process; [HotspotService] keeps the process in the foreground.
 */
class SessionController(
    private val context: Context,
    private val catalog: CatalogRepository,
    private val dao: PrankDao,
    private val settings: SettingsRepository,
    val localOnly: LocalOnlyHotspotManager,
    val system: SystemHotspotManager,
    val direct: WifiDirectHotspotManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + kotlinx.coroutines.SupervisorJob())
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val server = PortalServer { currentPage() }
    private var watchJob: Job? = null
    private var sessionJob: Job? = null

    init {
        scope.launch { server.visitors.collect { v -> _state.update { it.copy(visitors = v.values.sortedBy { x -> x.firstSeen }) } } }
    }

    fun start(config: HotspotConfig) {
        if (_state.value.phase in setOf(Phase.STARTING, Phase.WAITING_FOR_SYSTEM_HOTSPOT, Phase.RUNNING)) return
        val prank = pickPrank(config)
        _state.value = SessionState(
            phase = if (config.mode == HotspotMode.SYSTEM_HOTSPOT) Phase.WAITING_FOR_SYSTEM_HOTSPOT else Phase.STARTING,
            config = config,
            activePrank = prank,
        )
        ContextCompat.startForegroundService(context, HotspotService.startIntent(context))
        sessionJob = scope.launch {
            val manager = when (config.mode) {
                HotspotMode.WIFI_DIRECT -> direct
                HotspotMode.SYSTEM_HOTSPOT -> system
                HotspotMode.LOCAL_ONLY -> localOnly
            }
            when (val r = manager.start(config)) {
                is HotspotResult.Failed -> fail(r.reason)
                is HotspotResult.Started -> onHotspotUp(config, r.info)
            }
        }
    }

    private suspend fun onHotspotUp(config: HotspotConfig, info: HotspotInfo) {
        val now = System.currentTimeMillis()
        val ends = if (config.durationMinutes > 0) now + config.durationMinutes * 60_000L else 0L
        _state.update { it.copy(phase = Phase.RUNNING, info = info, startedAt = now, endsAt = ends, error = null) }
        HotspotService.refresh(context)
        if (config.portalEnabled) startPortal(config.mode == HotspotMode.WIFI_DIRECT)
        watch(config)
    }

    /** The hotspot interface can take a moment to get its address after the OS reports it up. */
    private suspend fun startPortal(wifiDirect: Boolean) {
        val upstream = SystemHotspotManager.upstreamInterfaces(context)
        var found: HotspotInterfaces.Candidate? = null
        for (attempt in 0 until 20) {
            found = withContext(Dispatchers.IO) { HotspotInterfaces.find(upstream, wifiDirect) }
            if (found != null) break
            delay(500)
        }
        if (found == null) {
            Log.w(TAG, "No hotspot interface. upstream=$upstream all=" + runCatching {
                java.net.NetworkInterface.getNetworkInterfaces().toList().map { i -> i.name + i.inetAddresses.toList() + i.isUp }
            }.getOrElse { it.toString() })
            _state.update { it.copy(portalError = "Couldn't find the hotspot's network address. Tap Retry.") }
            return
        }
        val address = found.address
        val result = withContext(Dispatchers.IO) { runCatching { server.start(address) } }
        result.onSuccess { url -> _state.update { it.copy(portalUrl = url, portalError = null) } }
            .onFailure { e ->
                Log.e(TAG, "Portal failed", e)
                _state.update { it.copy(portalError = "Prank page server failed: ${e.message}") }
            }
    }

    fun retryPortal() {
        if (_state.value.phase != Phase.RUNNING) return
        scope.launch {
            withContext(Dispatchers.IO) { server.stop() }
            _state.update { it.copy(portalUrl = null, portalError = null) }
            startPortal(_state.value.config?.mode == HotspotMode.WIFI_DIRECT)
        }
    }

    /** Ends the session when the timer runs out or the OS turns the hotspot off under us. */
    private fun watch(config: HotspotConfig) {
        watchJob?.cancel()
        watchJob = scope.launch {
            while (isActive) {
                val s = _state.value
                if (s.endsAt in 1..System.currentTimeMillis()) {
                    stop(reason = "Time's up: the hotspot switched itself off")
                    return@launch
                }
                val up = when (config.mode) {
                    HotspotMode.LOCAL_ONLY -> localOnly.observeStatus().value is HotspotStatus.Running
                    HotspotMode.SYSTEM_HOTSPOT -> withContext(Dispatchers.IO) { system.isHotspotUp() }
                    HotspotMode.WIFI_DIRECT -> direct.group()?.isGroupOwner == true
                }
                if (!up) {
                    stop(reason = "The hotspot was turned off")
                    return@launch
                }
                delay(if (config.mode == HotspotMode.LOCAL_ONLY) 1_000 else 3_000)
            }
        }
    }

    fun extend(minutes: Int) {
        _state.update { s ->
            if (s.phase != Phase.RUNNING) s
            else s.copy(endsAt = maxOf(s.endsAt, System.currentTimeMillis()) + minutes * 60_000L)
        }
    }

    fun setPrank(prank: PrankTemplate) {
        _state.update { it.copy(activePrank = prank, config = it.config?.copy(prankId = prank.id, randomPrank = false)) }
    }

    fun stop(reason: String? = null) {
        val s = _state.value
        watchJob?.cancel()
        sessionJob?.cancel()
        scope.launch {
            withContext(Dispatchers.IO) { server.stop() }
            when (s.config?.mode) {
                HotspotMode.SYSTEM_HOTSPOT -> system.markStopped()
                HotspotMode.WIFI_DIRECT -> direct.stop()
                else -> localOnly.stop()
            }
            if (s.phase == Phase.RUNNING && s.config != null) {
                withContext(Dispatchers.IO) {
                    dao.addSession(
                        SessionRecord(
                            mode = s.config.mode.name,
                            ssid = s.info?.ssid ?: s.config.ssid,
                            categoryId = s.config.categoryId,
                            prankId = s.activePrank?.id.orEmpty(),
                            startedAt = s.startedAt,
                            endedAt = System.currentTimeMillis(),
                            visitorCount = s.visitors.size,
                        ),
                    )
                }
            }
            _state.value = SessionState(error = reason?.takeIf { s.phase == Phase.RUNNING })
            HotspotService.stop(context)
        }
    }

    fun dismissError() = _state.update { if (it.phase == Phase.FAILED) SessionState() else it.copy(error = null) }

    private fun fail(reason: String) {
        _state.update { it.copy(phase = Phase.FAILED, error = reason) }
        HotspotService.stop(context)
    }

    private fun pickPrank(config: HotspotConfig): PrankTemplate =
        if (config.randomPrank) catalog.pranks.random()
        else catalog.prank(config.prankId) ?: catalog.defaultPrankFor(config.categoryId)

    private fun currentPage(): String {
        val s = _state.value
        val cfg = s.config ?: return PageRenderer.fallback()
        val prank = if (cfg.randomPrank) catalog.pranks.random() else s.activePrank ?: return PageRenderer.fallback()
        val sound = soundOn
        return PageRenderer.render(prank, cfg.customTitle, cfg.customMessage, sound)
    }

    @Volatile private var soundOn = true

    init {
        scope.launch { settings.settings.collect { soundOn = it.soundEffects } }
    }

    private companion object {
        const val TAG = "SessionController"
    }
}
