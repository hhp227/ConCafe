package com.hhp227.concafe.di

import org.koin.core.module.Module

// Desktop has no platform-specific presentation bindings; device gateways live in shared/jvmMain.
internal fun jvmPlatformModules(): List<Module> {
    return emptyList()
}
