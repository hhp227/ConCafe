package com.hhp227.concafe.di

import com.hhp227.concafe.data.repository.*
import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.domain.event.publisher.*
import com.hhp227.concafe.domain.repository.*
import com.hhp227.concafe.domain.usecase.*
import org.koin.dsl.module

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
    single<StorageRepository> { FakeStorageRepository() }
    single<ImageCompressionRepository> { PlatformImageCompressionRepository() }
}

val eventModule = module {
    single<BannerEventPublisher> { BannerEventPublisher() }
    single<CafeDetailEventPublisher> { CafeDetailEventPublisher() }
    single<CafeRegistrationClaimEventPublisher> { CafeRegistrationClaimEventPublisher() }
    single<CastClaimEventPublisher> { CastClaimEventPublisher() }
    single<CastEventPublisher> { CastEventPublisher() }
    single<NoticeManagementEventPublisher> { NoticeManagementEventPublisher() }
    single<ReviewEventPublisher> { ReviewEventPublisher() }
    single<ScheduleManagementEventPublisher> { ScheduleManagementEventPublisher() }
}

val useCaseModule = module {
    factory { GetHomeFeedUseCase(get(), get(), get(), get()) }
    factory { GetHomeBannerManagementUseCase(get(), get()) }
    factory { GetCafeDashboardUseCase(get(), get()) }
    factory { GetCafeEventPageUseCase(get()) }
    factory { GetCafeCastPageUseCase(get(), get()) }
    factory { GetCafeCastListPageUseCase(get()) }
    factory { GetCafeManagementUseCase(get(), get()) }
    factory { GetCheckInGuestFeedUseCase(get(), get()) }
    factory { GetCheckInUserFeedUseCase(get(), get(), get()) }
    factory { CreateVisitUseCase(get(), get()) }
    factory { CreateReviewUseCase(get(), get(), get(), get()) }
    factory { CreateCastClaimUseCase(get(), get(), get()) }
    factory { CreateCafeEventUseCase(get(), get()) }
    factory { CreateCafeNoticeUseCase(get(), get()) }
    factory { CreateHomeBannerUseCase(get(), get(), get()) }
    factory { CreateInquiryUseCase(get(), get()) }
    factory { CreateCafeOwnerClaimUseCase(get(), get()) }
    factory { CreateCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { ApproveCafeOwnerClaimUseCase(get(), get()) }
    factory { ApproveCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { DeleteCafeEventUseCase(get(), get()) }
    factory { DeleteCafeNoticeUseCase(get(), get()) }
    factory { DeleteCafeMenuGoodsUseCase(get(), get()) }
    factory { DeleteCastUseCase(get(), get(), get(), get()) }
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
    factory { SignInWithSocialProviderUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { RequestPhoneVerificationCodeUseCase() }
    factory { VerifyPhoneVerificationCodeUseCase() }
    factory { ShouldShowReviewPromptUseCase(get(), get()) }
    factory { ToggleFollowCastUseCase(get(), get()) }
    factory { ToggleFavoriteCafeUseCase(get(), get()) }
    factory { UpdateCafeInfoUseCase(get(), get()) }
    factory { UpdateCafeEventUseCase(get(), get()) }
    factory { UpdateCafeNoticeUseCase(get(), get()) }
    factory { UpdateCastScheduleUseCase(get(), get(), get()) }
    factory { ApproveCastClaimUseCase(get(), get(), get()) }
    factory { RejectCastClaimUseCase(get(), get(), get()) }
    factory { RejectCafeOwnerClaimUseCase(get(), get()) }
    factory { RejectCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { UpsertCastUseCase(get(), get(), get(), get()) }
    factory { UpsertCafeMenuGoodsUseCase(get(), get()) }
    factory { UploadImageUseCase(get(), get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    eventModule,
    useCaseModule
)
