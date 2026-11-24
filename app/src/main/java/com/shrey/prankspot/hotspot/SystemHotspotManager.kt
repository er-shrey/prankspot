package com.shrey.prankspot.hotspot

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import com.shrey.prankspot.domain.HotspotConfig
import com.shrey.prankspot.domain.HotspotInfo
import com.shrey.prankspot.domain.HotspotResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mode B: internet sharing. Ordinary apps can't switch tethering on, so the user turns on the
 * phone's own hotspot in Settings and this class waits until the hotspot interface appears.
 * Name and password are whatever the user set in Settings; we can't read them back.
 */
class SystemHotspotManager(private val context: Context) : HotspotManager {
    private val status = MutableStateFlow<HotspotStatus>(HotspotStatus.Idle)

    override fun observeStatus(): StateFlow<HotspotStatus> = status.asStateFlow()

    fun isHotspotUp(): Boolean = HotspotInterfaces.find(upstreamInterfaces(context)) != null

    /** Waits up to [timeoutMs] for the user to switch the system hotspot on. */
    override suspend fun start(config: HotspotConfig): HotspotResult {
        status.value = HotspotStatus.Starting
        val found = withTimeoutOrNull(TIMEOUT_MS) {
            while (!isHotspotUp()) delay(1_500)
            true
        }
        return if (found == true) {
            val info = HotspotInfo(ssid = null, password = null, security = config.security, customApplied = false)
            status.value = HotspotStatus.Running(info)
            HotspotResult.Started(info)
        } else {
            val msg = "No hotspot detected. Turn on Hotspot & tethering in Settings, then try again"
            status.value = HotspotStatus.Failed(msg)
            HotspotResult.Failed(msg)
        }
    }

    override suspend fun stop() {
        status.value = HotspotStatus.Idle
    }

    fun markStopped() {
        status.value = HotspotStatus.Idle
    }

    companion object {
        const val TIMEOUT_MS = 3 * 60_000L

        /**
         * Addresses the phone itself uses to reach the world (Wi-Fi client, mobile data, VPN).
         * Matched by address, not interface name: without STA/AP concurrency the hotspot reuses
         * wlan0, and the old Wi-Fi client network can linger on that name for a while.
         */
        @Suppress("DEPRECATION")
        fun upstreamInterfaces(context: Context): Set<String> {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            return cm.allNetworks.flatMap { n ->
                cm.getLinkProperties(n)?.linkAddresses.orEmpty().mapNotNull { it.address.hostAddress }
            }.toSet()
        }

        /** Opens the phone's hotspot screen; there's no public action for it, so try the known ones. */
        fun openSettings(context: Context) {
            val attempts = listOf(
                Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings")),
                Intent("android.settings.TETHER_SETTINGS"),
                Intent(Settings.ACTION_WIRELESS_SETTINGS),
                Intent(Settings.ACTION_SETTINGS),
            )
            for (i in attempts) {
                try {
                    context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
        }
    }
}

