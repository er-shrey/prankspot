package com.shrey.prankspot.hotspot

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiSsid
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shrey.prankspot.domain.HotspotConfig
import com.shrey.prankspot.domain.HotspotInfo
import com.shrey.prankspot.domain.HotspotResult
import com.shrey.prankspot.domain.Security
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/** Mode A: WifiManager.startLocalOnlyHotspot — no internet, just this phone and its guests. */
class LocalOnlyHotspotManager(context: Context) : HotspotManager {
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { main.post(it) }
    private val status = MutableStateFlow<HotspotStatus>(HotspotStatus.Idle)
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    override fun observeStatus(): StateFlow<HotspotStatus> = status.asStateFlow()

    override suspend fun start(config: HotspotConfig): HotspotResult {
        stop()
        status.value = HotspotStatus.Starting
        if (Capabilities.canCustomizeLocalOnly) {
            val custom = startOnce(config, customize = true)
            if (custom is HotspotResult.Started) return custom
            Log.w(TAG, "Custom local-only hotspot refused (${(custom as HotspotResult.Failed).reason}); retrying with system name")
        }
        return startOnce(config, customize = false)
    }

    @SuppressLint("MissingPermission", "NewApi")
    private suspend fun startOnce(config: HotspotConfig, customize: Boolean): HotspotResult =
        suspendCancellableCoroutine { cont ->
            val callback = object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = res
                    val info = readInfo(res, config, customize)
                    Log.i(TAG, "Started (customize=$customize): ssid=${info.ssid} applied=${info.customApplied}")
                    status.value = HotspotStatus.Running(info)
                    if (cont.isActive) cont.resume(HotspotResult.Started(info)) else res.close()
                }

                override fun onStopped() {
                    reservation = null
                    status.value = HotspotStatus.Idle
                }

                override fun onFailed(reason: Int) {
                    val msg = failureText(reason)
                    status.value = HotspotStatus.Failed(msg)
                    if (cont.isActive) cont.resume(HotspotResult.Failed(msg))
                }
            }
            try {
                if (customize) {
                    wifi.startLocalOnlyHotspotWithConfiguration(softApConfig(config), mainExecutor, callback)
                } else {
                    wifi.startLocalOnlyHotspot(callback, main)
                }
            } catch (e: SecurityException) {
                val msg = "Permission missing: allow Nearby devices${if (Build.VERSION.SDK_INT < 33) " / Location" else ""} for PrankSpot"
                status.value = HotspotStatus.Failed(msg)
                cont.resume(HotspotResult.Failed(msg))
            } catch (e: Exception) {
                val msg = e.message ?: "The hotspot could not start"
                status.value = HotspotStatus.Failed(msg)
                cont.resume(HotspotResult.Failed(msg))
            }
        }

    @SuppressLint("NewApi")
    private fun softApConfig(config: HotspotConfig): SoftApConfiguration {
        val b = SoftApConfiguration.Builder()
            .setWifiSsid(WifiSsid.fromBytes(config.ssid.toByteArray(Charsets.UTF_8)))
        if (config.security == Security.OPEN) {
            b.setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
        } else {
            b.setPassphrase(config.password, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)
        }
        return b.build()
    }

    @Suppress("DEPRECATION")
    private fun readInfo(res: WifiManager.LocalOnlyHotspotReservation, config: HotspotConfig, customize: Boolean): HotspotInfo {
        if (Build.VERSION.SDK_INT >= 30) {
            val c = res.softApConfiguration
            val ssid = if (Build.VERSION.SDK_INT >= 33) c.wifiSsid?.toString()?.unquote() else c.ssid?.unquote()
            val pass = c.passphrase
            val open = c.securityType == SoftApConfiguration.SECURITY_TYPE_OPEN
            return HotspotInfo(
                ssid = ssid,
                password = pass,
                security = if (open) Security.OPEN else Security.WPA2,
                customApplied = customize && ssid == config.ssid,
            )
        }
        val wc = res.wifiConfiguration
        return HotspotInfo(
            ssid = wc?.SSID?.unquote(),
            password = wc?.preSharedKey?.unquote(),
            security = if (wc?.preSharedKey.isNullOrEmpty()) Security.OPEN else Security.WPA2,
            customApplied = false,
        )
    }

    override suspend fun stop() {
        reservation?.close()
        reservation = null
        if (status.value !is HotspotStatus.Failed) status.value = HotspotStatus.Idle
    }

    private fun String.unquote() = removeSurrounding("\"")

    private fun failureText(reason: Int) = when (reason) {
        WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE ->
            "Turn off your phone's regular hotspot first: Android can't run both at once"
        WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED ->
            "Hotspots are blocked on this phone (device policy)"
        WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL ->
            "No free Wi-Fi channel right now. Try again in a moment"
        else -> "The phone refused to start the hotspot. Try toggling Wi-Fi and retry"
    }

    private companion object {
        const val TAG = "LocalOnlyHotspot"
    }
}
