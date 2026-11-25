package com.shrey.prankspot.hotspot

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Finds the IPv4 address this phone has on its own hotspot network, so the web server binds
 * only there. Hotspot interfaces are named ap0, swlan0, softap0, wlan1... depending on the maker;
 * the regular Wi-Fi client is wlan0 and mobile data is rmnet/ccmni.
 */
object HotspotInterfaces {
    private val hotspotPrefixes = listOf("ap", "swlan", "softap", "wlan1", "wlan2", "wigig")
    private val excludedPrefixes = listOf("lo", "rmnet", "ccmni", "dummy", "tun", "ppp", "ipsec", "v4-", "p2p", "aware", "nan", "eth", "radio", "rndis", "bt-pan", "ncm", "usb")

    data class Candidate(val name: String, val address: String)

    /** [exclude] holds the IPv4 addresses the phone uses as a client; the hotspot's own address is never one. */
    fun find(exclude: Set<String> = emptySet(), wifiDirect: Boolean = false): Candidate? =
        candidates(exclude, wifiDirect).firstOrNull()

    /** With [wifiDirect], only the Wi-Fi Direct group interface (p2p-…, 192.168.49.1) counts. */
    fun candidates(exclude: Set<String> = emptySet(), wifiDirect: Boolean = false): List<Candidate> {
        val all = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }.getOrDefault(emptyList())
        return all.asSequence()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .filter { i -> if (wifiDirect) i.name.startsWith("p2p") else excludedPrefixes.none { i.name.startsWith(it) } }
            .mapNotNull { i ->
                val v4 = i.inetAddresses.toList().filterIsInstance<Inet4Address>().firstOrNull { it.isSiteLocalAddress }
                v4?.let { Candidate(i.name, it.hostAddress!!) }
            }
            .filter { it.address !in exclude }
            // Known hotspot names first; anything else left (not upstream) is a weaker guess.
            .sortedBy { c -> if (hotspotPrefixes.any { c.name.startsWith(it) }) 0 else 1 }
            .toList()
    }
}
