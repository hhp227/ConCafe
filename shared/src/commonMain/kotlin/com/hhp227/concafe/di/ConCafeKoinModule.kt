package com.hhp227.concafe.di

import com.hhp227.concafe.data.repository.*
import com.hhp227.concafe.data.source.CommunityPostRemoteDataSource
import com.hhp227.concafe.data.source.*
import com.hhp227.concafe.data.source.cache.DefaultRemoteMemoryCache
import com.hhp227.concafe.data.source.cache.RemoteMemoryCache
import com.hhp227.concafe.data.source.cache.withLocalCache
import com.hhp227.concafe.data.source.firestore.FirestoreAuthDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreBannerRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreCafeRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreCastClaimRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreCastRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConfig
import com.hhp227.concafe.data.source.firestore.FirestoreCommunityPostRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreInquiryRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreMyInfoRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreNoticeRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreNotificationDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreRankingDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreReportRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreReviewRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreUserBlockRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreVisitRemoteDataSource
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.domain.event.publisher.*
import com.hhp227.concafe.domain.repository.CommunityPostRepository
import com.hhp227.concafe.domain.repository.*
import com.hhp227.concafe.domain.usecase.*
import com.hhp227.concafe.domain.usecase.CreateCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.UpdateCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.GetCommunityPostPageUseCase
import org.koin.dsl.module

private const val FIRESTORE_PROJECT_ID = "concafe-5f7fd"

val dataSourceModule = module {
    single { FirestoreConfig(projectId = FIRESTORE_PROJECT_ID) }
    single<RemoteMemoryCache> { DefaultRemoteMemoryCache() }
    single<BannerRemoteDataSource> { FirestoreBannerRemoteDataSource(get(), get(), get()) }
    single<CafeRemoteDataSource> { FirestoreCafeRemoteDataSource(get(), get(), get()).withLocalCache(get()) }
    single<CastRemoteDataSource> { FirestoreCastRemoteDataSource(get(), get(), get()).withLocalCache(get()) }
    single<CastClaimRemoteDataSource> { FirestoreCastClaimRemoteDataSource(get(), get(), get()) }
    single<InquiryRemoteDataSource> { FirestoreInquiryRemoteDataSource(get(), get(), get()) }
    single<ReportRemoteDataSource> { FirestoreReportRemoteDataSource(get(), get(), get()) }
    single<UserBlockRemoteDataSource> { FirestoreUserBlockRemoteDataSource(get(), get(), get()) }
    single<NoticeRemoteDataSource> { FirestoreNoticeRemoteDataSource(get(), get(), get()).withLocalCache(get()) }
    single<AuthDataSource> { FirestoreAuthDataSource() }
    single<RankingDataSource> { FirestoreRankingDataSource(get(), get(), get()) }
    single<ReviewRemoteDataSource> { FirestoreReviewRemoteDataSource(get(), get(), get()) }
    single<VisitRemoteDataSource> { FirestoreVisitRemoteDataSource(get(), get(), get()) }
    single<MyInfoRemoteDataSource> { FirestoreMyInfoRemoteDataSource(get(), get(), get()) }
    single<NotificationDataSource> { FirestoreNotificationDataSource(get(), get(), get()) }
    single<FirestoreSyncDataSource> { FirestoreSyncRemoteDataSource(get(), get(), get()) }
    single<NetworkStatusDataSource> { PlatformNetworkStatusDataSource() }
    single<CommunityPostRemoteDataSource> { FirestoreCommunityPostRemoteDataSource(get(), get(), get()) }
    single<AppUpdateRemoteDataSource> { KtorAppUpdateRemoteDataSource(get()) }
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<AdminOperationsRepository> { AdminOperationsRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<BannerRepository> { BannerRepositoryImpl(get(), get()) }
    single<CafeDashboardRepository> { CafeDashboardRepositoryImpl(get(), get()) }
    single<CafeManagementRepository> { CafeManagementRepositoryImpl(get(), get(), get()) }
    single<CafeOwnerClaimRepository> { CafeOwnerClaimRepositoryImpl(get(), get()) }
    single<CafeRegistrationClaimRepository> { CafeRegistrationClaimRepositoryImpl(get(), get()) }
    single<CafeRepository> { CafeRepositoryImpl(get(), get()) }
    single<CastRepository> { CastRepositoryImpl(get(), get()) }
    single<CastClaimRepository> { CastClaimRepositoryImpl(get(), get(), get()) }
    single<InquiryRepository> { InquiryRepositoryImpl(get()) }
    single<ReportRepository> { ReportRepositoryImpl(get()) }
    single<UserBlockRepository> { UserBlockRepositoryImpl(get()) }
    single<VisitRepository> { VisitRepositoryImpl(get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get()) }
    single<NoticeRepository> { NoticeRepositoryImpl(get()) }
    single<RankingRepository> { RankingRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get(), get(), get()) }
    single<ImageCompressionRepository> { PlatformImageCompressionRepository() }
    single<NetworkStatusRepository> { DefaultNetworkStatusRepository(get()) }
    single<NativeAdRepository> { NativeAdRepositoryImpl(get()) }
    single<CommunityPostRepository> { CommunityPostRepositoryImpl(get()) }
    single<AppUpdateRepository> { AppUpdateRepositoryImpl(get()) }
}

val eventModule = module {
    single<BannerEventPublisher> { BannerEventPublisher() }
    single<CafeEventEventPublisher> { CafeEventEventPublisher() }
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
    single<CommunityPostEventPublisher> { CommunityPostEventPublisher() }
}

val useCaseModule = module {
    single { CafeReviewUserNicknameCache() }
    factory { GetNearbyCafePageUseCase(get()) }
    factory { GetPopularCastPageUseCase(get(), get()) }
    factory { GetHomeBannersUseCase(get()) }
    factory { GetBirthdayCastsUseCase(get()) }
    factory { GetRecentNoticesUseCase(get()) }
    factory { GetHomeCafeEventsUseCase(get(), get()) }
    factory { GetAdminOperationsMetricsUseCase(get(), get()) }
    factory { GetAdminInquiryPageUseCase(get(), get()) }
    factory { GetAdminReportPageUseCase(get(), get()) }
    factory { GetAdminUserPageUseCase(get(), get()) }
    factory { GetHomeBannerManagementUseCase(get(), get()) }
    factory { GetCafeDashboardUseCase(get(), get()) }
    factory { GetCafeEventPageUseCase(get()) }
    factory { GetCafeEventParticipantCastsUseCase(get()) }
    factory { GetCafeCastPageUseCase(get(), get()) }
    factory { GetCafeCastListPageUseCase(get()) }
    factory { GetCafeManagementUseCase(get(), get()) }
    factory { GetCheckInGuestFeedUseCase(get(), get()) }
    factory { GetCheckInMapCafePageUseCase(get()) }
    factory { GetCheckInUserFeedUseCase(get(), get(), get()) }
    factory { CreateVisitUseCase(get(), get(), get()) }
    factory { CreateReviewUseCase(get(), get(), get(), get()) }
    factory { CreateCastClaimUseCase(get(), get(), get()) }
    factory { CreateCafeEventUseCase(get(), get(), get()) }
    factory { CafeExternalLinkLocalUseCase(get<CafeExternalLinkLocalStore>()) }
    factory { CreateCafeNoticeUseCase(get(), get()) }
    factory { CreateHomeBannerUseCase(get(), get(), get()) }
    factory { CreateInquiryUseCase(get(), get()) }
    factory { CreateCommunityPostReportUseCase(get(), get()) }
    factory { CreateUserBlockUseCase(get(), get()) }
    factory { CreateCafeOwnerClaimUseCase(get(), get(), get()) }
    factory { CreateCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { ApproveCafeOwnerClaimUseCase(get(), get(), get()) }
    factory { ApproveCafeRegistrationClaimUseCase(get(), get(), get()) }
    factory { DeleteCafeEventUseCase(get(), get(), get()) }
    factory { DeleteCafeNoticeUseCase(get(), get()) }
    factory { DeleteCafeMenuGoodsUseCase(get(), get()) }
    factory { DeleteHomeBannerUseCase(get(), get(), get(), get()) }
    factory { DeleteCastUseCase(get(), get(), get(), get()) }
    factory { DeleteAccountUseCase(get()) }
    factory { DeleteReviewUseCase(get(), get(), get()) }
    factory { UpdateReviewUseCase(get(), get()) }
    factory { GetReviewUseCase(get()) }
    factory { ChangePasswordUseCase(get()) }
    factory { DismissReviewPromptUseCase(get(), get()) }
    factory { GetExploreFeedUseCase(get(), get()) }
    factory { GetExploreCafePageUseCase(get()) }
    factory { GetExploreCastPageUseCase(get()) }
    factory { GetCafeDetailUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { GetCafeMenuGoodsUseCase(get()) }
    factory { GetCafeNoticePageUseCase(get()) }
    factory { GetCafeReviewPageUseCase(get(), get(), get(), get()) }
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
    factory { GetCafeScheduleCalendarUseCase(get()) }
    factory { GetSignUpCafeListUseCase(get()) }
    factory { GetPendingCastClaimsForCafeUseCase(get(), get()) }
    factory { GetPendingCafeOwnerClaimsUseCase(get(), get()) }
    factory { GetPendingCafeRegistrationClaimsUseCase(get(), get()) }
    factory { MarkNotificationReadUseCase(get(), get()) }
    factory { RegisterPushTokenUseCase(get(), get()) }
    factory { SendFanAnnouncementUseCase(get()) }
    factory { UpdateNotificationSettingsUseCase(get(), get()) }
    factory { ObserveCurrentUserUseCase(get()) }
    factory { ObserveNetworkAlertStateUseCase(get()) }
    factory { RestoreSessionUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignInWithAppleIdTokenUseCase(get()) }
    factory { SignInWithGoogleIdTokenUseCase(get()) }
    factory { SignInWithKakaoIdTokenUseCase(get()) }
    factory { SignInWithSocialProviderUseCase(get()) }
    factory { RequestPasswordResetUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { CompleteSignUpForCurrentUserUseCase(get()) }
    factory { SignOutUseCase(get(), get()) }
    factory { RequestPhoneVerificationCodeUseCase() }
    factory { VerifyPhoneVerificationCodeUseCase() }
    factory { ShouldShowReviewPromptUseCase(get(), get()) }
    factory { ToggleFollowCastUseCase(get(), get(), get()) }
    factory { ToggleFavoriteCafeUseCase(get(), get(), get()) }
    factory { UpdateUserProfileUseCase(get(), get(), get()) }
    factory { UpdateCafeInfoUseCase(get(), get()) }
    factory { UpdateCafeSocialMediaUseCase(get()) }
    factory { UpdateCafeReservationUrlUseCase(get()) }
    factory { UpdateCafeTableCountsUseCase(get()) }
    factory { UpdateCafeEventUseCase(get(), get(), get()) }
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
    factory { LoadNativeAdUseCase(get()) }
    factory { ClearNativeAdUseCase(get()) }
    factory { CheckAppUpdateUseCase(get()) }
    factory { GetCommunityPostPageUseCase(get()) }
    factory { CreateCommunityPostUseCase(get(), get(), get()) }
    factory { UpdateCommunityPostUseCase(get(), get(), get()) }
    factory { GetCommunityPostUseCase(get()) }
    factory { IncrementCommunityPostViewCountUseCase(get()) }
    factory { DeleteCommunityPostUseCase(get(), get(), get()) }
    factory { CheckCommunityPostLikedUseCase(get(), get()) }
    factory { ToggleCommunityPostLikeUseCase(get(), get()) }
    factory { GetCommunityCommentsUseCase(get()) }
    factory { AddCommunityCommentUseCase(get(), get()) }
    factory { GetCommunityCommentPageUseCase(get()) }
    factory { UpdateCommunityCommentUseCase(get(), get()) }
    factory { DeleteCommunityCommentUseCase(get(), get()) }
    factory { GetCafeEventLikeStatusUseCase(get(), get()) }
    factory { ToggleCafeEventLikeUseCase(get(), get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    eventModule,
    useCaseModule
)
