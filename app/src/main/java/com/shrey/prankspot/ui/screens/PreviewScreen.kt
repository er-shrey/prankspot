package com.shrey.prankspot.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.portal.PageRenderer
import com.shrey.prankspot.ui.AppViewModel
import com.shrey.prankspot.ui.PrankWebView

@Composable
fun PreviewScreen(vm: AppViewModel, prankId: String, onBack: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val prank = vm.prank(prankId)
    Screen("Preview: what guests see", onBack) { pad ->
        if (prank == null) {
            Text("That prank page is missing.", Modifier.padding(pad))
        } else {
            // Personal touches apply only to the page chosen in setup.
            val mine = draft.prankId == prankId
            val html = remember(prankId, draft.customTitle, draft.customMessage, settings.soundEffects) {
                PageRenderer.render(
                    prank,
                    if (mine) draft.customTitle else "",
                    if (mine) draft.customMessage else "",
                    settings.soundEffects,
                )
            }
            PrankWebView(html, Modifier.fillMaxSize().padding(pad))
        }
    }
}
