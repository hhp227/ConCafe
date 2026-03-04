package org.hhp227.concafe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ConCafe",
    ) {
        App()
    }
}