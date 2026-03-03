package org.hhp227.maidlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform