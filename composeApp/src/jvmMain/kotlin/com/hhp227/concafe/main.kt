package com.hhp227.concafe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.desktop_icon
import com.hhp227.concafe.di.doInitConCafeAppKoin
import com.hhp227.concafe.di.jvmPlatformModules
import com.hhp227.concafe.presentation.App
import java.io.File
import org.jetbrains.compose.resources.painterResource

fun main() {
    // Must run before `application {}` so Compose runtime never caches an invalid external resources dir.
    normalizeComposeResourcesDirProperty()
    installDesktopCrashLogger()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "콘카(ConCafe)",
            icon = painterResource(Res.drawable.desktop_icon),
        ) {
            App()
        }
        doInitConCafeAppKoin(jvmPlatformModules())
    }
}

private fun normalizeComposeResourcesDirProperty() {
    val propertyName = "compose.application.resources.dir"
    val raw = System.getProperty(propertyName)?.trim().orEmpty()

    if (raw.isBlank()) {
        return
    }

    val isUnresolvedTemplatePath = raw.contains("\$APPDIR", ignoreCase = true)
    val dir = File(raw)
    val hasValidExternalResources =
        !isUnresolvedTemplatePath &&
            dir.exists() &&
            dir.isDirectory &&
            dir.walkTopDown().any { entry -> entry.isFile && entry.extension == "cvr" }

    if (!hasValidExternalResources) {
        System.clearProperty(propertyName)
        appendDesktopLog(
            "[${java.time.LocalDateTime.now()}] Cleared invalid $propertyName: '$raw'"
        )
    }
}
