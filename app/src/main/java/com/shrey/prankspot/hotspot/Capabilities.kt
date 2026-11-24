package com.shrey.prankspot.hotspot

import android.os.Build

object Capabilities {
    /**
     * SoftApConfiguration.Builder.setWifiSsid / setPassphrase became public in Android 16 QPR2
     * (API 36.1). Before that the system picks the local-only hotspot's name and password.
     */
    val canCustomizeLocalOnly: Boolean
        get() = Build.VERSION.SDK_INT >= 36 &&
            Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1

    val androidLabel: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
