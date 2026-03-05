package org.hhp227.concafe.di

import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun doInitKoin(): KoinApplication? {
    val current = GlobalContext.getOrNull()

    return if (current == null) {
        startKoin {
            modules(concafeModules)
        }
    } else {
        null
    }
}
