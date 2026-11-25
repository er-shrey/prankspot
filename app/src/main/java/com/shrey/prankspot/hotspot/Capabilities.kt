package com.shrey.prankspot.hotspot

import android.os.Build

object Capabilities {
    val androidLabel: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
