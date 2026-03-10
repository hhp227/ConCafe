package org.hhp227.concafe.di

import org.hhp227.concafe.data.repository.FakeAuthRepository
import org.hhp227.concafe.data.repository.FakeBannerRepository
import org.hhp227.concafe.data.repository.FakeCafeDashboardRepository
import org.hhp227.concafe.data.repository.FakeCafeManagementRepository
import org.hhp227.concafe.data.repository.FakeCafeRepository
import org.hhp227.concafe.data.repository.FakeCastRepository
import org.hhp227.concafe.data.repository.FakeNoticeRepository
import org.hhp227.concafe.data.repository.FakeNotificationRepository
import org.hhp227.concafe.data.repository.FakeRankingRepository
import org.hhp227.concafe.data.repository.FakeReviewRepository
import org.hhp227.concafe.data.repository.FakeUserRepository
import org.hhp227.concafe.data.repository.FakeVisitRepository
import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.data.source.MockConCafeDataSource
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.BannerRepository
import org.hhp227.concafe.domain.repository.CafeDashboardRepository
import org.hhp227.concafe.domain.repository.CafeManagementRepository
import org.hhp227.concafe.domain.repository.CafeRepository
import org.hhp227.concafe.domain.repository.CastRepository
import org.hhp227.concafe.domain.repository.NoticeRepository
import org.hhp227.concafe.domain.repository.NotificationRepository
import org.hhp227.concafe.domain.repository.RankingRepository
import org.hhp227.concafe.domain.repository.ReviewRepository
import org.hhp227.concafe.domain.repository.UserRepository
import org.hhp227.concafe.domain.repository.VisitRepository
import org.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import org.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import org.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import org.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import org.hhp227.concafe.domain.usecase.CreateVisitUseCase
import org.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import org.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import org.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import org.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import org.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import org.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import org.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import org.hhp227.concafe.domain.usecase.MarkNotificationReadUseCase
import org.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import org.hhp227.concafe.domain.usecase.SignInUseCase
import org.hhp227.concafe.domain.usecase.SignUpUseCase
import org.hhp227.concafe.domain.usecase.SignOutUseCase
import org.hhp227.concafe.domain.usecase.ToggleFollowCastUseCase
import org.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase
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
    factory { GetCafeManagementUseCase(get(), get()) }
    factory { GetCheckInGuestFeedUseCase(get(), get()) }
    factory { GetCheckInUserFeedUseCase(get(), get(), get()) }
    factory { CreateVisitUseCase(get(), get()) }
    factory { GetExploreFeedUseCase(get(), get()) }
    factory { GetCafeDetailUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GetCastDetailUseCase(get(), get(), get(), get()) }
    factory { GetMainNavigationUseCase(get()) }
    factory { GetMyInfoUseCase(get(), get(), get(), get()) }
    factory { GetNotificationFeedUseCase(get(), get()) }
    factory { GetRankingFeedUseCase(get(), get(), get()) }
    factory { MarkNotificationReadUseCase(get(), get()) }
    factory { ObserveCurrentUserUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ToggleFollowCastUseCase(get(), get()) }
    factory { ToggleFavoriteCafeUseCase(get(), get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    useCaseModule
)
