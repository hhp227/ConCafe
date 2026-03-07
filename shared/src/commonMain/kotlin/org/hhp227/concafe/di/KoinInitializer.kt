package org.hhp227.concafe.di

import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import org.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import org.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import org.hhp227.concafe.domain.usecase.SignOutUseCase
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

private var koinApplication: KoinApplication? = null

fun doInitKoin(): KoinApplication? {
    return doInitKoin(emptyList())
}

fun doInitKoin(extraModules: List<Module>): KoinApplication? {
    return if (koinApplication == null) {
        startKoin {
            modules(concafeModules + extraModules)
        }.also { koinApplication = it }
    } else {
        null
    }
}

fun resolveGetHomeFeedUseCase(): GetHomeFeedUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetExploreFeedUseCase(): GetExploreFeedUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetMyInfoUseCase(): GetMyInfoUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetMainNavigationUseCase(): GetMainNavigationUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveSignOutUseCase(): SignOutUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveAuthRepository(): AuthRepository {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}
