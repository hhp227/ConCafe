package com.hhp227.concafe.di

import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.DismissReviewPromptUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetCafeReviewPageUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import com.hhp227.concafe.domain.usecase.CreateVisitUseCase
import com.hhp227.concafe.domain.usecase.CreateReviewUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import com.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import com.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.domain.usecase.GetFanManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import com.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import com.hhp227.concafe.domain.usecase.GetScheduleManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetSignUpCafeListUseCase
import com.hhp227.concafe.domain.usecase.MarkNotificationReadUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeCastVersionUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveReviewEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastVersionUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.ShouldShowReviewPromptUseCase
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignUpUseCase
import com.hhp227.concafe.domain.usecase.SignOutUseCase
import com.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeInfoUseCase
import com.hhp227.concafe.domain.usecase.UpsertCastUseCase
import com.hhp227.concafe.domain.usecase.UpsertCafeMenuGoodsUseCase
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

fun resolveGetCafeManagementUseCase(): GetCafeManagementUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetCafeDashboardUseCase(): GetCafeDashboardUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetCafeCastPageUseCase(): GetCafeCastPageUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetCafeCastListPageUseCase(): GetCafeCastListPageUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetCheckInGuestFeedUseCase(): GetCheckInGuestFeedUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateVisitUseCase(): CreateVisitUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateReviewUseCase(): CreateReviewUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveDeleteCafeMenuGoodsUseCase(): DeleteCafeMenuGoodsUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveDismissReviewPromptUseCase(): DismissReviewPromptUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetCheckInUserFeedUseCase(): GetCheckInUserFeedUseCase {
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

fun resolveGetCafeReviewPageUseCase(): GetCafeReviewPageUseCase {
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

fun resolveGetFanManagementDataUseCase(): GetFanManagementDataUseCase {
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

fun resolveGetScheduleManagementDataUseCase(): GetScheduleManagementDataUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetSignUpCafeListUseCase(): GetSignUpCafeListUseCase {
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

fun resolveSignUpUseCase(): SignUpUseCase {
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

fun resolveObserveCafeDetailUseCase(): ObserveCafeDetailUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveObserveCafeDetailEventUseCase(): ObserveCafeDetailEventUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveObserveCafeCastVersionUseCase(): ObserveCafeCastVersionUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveObserveReviewEventUseCase(): ObserveReviewEventUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveObserveCastEventUseCase(): ObserveCastEventUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveObserveCastVersionUseCase(): ObserveCastVersionUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveShouldShowReviewPromptUseCase(): ShouldShowReviewPromptUseCase {
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

fun resolveUpdateCafeInfoUseCase(): UpdateCafeInfoUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveUpsertCastUseCase(): UpsertCastUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveUpsertCafeMenuGoodsUseCase(): UpsertCafeMenuGoodsUseCase {
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
