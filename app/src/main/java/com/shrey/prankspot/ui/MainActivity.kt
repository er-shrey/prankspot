package com.shrey.prankspot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shrey.prankspot.ui.screens.ActiveScreen
import com.shrey.prankspot.ui.screens.CategoryScreen
import com.shrey.prankspot.ui.screens.ConfigureScreen
import com.shrey.prankspot.ui.screens.DevicesScreen
import com.shrey.prankspot.ui.screens.HomeScreen
import com.shrey.prankspot.ui.screens.LibraryScreen
import com.shrey.prankspot.ui.screens.PreviewScreen
import com.shrey.prankspot.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CONFIGURE = "configure"
    const val ACTIVE = "active"
    const val LIBRARY = "library"
    const val PREVIEW = "preview/{id}"
    const val DEVICES = "devices"
    const val SETTINGS = "settings"
    fun preview(id: String) = "preview/$id"
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            PrankSpotTheme(settings.theme) {
                AppNav(vm, rememberNavController())
            }
        }
    }
}

@Composable
private fun AppNav(vm: AppViewModel, nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(
        nav, startDestination = Routes.HOME,
        enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                vm,
                onCreate = { nav.navigate(Routes.CATEGORIES) },
                onCategory = { vm.selectCategory(it); nav.navigate(Routes.CONFIGURE) },
                onActive = { nav.navigate(Routes.ACTIVE) },
                onLibrary = { nav.navigate(Routes.LIBRARY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoryScreen(vm, onBack = back, onPick = {
                vm.selectCategory(it)
                nav.navigate(Routes.CONFIGURE) { popUpTo(Routes.HOME) }
            })
        }
        composable(Routes.CONFIGURE) {
            ConfigureScreen(
                vm, onBack = back,
                onChangeCategory = { nav.navigate(Routes.CATEGORIES) },
                onLibrary = { nav.navigate(Routes.LIBRARY) },
                onPreview = { nav.navigate(Routes.preview(it)) },
                onStarted = { nav.navigate(Routes.ACTIVE) { popUpTo(Routes.HOME) } },
            )
        }
        composable(Routes.ACTIVE) {
            ActiveScreen(
                vm, onBack = back,
                onDevices = { nav.navigate(Routes.DEVICES) },
                onLibrary = { nav.navigate(Routes.LIBRARY) },
                onPreview = { nav.navigate(Routes.preview(it)) },
                onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(vm, onBack = back, onPreview = { nav.navigate(Routes.preview(it)) }, onChosen = back)
        }
        composable(Routes.PREVIEW) { entry ->
            PreviewScreen(vm, entry.arguments?.getString("id").orEmpty(), onBack = back)
        }
        composable(Routes.DEVICES) { DevicesScreen(vm, onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(vm, onBack = back) }
    }
}
