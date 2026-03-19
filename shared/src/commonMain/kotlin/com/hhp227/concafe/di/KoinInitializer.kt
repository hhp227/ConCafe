package com.hhp227.concafe.di

import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.DismissReviewPromptUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeEventUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.CreateHomeBannerUseCase
import com.hhp227.concafe.domain.usecase.CreateInquiryUseCase
import com.hhp227.concafe.domain.usecase.ApproveCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.ApproveCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeEventUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.DeleteCastUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeReviewPageUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import com.hhp227.concafe.domain.usecase.CreateVisitUseCase
import com.hhp227.concafe.domain.usecase.CreateReviewUseCase
import com.hhp227.concafe.domain.usecase.CreateCastClaimUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import com.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import com.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import com.hhp227.concafe.domain.usecase.GetExploreCafePageUseCase
import com.hhp227.concafe.domain.usecase.GetExploreCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.domain.usecase.GetFanManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetMyCastClaimStatusUseCase
import com.hhp227.concafe.domain.usecase.GetMyRequestableCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import com.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import com.hhp227.concafe.domain.usecase.GetScheduleManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetSignUpCafeListUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCastClaimsForCafeUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeOwnerClaimsUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeRegistrationClaimsUseCase
import com.hhp227.concafe.domain.usecase.MarkNotificationReadUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.ShouldShowReviewPromptUseCase
import com.hhp227.concafe.domain.usecase.RestoreSessionUseCase
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignUpUseCase
import com.hhp227.concafe.domain.usecase.SignOutUseCase
import com.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeInfoUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeEventUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.UpdateCastScheduleUseCase
import com.hhp227.concafe.domain.usecase.ApproveCastClaimUseCase
import com.hhp227.concafe.domain.usecase.RejectCastClaimUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.UpsertCastUseCase
import com.hhp227.concafe.domain.usecase.UpsertCafeMenuGoodsUseCase
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform.getKoin

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

fun resolveGetExploreCafePageUseCase(): GetExploreCafePageUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetExploreCastPageUseCase(): GetExploreCastPageUseCase {
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

fun resolveGetCafeEventPageUseCase(): GetCafeEventPageUseCase {
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

fun resolveCreateCastClaimUseCase(): CreateCastClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateCafeEventUseCase(): CreateCafeEventUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateCafeNoticeUseCase(): CreateCafeNoticeUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateHomeBannerUseCase(): CreateHomeBannerUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateInquiryUseCase(): CreateInquiryUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateCafeOwnerClaimUseCase(): CreateCafeOwnerClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCreateCafeRegistrationClaimUseCase(): CreateCafeRegistrationClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveDeleteCafeEventUseCase(): DeleteCafeEventUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveDeleteCafeNoticeUseCase(): DeleteCafeNoticeUseCase {
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

fun resolveDeleteCastUseCase(): DeleteCastUseCase {
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

fun resolveGetCafeNoticePageUseCase(): GetCafeNoticePageUseCase {
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

fun resolveGetMyCastClaimStatusUseCase(): GetMyCastClaimStatusUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetMyRequestableCastPageUseCase(): GetMyRequestableCastPageUseCase {
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

fun resolveGetPendingCastClaimsForCafeUseCase(): GetPendingCastClaimsForCafeUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetPendingCafeOwnerClaimsUseCase(): GetPendingCafeOwnerClaimsUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveGetPendingCafeRegistrationClaimsUseCase(): GetPendingCafeRegistrationClaimsUseCase {
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

fun resolveRestoreSessionUseCase(): RestoreSessionUseCase {
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

fun resolveUpdateCafeEventUseCase(): UpdateCafeEventUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveUpdateCafeNoticeUseCase(): UpdateCafeNoticeUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveUpdateCastScheduleUseCase(): UpdateCastScheduleUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveApproveCastClaimUseCase(): ApproveCastClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveApproveCafeOwnerClaimUseCase(): ApproveCafeOwnerClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveApproveCafeRegistrationClaimUseCase(): ApproveCafeRegistrationClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveRejectCastClaimUseCase(): RejectCastClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveRejectCafeOwnerClaimUseCase(): RejectCafeOwnerClaimUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveRejectCafeRegistrationClaimUseCase(): RejectCafeRegistrationClaimUseCase {
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

fun resolveUploadImageUseCase(): UploadImageUseCase {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

/*
    아래부터는 ResolveEventPublisher 관련 메소드
 */

fun resolveBannerEventPublisher(): BannerEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCafeDetailEventPublisher(): CafeDetailEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCafeRegistrationClaimEventPublisher(): CafeRegistrationClaimEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCastClaimEventPublisher(): CastClaimEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveCastEventPublisher(): CastEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveNoticeManagementEventPublisher(): NoticeManagementEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveReviewEventPublisher(): ReviewEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}

fun resolveScheduleManagementEventPublisher(): ScheduleManagementEventPublisher {
    val koin = requireNotNull(koinApplication?.koin) {
        "Koin is not initialized. Call doInitKoin() before resolving dependencies."
    }
    return koin.get()
}
