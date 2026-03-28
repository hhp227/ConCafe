package com.hhp227.concafe.di

import com.hhp227.concafe.data.repository.AuthRepositoryImpl
import com.hhp227.concafe.data.repository.AdminOperationsRepositoryImpl
import com.hhp227.concafe.data.repository.BannerRepositoryImpl
import com.hhp227.concafe.data.repository.CafeDashboardRepositoryImpl
import com.hhp227.concafe.data.repository.CafeManagementRepositoryImpl
import com.hhp227.concafe.data.repository.CafeOwnerClaimRepositoryImpl
import com.hhp227.concafe.data.repository.CafeRegistrationClaimRepositoryImpl
import com.hhp227.concafe.data.repository.CafeRepositoryImpl
import com.hhp227.concafe.data.repository.CastClaimRepositoryImpl
import com.hhp227.concafe.data.repository.CastRepositoryImpl
import com.hhp227.concafe.data.repository.DefaultNetworkStatusRepository
import com.hhp227.concafe.data.repository.InquiryRepositoryImpl
import com.hhp227.concafe.data.repository.NoticeRepositoryImpl
import com.hhp227.concafe.data.repository.NotificationRepositoryImpl
import com.hhp227.concafe.data.repository.PlatformImageCompressionRepository
import com.hhp227.concafe.data.repository.RankingRepositoryImpl
import com.hhp227.concafe.data.repository.ReviewRepositoryImpl
import com.hhp227.concafe.data.repository.StorageRepositoryImpl
import com.hhp227.concafe.data.repository.UserRepositoryImpl
import com.hhp227.concafe.data.repository.VisitRepositoryImpl
import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.BannerDataSource
import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastClaimDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.InquiryDataSource
import com.hhp227.concafe.data.source.NetworkStatusDataSource
import com.hhp227.concafe.data.source.NoticeDataSource
import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.PlatformNetworkStatusDataSource
import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.data.source.ReviewDataSource
import com.hhp227.concafe.data.source.SocialDataSource
import com.hhp227.concafe.data.source.VisitDataSource
import com.hhp227.concafe.data.source.MyInfoDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConfig
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.domain.event.publisher.*
import com.hhp227.concafe.domain.repository.*
import com.hhp227.concafe.domain.usecase.*
import org.koin.dsl.module

private const val FIRESTORE_PROJECT_ID = "concafe-5f7fd"

val dataSourceModule = module {
    single { FirestoreConfig(projectId = FIRESTORE_PROJECT_ID) }
    single {
        FirestoreConCafeDataSource(
            config = get(),
            restApi = get(),
            tokenProvider = get()
        )
    }
    single<AuthDataSource> { get<FirestoreConCafeDataSource>() }
    single<BannerDataSource> { get<FirestoreConCafeDataSource>() }
    single<CafeDataSource> { get<FirestoreConCafeDataSource>() }
    single<CastDataSource> { get<FirestoreConCafeDataSource>() }
    single<CastClaimDataSource> { get<FirestoreConCafeDataSource>() }
    single<InquiryDataSource> { get<FirestoreConCafeDataSource>() }
    single<NoticeDataSource> { get<FirestoreConCafeDataSource>() }
    single<PagingDataSource> { get<FirestoreConCafeDataSource>() }
    single<RankingDataSource> { get<FirestoreConCafeDataSource>() }
    single<ReviewDataSource> { get<FirestoreConCafeDataSource>() }
    single<SocialDataSource> { get<FirestoreConCafeDataSource>() }
    single<VisitDataSource> { get<FirestoreConCafeDataSource>() }
    single<MyInfoDataSource> { get<FirestoreConCafeDataSource>() }
    single<NotificationDataSource> { get<FirestoreConCafeDataSource>() }
    single<FirestoreSyncDataSource> { get<FirestoreConCafeDataSource>() }
    single<NetworkStatusDataSource> { PlatformNetworkStatusDataSource() }
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<AdminOperationsRepository> { AdminOperationsRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
    single<BannerRepository> { BannerRepositoryImpl(get(), get(), get()) }
    single<CafeDashboardRepository> { CafeDashboardRepositoryImpl(get(), get()) }
    single<CafeManagementRepository> { CafeManagementRepositoryImpl(get(), get(), get(), get(), get()) }
    single<CafeOwnerClaimRepository> { CafeOwnerClaimRepositoryImpl(get(), get(), get()) }
    single<CafeRegistrationClaimRepository> { CafeRegistrationClaimRepositoryImpl(get(), get(), get()) }
    single<CafeRepository> { CafeRepositoryImpl(get(), get(), get(), get()) }
    single<CastRepository> { CastRepositoryImpl(get(), get(), get(), get()) }
    single<CastClaimRepository> { CastClaimRepositoryImpl(get(), get(), get(), get(), get()) }
    single<InquiryRepository> { InquiryRepositoryImpl(get(), get()) }
    single<VisitRepository> { VisitRepositoryImpl(get(), get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get(), get()) }
    single<NoticeRepository> { NoticeRepositoryImpl(get(), get(), get()) }
    single<RankingRepository> { RankingRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get(), get(), get()) }
    single<ImageCompressionRepository> { PlatformImageCompressionRepository() }
    single<NetworkStatusRepository> { DefaultNetworkStatusRepository(get()) }
}

val eventModule = module {
    single<BannerEventPublisher> { BannerEventPublisher() }
    single<CafeDetailEventPublisher> { CafeDetailEventPublisher() }
    single<CafeOwnerClaimEventPublisher> { CafeOwnerClaimEventPublisher() }
    single<CafeRegistrationClaimEventPublisher> { CafeRegistrationClaimEventPublisher() }
    single<CastClaimEventPublisher> { CastClaimEventPublisher() }
    single<CastEventPublisher> { CastEventPublisher() }
    single<NoticeManagementEventPublisher> { NoticeManagementEventPublisher() }
    single<ReviewEventPublisher> { ReviewEventPublisher() }
    single<ScheduleManagementEventPublisher> { ScheduleManagementEventPublisher() }
    single<VisitEventPublisher> { VisitEventPublisher() }
    single<UserEventPublisher> { UserEventPublisher() }
}

val useCaseModule = module {
    factory { GetHomeFeedUseCase(get(), get(), get(), get()) }
    factory { GetAdminOperationsMetricsUseCase(get(), get()) }
    factory { GetAdminInquiryPageUseCase(get(), get()) }
    factory { GetHomeBannerManagementUseCase(get(), get()) }
    factory { GetCafeDashboardUseCase(get(), get()) }
    factory { GetCafeEventPageUseCase(get()) }
    factory { GetCafeCastPageUseCase(get(), get()) }
    factory { GetCafeCastListPageUseCase(get()) }
    factory { GetCafeManagementUseCase(get(), get()) }
    factory { GetCheckInGuestFeedUseCase(get(), get()) }
    factory { GetCheckInUserFeedUseCase(get(), get(), get()) }
    factory { CreateVisitUseCase(get(), get(), get()) }
    factory { CreateReviewUseCase(get(), get(), get(), get()) }
    factory { CreateCastClaimUseCase(get(), get(), get()) }
    factory { CreateCafeEventUseCase(get(), get()) }
    factory { CafeExternalLinkLocalUseCase(get<CafeExternalLinkLocalStore>()) }
    factory { CreateCafeNoticeUseCase(get(), get()) }
    factory { CreateHomeBannerUseCase(get(), get(), get()) }
    factory { CreateInquiryUseCase(get(), get()) }
    factory { CreateCafeOwnerClaimUseCase(get(), get(), get()) }
    factory { CreateCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { ApproveCafeOwnerClaimUseCase(get(), get(), get()) }
    factory { ApproveCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { DeleteCafeEventUseCase(get(), get()) }
    factory { DeleteCafeNoticeUseCase(get(), get()) }
    factory { DeleteCafeMenuGoodsUseCase(get(), get()) }
    factory { DeleteHomeBannerUseCase(get(), get(), get(), get()) }
    factory { DeleteCastUseCase(get(), get(), get(), get()) }
    factory { DeleteAccountUseCase(get()) }
    factory { ChangePasswordUseCase(get()) }
    factory { DismissReviewPromptUseCase(get(), get()) }
    factory { GetExploreFeedUseCase(get(), get()) }
    factory { GetExploreCafePageUseCase(get()) }
    factory { GetExploreCastPageUseCase(get()) }
    factory { GetCafeDetailUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetCafeNoticePageUseCase(get()) }
    factory { GetCafeReviewPageUseCase(get(), get(), get()) }
    factory { GetCastDetailUseCase(get(), get(), get(), get()) }
    factory { GetFanManagementDataUseCase(get(), get(), get()) }
    factory { GetMainNavigationUseCase(get()) }
    factory { GetMyCastClaimStatusUseCase(get(), get()) }
    factory { GetMyRequestableCastPageUseCase(get(), get()) }
    factory { GetMyInfoUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetNotificationFeedUseCase(get(), get()) }
    factory { GetNotificationSettingsUseCase(get(), get()) }
    factory { GetRankingFeedUseCase(get()) }
    factory { GetScheduleManagementDataUseCase(get(), get()) }
    factory { GetSignUpCafeListUseCase(get()) }
    factory { GetPendingCastClaimsForCafeUseCase(get(), get()) }
    factory { GetPendingCafeOwnerClaimsUseCase(get(), get()) }
    factory { GetPendingCafeRegistrationClaimsUseCase(get(), get()) }
    factory { MarkNotificationReadUseCase(get(), get()) }
    factory { RegisterPushTokenUseCase(get(), get()) }
    factory { UpdateNotificationSettingsUseCase(get(), get()) }
    factory { ObserveCurrentUserUseCase(get()) }
    factory { ObserveNetworkAlertStateUseCase(get()) }
    factory { RestoreSessionUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignInWithGoogleIdTokenUseCase(get()) }
    factory { SignInWithSocialProviderUseCase(get()) }
    factory { RequestPasswordResetUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { RequestPhoneVerificationCodeUseCase() }
    factory { VerifyPhoneVerificationCodeUseCase() }
    factory { ShouldShowReviewPromptUseCase(get(), get()) }
    factory { ToggleFollowCastUseCase(get(), get(), get()) }
    factory { ToggleFavoriteCafeUseCase(get(), get(), get()) }
    factory { UpdateUserProfileUseCase(get(), get(), get()) }
    factory { UpdateCafeInfoUseCase(get(), get()) }
    factory { UpdateCafeEventUseCase(get(), get()) }
    factory { UpdateCafeNoticeUseCase(get(), get()) }
    factory { UpdateHomeBannerUseCase(get(), get(), get(), get()) }
    factory { UpdateCastScheduleUseCase(get(), get(), get()) }
    factory { ApproveCastClaimUseCase(get(), get(), get()) }
    factory { RejectCastClaimUseCase(get(), get(), get()) }
    factory { RejectCafeOwnerClaimUseCase(get(), get(), get()) }
    factory { RejectCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { UpsertCastUseCase(get(), get(), get(), get()) }
    factory { UpsertCafeMenuGoodsUseCase(get(), get()) }
    factory { UploadImageUseCase(get(), get()) }
    factory { DeleteImageUseCase(get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    eventModule,
    useCaseModule
)
