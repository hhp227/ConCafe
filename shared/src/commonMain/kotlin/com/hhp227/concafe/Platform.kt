package com.hhp227.concafe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform