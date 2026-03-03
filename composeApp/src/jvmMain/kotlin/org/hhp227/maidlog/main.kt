package org.hhp227.maidlog

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MaidLog",
    ) {
        App()
    }
}