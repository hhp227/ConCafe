package com.hhp227.concafe.di

import com.hhp227.concafe.data.repository.FakeAuthRepository
import com.hhp227.concafe.data.repository.FakeBannerRepository
import com.hhp227.concafe.data.repository.FakeCafeDashboardRepository
import com.hhp227.concafe.data.repository.FakeCafeManagementRepository
import com.hhp227.concafe.data.repository.FakeCafeOwnerClaimRepository
import com.hhp227.concafe.data.repository.FakeCafeRegistrationClaimRepository
import com.hhp227.concafe.data.repository.FakeCafeRepository
import com.hhp227.concafe.data.repository.FakeCastRepository
import com.hhp227.concafe.data.repository.FakeCastClaimRepository
import com.hhp227.concafe.data.repository.FakeInquiryRepository
import com.hhp227.concafe.data.repository.FakeNoticeRepository
import com.hhp227.concafe.data.repository.FakeNotificationRepository
import com.hhp227.concafe.data.repository.FakeRankingRepository
import com.hhp227.concafe.data.repository.FakeReviewRepository
import com.hhp227.concafe.data.repository.FakeUserRepository
import com.hhp227.concafe.data.repository.FakeVisitRepository
import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.dataevent.publisher.BannerEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.CafeDetailEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.CafeRegistrationClaimEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.CastClaimEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.CastEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.NoticeManagementEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.ReviewEventPublisherImpl
import com.hhp227.concafe.dataevent.publisher.ScheduleManagementEventPublisherImpl
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CafeDashboardRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CafeOwnerClaimRepository
import com.hhp227.concafe.domain.repository.CafeRegistrationClaimRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.CastClaimRepository
import com.hhp227.concafe.domain.repository.InquiryRepository
import com.hhp227.concafe.domain.repository.NoticeRepository
import com.hhp227.concafe.domain.repository.NotificationRepository
import com.hhp227.concafe.domain.repository.RankingRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository
import com.hhp227.concafe.domain.repository.VisitRepository
import com.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import com.hhp227.concafe.domain.usecase.GetExploreCafePageUseCase
import com.hhp227.concafe.domain.usecase.GetExploreCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeReviewPageUseCase
import com.hhp227.concafe.domain.usecase.GetFanManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import com.hhp227.concafe.domain.usecase.CreateVisitUseCase
import com.hhp227.concafe.domain.usecase.CreateReviewUseCase
import com.hhp227.concafe.domain.usecase.CreateCastClaimUseCase
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
import com.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.DeleteCastUseCase
import com.hhp227.concafe.domain.usecase.DismissReviewPromptUseCase
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import com.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import com.hhp227.concafe.domain.usecase.GetMyCastClaimStatusUseCase
import com.hhp227.concafe.domain.usecase.GetMyRequestableCastPageUseCase
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
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignUpUseCase
import com.hhp227.concafe.domain.usecase.SignOutUseCase
import com.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase
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
import org.koin.dsl.module
import kotlin.math.sin

val dataSourceModule = module {
    single<ConCafeDataSource> { MockConCafeDataSource() }
}

val repositoryModule = module {
    single<AuthRepository> { FakeAuthRepository(get()) }
    single<UserRepository> { FakeUserRepository(get()) }
    single<BannerRepository> { FakeBannerRepository(get()) }
    single<CafeDashboardRepository> { FakeCafeDashboardRepository(get()) }
    single<CafeManagementRepository> { FakeCafeManagementRepository(get()) }
    single<CafeOwnerClaimRepository> { FakeCafeOwnerClaimRepository(get()) }
    single<CafeRegistrationClaimRepository> { FakeCafeRegistrationClaimRepository(get()) }
    single<CafeRepository> { FakeCafeRepository(get()) }
    single<CastRepository> { FakeCastRepository(get()) }
    single<CastClaimRepository> { FakeCastClaimRepository(get()) }
    single<InquiryRepository> { FakeInquiryRepository(get()) }
    single<VisitRepository> { FakeVisitRepository(get()) }
    single<ReviewRepository> { FakeReviewRepository(get()) }
    single<NoticeRepository> { FakeNoticeRepository(get()) }
    single<RankingRepository> { FakeRankingRepository(get()) }
    single<NotificationRepository> { FakeNotificationRepository(get()) }
}

val eventModule = module {
    single<BannerEventPublisher> { BannerEventPublisherImpl() }
    single<CafeDetailEventPublisher> { CafeDetailEventPublisherImpl() }
    single<CafeRegistrationClaimEventPublisher> { CafeRegistrationClaimEventPublisherImpl() }
    single<CastClaimEventPublisher> { CastClaimEventPublisherImpl() }
    single<CastEventPublisher> { CastEventPublisherImpl() }
    single<NoticeManagementEventPublisher> { NoticeManagementEventPublisherImpl() }
    single<ReviewEventPublisher> { ReviewEventPublisherImpl() }
    single<ScheduleManagementEventPublisher> { ScheduleManagementEventPublisherImpl() }
}

val useCaseModule = module {
    factory { GetHomeFeedUseCase(get(), get(), get(), get()) }
    factory { GetCafeDashboardUseCase(get(), get()) }
    factory { GetCafeEventPageUseCase(get()) }
    factory { GetCafeCastPageUseCase(get(), get()) }
    factory { GetCafeCastListPageUseCase(get()) }
    factory { GetCafeManagementUseCase(get(), get()) }
    factory { GetCheckInGuestFeedUseCase(get(), get()) }
    factory { GetCheckInUserFeedUseCase(get(), get(), get()) }
    factory { CreateVisitUseCase(get(), get()) }
    factory { CreateReviewUseCase(get(), get(), get()) }
    factory { CreateCastClaimUseCase(get(), get()) }
    factory { CreateCafeEventUseCase(get()) }
    factory { CreateCafeNoticeUseCase(get()) }
    factory { CreateHomeBannerUseCase(get(), get(), get()) }
    factory { CreateInquiryUseCase(get(), get()) }
    factory { CreateCafeOwnerClaimUseCase(get(), get()) }
    factory { CreateCafeRegistrationClaimUseCase(get(), get()) }
    factory { ApproveCafeOwnerClaimUseCase(get(), get()) }
    factory { ApproveCafeRegistrationClaimUseCase(get(), get()) }
    factory { DeleteCafeEventUseCase(get()) }
    factory { DeleteCafeNoticeUseCase(get()) }
    factory { DeleteCafeMenuGoodsUseCase(get()) }
    factory { DeleteCastUseCase(get(), get(), get()) }
    factory { DismissReviewPromptUseCase(get(), get()) }
    factory { GetExploreFeedUseCase(get(), get()) }
    factory { GetExploreCafePageUseCase(get()) }
    factory { GetExploreCastPageUseCase(get()) }
    factory { GetCafeDetailUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetCafeNoticePageUseCase(get()) }
    factory { GetCafeReviewPageUseCase(get(), get(), get(), get()) }
    factory { GetCastDetailUseCase(get(), get(), get(), get()) }
    factory { GetFanManagementDataUseCase(get(), get(), get()) }
    factory { GetMainNavigationUseCase(get()) }
    factory { GetMyCastClaimStatusUseCase(get(), get()) }
    factory { GetMyRequestableCastPageUseCase(get(), get()) }
    factory { GetMyInfoUseCase(get(), get(), get(), get(), get()) }
    factory { GetNotificationFeedUseCase(get(), get()) }
    factory { GetRankingFeedUseCase(get(), get(), get()) }
    factory { GetScheduleManagementDataUseCase(get(), get()) }
    factory { GetSignUpCafeListUseCase(get()) }
    factory { GetPendingCastClaimsForCafeUseCase(get(), get()) }
    factory { GetPendingCafeOwnerClaimsUseCase(get(), get()) }
    factory { GetPendingCafeRegistrationClaimsUseCase(get(), get()) }
    factory { MarkNotificationReadUseCase(get(), get()) }
    factory { ObserveCurrentUserUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ShouldShowReviewPromptUseCase(get(), get()) }
    factory { ToggleFollowCastUseCase(get(), get()) }
    factory { ToggleFavoriteCafeUseCase(get(), get()) }
    factory { UpdateCafeInfoUseCase(get()) }
    factory { UpdateCafeEventUseCase(get()) }
    factory { UpdateCafeNoticeUseCase(get()) }
    factory { UpdateCastScheduleUseCase(get(), get()) }
    factory { ApproveCastClaimUseCase(get(), get()) }
    factory { RejectCastClaimUseCase(get(), get()) }
    factory { RejectCafeOwnerClaimUseCase(get(), get()) }
    factory { RejectCafeRegistrationClaimUseCase(get(), get()) }
    factory { UpsertCastUseCase(get(), get(), get()) }
    factory { UpsertCafeMenuGoodsUseCase(get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    eventModule,
    useCaseModule
)
