package com.hhp227.maidlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform