package com.shrey.prankspot.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shrey.prankspot.domain.HotspotMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val defaultMode: HotspotMode = HotspotMode.WIFI_DIRECT,
    val defaultCategory: String = "birthday",
    val defaultPrankId: String = "",
    val defaultDurationMinutes: Int = 30,
    val autoShutdown: Boolean = true,
    val randomPrank: Boolean = false,
    val soundEffects: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val mode = stringPreferencesKey("default_mode")
        val category = stringPreferencesKey("default_category")
        val prank = stringPreferencesKey("default_prank")
        val duration = intPreferencesKey("default_duration")
        val autoShutdown = booleanPreferencesKey("auto_shutdown")
        val random = booleanPreferencesKey("random_prank")
        val sound = booleanPreferencesKey("sound_effects")
        val theme = stringPreferencesKey("theme")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            defaultMode = p[K.mode].enumOr(d.defaultMode),
            defaultCategory = p[K.category] ?: d.defaultCategory,
            defaultPrankId = p[K.prank] ?: d.defaultPrankId,
            defaultDurationMinutes = p[K.duration] ?: d.defaultDurationMinutes,
            autoShutdown = p[K.autoShutdown] ?: d.autoShutdown,
            randomPrank = p[K.random] ?: d.randomPrank,
            soundEffects = p[K.sound] ?: d.soundEffects,
            theme = p[K.theme].enumOr(d.theme),
        )
    }

    suspend fun save(s: AppSettings) {
        context.dataStore.edit { p ->
            p[K.mode] = s.defaultMode.name
            p[K.category] = s.defaultCategory
            p[K.prank] = s.defaultPrankId
            p[K.duration] = s.defaultDurationMinutes
            p[K.autoShutdown] = s.autoShutdown
            p[K.random] = s.randomPrank
            p[K.sound] = s.soundEffects
            p[K.theme] = s.theme.name
        }
    }

    private inline fun <reified E : Enum<E>> String?.enumOr(default: E): E =
        this?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default
}
