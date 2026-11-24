package com.shrey.prankspot

import android.app.Application
import android.content.Context
import com.shrey.prankspot.data.CatalogRepository
import com.shrey.prankspot.data.PrankDatabase
import com.shrey.prankspot.data.SettingsRepository
import com.shrey.prankspot.hotspot.LocalOnlyHotspotManager
import com.shrey.prankspot.hotspot.SystemHotspotManager
import com.shrey.prankspot.session.SessionController

/** Manual dependency container: every platform service is built here and passed in. */
class AppContainer(context: Context) {
    val catalog = CatalogRepository(context)
    val database = PrankDatabase.create(context)
    val settings = SettingsRepository(context)
    val session = SessionController(
        context = context,
        catalog = catalog,
        dao = database.dao(),
        settings = settings,
        localOnly = LocalOnlyHotspotManager(context),
        system = SystemHotspotManager(context),
    )
}

class PrankSpotApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) android.webkit.WebView.setWebContentsDebuggingEnabled(true)
        container = AppContainer(this)
    }
}
