package com.shrey.prankspot.hotspot

import com.shrey.prankspot.domain.HotspotConfig
import com.shrey.prankspot.domain.HotspotInfo
import com.shrey.prankspot.domain.HotspotResult
import kotlinx.coroutines.flow.StateFlow

sealed interface HotspotStatus {
    data object Idle : HotspotStatus
    data object Starting : HotspotStatus
    /** Only set once the OS has confirmed the network is up. */
    data class Running(val info: HotspotInfo) : HotspotStatus
    data class Failed(val reason: String) : HotspotStatus
}

interface HotspotManager {
    suspend fun start(config: HotspotConfig): HotspotResult
    suspend fun stop()
    fun observeStatus(): StateFlow<HotspotStatus>
    suspend fun getConfiguration(): HotspotInfo? =
        (observeStatus().value as? HotspotStatus.Running)?.info
}
