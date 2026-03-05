package org.hhp227.concafe.di

import org.hhp227.concafe.data.repository.FakeAuthRepository
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
import org.hhp227.concafe.domain.repository.CafeRepository
import org.hhp227.concafe.domain.repository.CastRepository
import org.hhp227.concafe.domain.repository.NoticeRepository
import org.hhp227.concafe.domain.repository.NotificationRepository
import org.hhp227.concafe.domain.repository.RankingRepository
import org.hhp227.concafe.domain.repository.ReviewRepository
import org.hhp227.concafe.domain.repository.UserRepository
import org.hhp227.concafe.domain.repository.VisitRepository
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import org.koin.dsl.module

val dataSourceModule = module {
    single<ConCafeDataSource> { MockConCafeDataSource() }
}

val repositoryModule = module {
    single<AuthRepository> { FakeAuthRepository(get()) }
    single<UserRepository> { FakeUserRepository(get()) }
    single<CafeRepository> { FakeCafeRepository(get()) }
    single<CastRepository> { FakeCastRepository(get()) }
    single<VisitRepository> { FakeVisitRepository(get()) }
    single<ReviewRepository> { FakeReviewRepository(get()) }
    single<NoticeRepository> { FakeNoticeRepository(get()) }
    single<RankingRepository> { FakeRankingRepository(get()) }
    single<NotificationRepository> { FakeNotificationRepository(get()) }
}

val useCaseModule = module {
    factory { GetHomeFeedUseCase(get()) }
}

val concafeModules = listOf(
    dataSourceModule,
    repositoryModule,
    useCaseModule
)
