package com.hhp227.concafe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.desktop_icon
import com.hhp227.concafe.di.doInitConCafeAppKoin
import com.hhp227.concafe.presentation.App
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "콘카(ConCafe)",
        icon = painterResource(Res.drawable.desktop_icon),
    ) {
        App()
    }
    doInitConCafeAppKoin()
}
