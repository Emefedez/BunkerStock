package com.bunkerstock.web

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.bunkerstock.app.ui.BunkerController
import com.bunkerstock.app.ui.BunkerStockRoot
import kotlinx.browser.document

private fun jsHideLoader(): Unit = js("window.BunkerWeb.hideLoader()")
private fun jsGetInitialHashInvite(): String = js("window.BunkerWeb.getInitialHashInvite()")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.getElementById("compose-root") ?: document.body ?: return
    ComposeViewport(root) {
        val scope = rememberCoroutineScope()
        val storage = remember { WebBunkerStorage() }
        val platform = remember { WebBunkerPlatform(scope) }
        val controller = remember { BunkerController(scope, storage, platform) }

        LaunchedEffect(Unit) {
            jsHideLoader()
            val invite = jsGetInitialHashInvite()
            if (invite.isNotBlank()) {
                controller.joinFamilyByInvite(invite)
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Use side NavigationRail only on landscape tablet/desktop so portrait iPad keeps centered bottom nav
            val isExpanded = maxWidth >= 840.dp && maxWidth > maxHeight
            BunkerStockRoot(
                controller = controller,
                platform = platform,
                isExpandedWidth = isExpanded
            )
        }
    }
}
