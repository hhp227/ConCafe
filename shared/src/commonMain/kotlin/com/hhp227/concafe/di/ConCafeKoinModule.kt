package com.hhp227.concafe.di

import com.hhp227.concafe.data.repository.FakeAuthRepository
import com.hhp227.concafe.data.repository.FakeBannerRepository
import com.hhp227.concafe.data.repository.FakeCafeDashboardRepository
import com.hhp227.concafe.data.repository.FakeCafeManagementRepository
import com.hhp227.concafe.data.repository.FakeCafeRepository
import com.hhp227.concafe.data.repository.FakeCastRepository
import com.hhp227.concafe.data.repository.FakeNoticeRepository
import com.hhp227.concafe.data.repository.FakeNotificationRepository
import com.hhp227.concafe.data.repository.FakeRankingRepository
import com.hhp227.concafe.data.repository.FakeReviewRepository
import com.hhp227.concafe.data.repository.FakeUserRepository
import com.hhp227.concafe.data.repository.FakeVisitRepository
import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CafeDashboardRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.NoticeRepository
import com.hhp227.concafe.domain.repository.NotificationRepository
import com.hhp227.concafe.domain.repository.RankingRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository
import com.hhp227.concafe.domain.repository.VisitRepository
import com.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetFanManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import com.hhp227.concafe.domain.usecase.CreateVisitUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import com.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import com.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import com.hhp227.concafe.domain.usecase.GetScheduleManagementDataUseCase
import com.hhp227.concafe.domain.usecase.GetSignUpCafeListUseCase
import com.hhp227.concafe.domain.usecase.MarkNotificationReadUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeCastVersionUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastVersionUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignUpUseCase
import com.hhp227.concafe.domain.usecase.SignOutUseCase
import com.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeInfoUseCase
import com.hhp227.concafe.domain.usecase.UpsertCastUseCase
import com.hhp227.concafe.domain.usecase.UpsertCafeMenuGoodsUseCase
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
    single<CafeRepository> { FakeCafeRepository(get()) }
    single<CastRepository> { FakeCastRepository(get()) }
    single<VisitRepository> { FakeVisitRepository(get()) }
    single<ReviewRepository> { FakeReviewRepository(get()) }
    single<NoticeRepository> { FakeNoticeRepository(get()) }
    single<RankingRepository> { FakeRankingRepository(get()) }
    single<NotificationRepository> { FakeNotificationRepository(get()) }
}

val useCaseModule = module {
    factory { GetHomeFeedUseCase(get(), get(), get(), get()) }
    factory { GetCafeDashboardUseCase(get(), get()) }
    factory { GetCafeCastPageUseCase(get(), get()) }
    factory { GetCafeCastListPageUseCase(get()) }
    factory { GetCafeManagementUseCase(get(), get()) }
    factory { GetCheckInGuestFeedUseCase(get(), get()) }
    factory { GetCheckInUserFeedUseCase(get(), get(), get()) }
    factory { CreateVisitUseCase(get(), get()) }
    factory { DeleteCafeMenuGoodsUseCase(get()) }
    factory { GetExploreFeedUseCase(get(), get()) }
    factory { GetCafeDetailUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetCastDetailUseCase(get(), get(), get(), get()) }
    factory { GetFanManagementDataUseCase(get(), get(), get()) }
    factory { GetMainNavigationUseCase(get()) }
    factory { GetMyInfoUseCase(get(), get(), get(), get(), get()) }
    factory { GetNotificationFeedUseCase(get(), get()) }
    factory { GetRankingFeedUseCase(get(), get(), get()) }
    factory { GetScheduleManagementDataUseCase(get(), get()) }
    factory { GetSignUpCafeListUseCase(get()) }
    factory { MarkNotificationReadUseCase(get(), get()) }
    factory { ObserveCafeDetailUseCase(get()) }
    factory { ObserveCafeCastVersionUseCase(get()) }
    factory { ObserveCastVersionUseCase(get()) }
    factory { ObserveCurrentUserUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ToggleFollowCastUseCase(get(), get()) }
    factory { ToggleFavoriteCafeUseCase(get(), get()) }
    factory { UpdateCafeInfoUseCase(get()) }
    factory { UpsertCastUseCase(get(), get(), get()) }
    factory { UpsertCafeMenuGoodsUseCase(get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    useCaseModule
)
