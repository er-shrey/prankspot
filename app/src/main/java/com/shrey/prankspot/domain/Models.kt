package com.shrey.prankspot.domain

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val ssids: List<String>,
    val pranks: List<PrankTemplate>,
)

data class PrankTemplate(
    val id: String,
    val categoryId: String,
    val name: String,
    val emoji: String,
    val title: String,
    val message: String,
    val extras: List<String>,
    val button: String,
    val reveal: String,
    val bgStart: String,
    val bgEnd: String,
    val textColor: String,
    val accent: String,
    val animation: String,
    val sound: String,
)

enum class HotspotMode { LOCAL_ONLY, SYSTEM_HOTSPOT }

enum class Security { OPEN, WPA2 }

/** What the user asked for. The OS may not honour every field (see [HotspotInfo.customApplied]). */
data class HotspotConfig(
    val mode: HotspotMode = HotspotMode.LOCAL_ONLY,
    val categoryId: String = "custom",
    val ssid: String = "",
    val password: String = "",
    val security: Security = Security.WPA2,
    /** 0 means no time limit. */
    val durationMinutes: Int = 30,
    val portalEnabled: Boolean = true,
    val prankId: String = "",
    val randomPrank: Boolean = false,
    val customTitle: String = "",
    val customMessage: String = "",
)

/** What the OS actually started. */
data class HotspotInfo(
    val ssid: String?,
    val password: String?,
    val security: Security,
    /** True when the hotspot is using the name and password the user chose. */
    val customApplied: Boolean,
)

sealed interface HotspotResult {
    data class Started(val info: HotspotInfo) : HotspotResult
    data class Failed(val reason: String) : HotspotResult
}

data class Visitor(
    val ip: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val visits: Int,
    val userAgent: String,
)
