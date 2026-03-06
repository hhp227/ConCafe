package org.hhp227.concafe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.hhp227.concafe.di.doInitConCafeAppKoin
import org.hhp227.concafe.presentation.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ConCafe",
    ) {
        App()
    }
    doInitConCafeAppKoin()
}
