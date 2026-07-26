package com.hhp227.concafe

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

private val logTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun installDesktopCrashLogger() {
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        appendDesktopLog(
            buildString {
                appendLine("[${timestamp()}] Uncaught exception on thread=${thread.name}")
                appendLine(stackTraceString(throwable))
            }
        )
        previousHandler?.uncaughtException(thread, throwable)
    }
    appendDesktopLog("[${timestamp()}] Desktop logger initialized")
}

fun appendDesktopLog(message: String) {
    runCatching {
        val logFile = desktopLogFilePath()
        logFile.parent?.createDirectories()
        if (!logFile.exists()) {
            Files.createFile(logFile)
            // BOM을 넣어 Windows 메모장이 UTF-8로 확실히 인식하게 만든다.
            logFile.writeText("\uFEFF")
        }
        logFile.appendText(message.trimEnd() + "\n")
    }
}

private fun desktopLogFilePath(): Path {
    val localAppData = System.getenv("LOCALAPPDATA").orEmpty().trim()
    return if (localAppData.isNotEmpty()) {
        Paths.get(localAppData, "ConCafe", "logs", "desktop.log")
    } else {
        Paths.get(System.getProperty("java.io.tmpdir"), "ConCafe", "logs", "desktop.log")
    }
}

private fun timestamp(): String = LocalDateTime.now().format(logTimeFormatter)

private fun stackTraceString(throwable: Throwable): String {
    val writer = StringWriter()
    PrintWriter(writer).use { printWriter ->
        throwable.printStackTrace(printWriter)
    }
    return writer.toString()
}
