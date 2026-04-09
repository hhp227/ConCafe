package com.hhp227.concafe.presentation.settings

actual fun currentAppVersion(): String =
    requireNotNull(System.getProperty("app.version") ?: object {}.javaClass.`package`?.implementationVersion) {
        "app.version system property or package implementation version must be configured"
    }
