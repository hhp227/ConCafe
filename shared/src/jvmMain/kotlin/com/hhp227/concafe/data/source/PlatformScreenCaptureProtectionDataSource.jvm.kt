package com.hhp227.concafe.data.source

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Window

object DesktopScreenCaptureProtectionWindowHolder {
    var currentWindow: Window? = null
}

actual fun createPlatformScreenCaptureProtectionDataSource(): ScreenCaptureProtectionDataSource {
    return AwtScreenCaptureProtectionDataSource()
}

private class AwtScreenCaptureProtectionDataSource : ScreenCaptureProtectionDataSource {
    override fun enable() {
        setWindowsDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)
    }

    override fun disable() {
        setWindowsDisplayAffinity(WDA_NONE)
    }

    private fun setWindowsDisplayAffinity(affinity: Int) {
        if (!isWindows()) {
            return
        }

        val window = DesktopScreenCaptureProtectionWindowHolder.currentWindow ?: return

        runCatching {
            val hwnd = Native.getWindowPointer(window)
            WindowsUser32.INSTANCE.SetWindowDisplayAffinity(hwnd, affinity)
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name")
            ?.contains("Windows", ignoreCase = true) == true
    }
}

private interface WindowsUser32 : Library {
    fun SetWindowDisplayAffinity(windowHandle: Pointer, displayAffinity: Int): Boolean

    companion object {
        val INSTANCE: WindowsUser32 = Native.load("user32", WindowsUser32::class.java)
    }
}

private const val WDA_NONE = 0x00000000
private const val WDA_EXCLUDEFROMCAPTURE = 0x00000011
