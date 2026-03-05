package org.hhp227.concafe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.hhp227.concafe.di.doInitKoin
import org.hhp227.concafe.presentation.App

fun main() = application {
    doInitKoin()
    Window(
        onCloseRequest = ::exitApplication,
        title = "ConCafe",
    ) {
        App()
    }
}
