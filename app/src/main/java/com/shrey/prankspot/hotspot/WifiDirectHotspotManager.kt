package com.shrey.prankspot.hotspot

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.shrey.prankspot.domain.DirectName
import com.shrey.prankspot.domain.HotspotConfig
import com.shrey.prankspot.domain.HotspotInfo
import com.shrey.prankspot.domain.HotspotResult
import com.shrey.prankspot.domain.Security
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Funny-name mode: a Wi-Fi Direct group the phone owns. Unlike the local-only hotspot, the public
 * API lets apps choose this network's name (it must start with "DIRECT-xy") and its WPA2
 * password. Other phones, laptops and iPhones join it like any Wi-Fi network; there's no internet.
 */
class WifiDirectHotspotManager(context: Context) : HotspotManager {
    private val app = context.applicationContext
    private val p2p: WifiP2pManager? = app.getSystemService(WifiP2pManager::class.java)
    private var channel: WifiP2pManager.Channel? = null
    private val status = MutableStateFlow<HotspotStatus>(HotspotStatus.Idle)

    override fun observeStatus(): StateFlow<HotspotStatus> = status.asStateFlow()

    private fun channel(): WifiP2pManager.Channel? {
        if (channel == null) channel = p2p?.initialize(app, Looper.getMainLooper()) { channel = null }
        return channel
    }

    @SuppressLint("MissingPermission")
    override suspend fun start(config: HotspotConfig): HotspotResult {
        val mgr = p2p
        val ch = channel()
        if (Build.VERSION.SDK_INT < 29 || mgr == null || ch == null) {
            return fail("This phone doesn't support Wi-Fi Direct groups. Try Share internet mode")
        }
        status.value = HotspotStatus.Starting
        // A group left over from another app or an earlier run blocks ours.
        call { l -> mgr.removeGroup(ch, l) }

        val name = DirectName.full(config.ssid)
        val p2pConfig = try {
            WifiP2pConfig.Builder()
                .setNetworkName(name)
                .setPassphrase(config.password)
                .setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_2GHZ)
                .enablePersistentMode(false)
                .build()
        } catch (e: IllegalArgumentException) {
            return fail(e.message ?: "That name or password isn't allowed")
        }
        val err = try {
            call { l -> mgr.createGroup(ch, p2pConfig, l) }
        } catch (e: SecurityException) {
            return fail("Permission missing: allow Nearby devices${if (Build.VERSION.SDK_INT < 33) " / Location" else ""} for PrankSpot")
        }
        if (err != null) return fail(reasonText(err))

        // createGroup only says the request was accepted; wait until the group really exists.
        repeat(30) {
            val g = group()
            if (g != null && g.isGroupOwner) {
                val info = HotspotInfo(g.networkName, g.passphrase, Security.WPA2, customApplied = g.networkName == name)
                status.value = HotspotStatus.Running(info)
                return HotspotResult.Started(info)
            }
            delay(500)
        }
        call { l -> mgr.removeGroup(ch, l) }
        return fail("Wi-Fi Direct didn't start. Turn Wi-Fi on and try again")
    }

    /** Polled by the session while running: null once the group is gone. */
    @SuppressLint("MissingPermission")
    suspend fun group(): WifiP2pGroup? {
        val mgr = p2p ?: return null
        val ch = channel() ?: return null
        return try {
            suspendCancellableCoroutine { c -> mgr.requestGroupInfo(ch) { g -> if (c.isActive) c.resume(g) } }
        } catch (e: SecurityException) {
            null
        }
    }

    override suspend fun stop() {
        val mgr = p2p
        val ch = channel
        if (mgr != null && ch != null) call { l -> mgr.removeGroup(ch, l) }
        if (status.value !is HotspotStatus.Failed) status.value = HotspotStatus.Idle
    }

    fun markStopped() {
        status.value = HotspotStatus.Idle
    }

    /** Runs a WifiP2pManager call; returns null on success or the failure reason. */
    private suspend fun call(block: (WifiP2pManager.ActionListener) -> Unit): Int? =
        suspendCancellableCoroutine { c ->
            block(object : WifiP2pManager.ActionListener {
                override fun onSuccess() { if (c.isActive) c.resume(null) }
                override fun onFailure(reason: Int) { if (c.isActive) c.resume(reason) }
            })
        }

    private fun fail(msg: String): HotspotResult.Failed {
        Log.w(TAG, msg)
        status.value = HotspotStatus.Failed(msg)
        return HotspotResult.Failed(msg)
    }

    private fun reasonText(reason: Int) = when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "This phone doesn't support Wi-Fi Direct. Try Share internet mode"
        WifiP2pManager.BUSY -> "Wi-Fi Direct is busy (another app or a connection is using it). Try again in a moment"
        else -> "Wi-Fi Direct refused to start. Make sure Wi-Fi is on, then try again"
    }

    companion object {
        private const val TAG = "WifiDirectHotspot"

        fun isSupported(context: Context): Boolean =
            Build.VERSION.SDK_INT >= 29 &&
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI_DIRECT)
    }
}
