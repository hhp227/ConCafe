package org.hhp227.concafe.di

import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import org.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import org.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import org.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import org.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import org.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import org.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import org.hhp227.concafe.domain.usecase.MarkNotificationReadUseCase
import org.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import org.hhp227.concafe.domain.usecase.SignInUseCase
import org.hhp227.concafe.domain.usecase.SignOutUseCase
import org.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase
import org.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase
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

fun resolveGetCafeDetailUseCase(): GetCafeDetailUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetCastDetailUseCase(): GetCastDetailUseCase {
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

fun resolveGetRankingFeedUseCase(): GetRankingFeedUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetNotificationFeedUseCase(): GetNotificationFeedUseCase {
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

fun resolveSignInUseCase(): SignInUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveMarkNotificationReadUseCase(): MarkNotificationReadUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveToggleFavoriteCafeUseCase(): ToggleFavoriteCafeUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveToggleFollowCastUseCase(): ToggleFollowCastUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveObserveCurrentUserUseCase(): ObserveCurrentUserUseCase {
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
