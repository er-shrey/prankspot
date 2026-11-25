package com.shrey.prankspot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shrey.prankspot.PrankSpotApp
import com.shrey.prankspot.data.AppSettings
import com.shrey.prankspot.data.FavoritePrank
import com.shrey.prankspot.data.FavoriteSsid
import com.shrey.prankspot.domain.Category
import com.shrey.prankspot.domain.DirectName
import com.shrey.prankspot.domain.HotspotConfig
import com.shrey.prankspot.domain.HotspotMode
import com.shrey.prankspot.domain.PasswordGenerator
import com.shrey.prankspot.domain.PrankTemplate
import com.shrey.prankspot.domain.Security
import com.shrey.prankspot.domain.SsidGenerator
import com.shrey.prankspot.hotspot.SystemHotspotManager
import com.shrey.prankspot.hotspot.WifiDirectHotspotManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as PrankSpotApp).container
    private val dao = c.database.dao()
    private val ssidGen = SsidGenerator()
    private val passGen = PasswordGenerator()

    /** Wi-Fi Direct needs Android 10+ and the hardware feature; otherwise fall back. */
    val directSupported = WifiDirectHotspotManager.isSupported(app)

    val categories: List<Category> = c.catalog.categories
    val pranks: List<PrankTemplate> = c.catalog.pranks
    val session = c.session.state

    val settings: StateFlow<AppSettings> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val favoriteSsids: StateFlow<List<FavoriteSsid>> =
        dao.favoriteSsids().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favoritePranks: StateFlow<Set<String>> =
        dao.favoritePranks().map { l -> l.map(FavoritePrank::prankId).toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val history = dao.sessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _draft = MutableStateFlow(HotspotConfig())
    val draft: StateFlow<HotspotConfig> = _draft.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    init {
        viewModelScope.launch {
            val s = c.settings.settings.first()
            _draft.value = HotspotConfig(
                mode = if (s.defaultMode == HotspotMode.WIFI_DIRECT && !directSupported) HotspotMode.SYSTEM_HOTSPOT else s.defaultMode,
                durationMinutes = if (s.autoShutdown) s.defaultDurationMinutes else 0,
                randomPrank = s.randomPrank,
                password = passGen.generate(),
            )
            selectCategory(s.defaultCategory, prankId = s.defaultPrankId)
        }
    }

    fun category(id: String) = c.catalog.category(id)
    fun prank(id: String) = c.catalog.prank(id)

    fun selectedPrank(): PrankTemplate =
        c.catalog.prank(_draft.value.prankId) ?: c.catalog.defaultPrankFor(_draft.value.categoryId)

    fun selectCategory(id: String, prankId: String = "") {
        val cat = c.catalog.category(id)
        val picks = ssidGen.suggestions(cat, fits = fitsFor(_draft.value.mode))
        _suggestions.value = picks
        _draft.update {
            it.copy(
                categoryId = cat.id,
                ssid = picks.first(),
                prankId = c.catalog.prank(prankId)?.id ?: cat.pranks.first().id,
            )
        }
    }

    fun regenerate() {
        val picks = ssidGen.suggestions(c.catalog.category(_draft.value.categoryId), fits = fitsFor(_draft.value.mode))
        _suggestions.value = picks
        _draft.update { it.copy(ssid = picks.first()) }
    }

    /** Wi-Fi Direct names lose 10 bytes to the "DIRECT-xy " prefix, so suggest only names that fit. */
    private fun fitsFor(mode: HotspotMode): (String) -> Boolean =
        if (mode == HotspotMode.WIFI_DIRECT) DirectName::fits else { _ -> true }

    fun setMode(mode: HotspotMode) {
        _draft.update {
            it.copy(
                mode = mode,
                // Wi-Fi Direct and the local-only hotspot always need a password.
                security = if (mode == HotspotMode.SYSTEM_HOTSPOT) it.security else Security.WPA2,
                password = it.password.ifBlank { passGen.generate() },
            )
        }
        val ssid = _draft.value.ssid
        if (!fitsFor(mode)(ssid)) regenerate()
        else _suggestions.value = _suggestions.value.filter(fitsFor(mode)).ifEmpty {
            ssidGen.suggestions(c.catalog.category(_draft.value.categoryId), fits = fitsFor(mode))
        }
    }

    fun edit(transform: (HotspotConfig) -> HotspotConfig) = _draft.update(transform)

    fun generatePassword() = _draft.update { it.copy(password = passGen.generate(), security = Security.WPA2) }

    fun toggleFavoriteSsid(ssid: String) = viewModelScope.launch {
        if (favoriteSsids.value.any { it.ssid == ssid }) dao.removeFavoriteSsid(ssid)
        else dao.addFavoriteSsid(FavoriteSsid(ssid, _draft.value.categoryId, System.currentTimeMillis()))
    }

    fun toggleFavoritePrank(id: String) = viewModelScope.launch {
        if (id in favoritePranks.value) dao.removeFavoritePrank(id)
        else dao.addFavoritePrank(FavoritePrank(id, System.currentTimeMillis()))
    }

    fun start() {
        val cfg = _draft.value
        c.session.start(cfg)
        if (cfg.mode == HotspotMode.SYSTEM_HOTSPOT) SystemHotspotManager.openSettings(getApplication())
    }

    fun stop() = c.session.stop()
    fun extend(minutes: Int) = c.session.extend(minutes)
    fun retryPortal() = c.session.retryPortal()
    fun dismissError() = c.session.dismissError()

    /** Changing the page while live takes effect on the guest's next refresh. */
    fun choosePrank(p: PrankTemplate) {
        _draft.update { it.copy(prankId = p.id, randomPrank = false) }
        if (session.value.phase == com.shrey.prankspot.session.Phase.RUNNING) c.session.setPrank(p)
    }

    fun saveSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        c.settings.save(transform(settings.value))
    }

    fun clearHistory() = viewModelScope.launch { dao.clearSessions() }
}
