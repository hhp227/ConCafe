package org.hhp227.concafe.di

import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun doInitKoin(): KoinApplication? {
    return doInitKoin(emptyList())
}

fun doInitKoin(extraModules: List<Module>): KoinApplication? {
    val current = GlobalContext.getOrNull()
    return if (current == null) {
        startKoin {
            modules(concafeModules + extraModules)
        }
    } else {
        null
    }
}
