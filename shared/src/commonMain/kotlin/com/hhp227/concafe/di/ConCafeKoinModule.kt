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
import com.hhp227.concafe.data.source.BannerRemoteDataSource
import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.CastClaimRemoteDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.data.source.InquiryRemoteDataSource
import com.hhp227.concafe.data.source.MyInfoRemoteDataSource
import com.hhp227.concafe.data.source.NetworkStatusDataSource
import com.hhp227.concafe.data.source.NoticeRemoteDataSource
import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.data.source.PlatformNetworkStatusDataSource
import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.data.source.ReviewRemoteDataSource
import com.hhp227.concafe.data.source.VisitRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConfig
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.event.publisher.*
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimStatus
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.model.Stamp
import com.hhp227.concafe.domain.model.Visit
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
    single<BannerRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : BannerRemoteDataSource {
            override suspend fun fetchHomeBanners(): List<HomeBanner> {
                return dataSource.fetchHomeBanners()
            }
        }
    }
    single<CafeRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : CafeRemoteDataSource {
            override suspend fun searchCafesRemote(
                query: String?,
                country: String?,
                city: String?,
                sort: CafeSort,
                cursor: String?,
                pageSize: Int
            ) = dataSource.searchCafesRemote(query, country, city, sort, cursor, pageSize)

            override suspend fun refreshCafeDetail(cafeId: String) = dataSource.refreshCafeDetail(cafeId)

            override suspend fun fetchCafeDetail(cafeId: String): CafeDetail {
                return dataSource.fetchCafeDetailRemote(cafeId) ?: throw NoSuchElementException("cafe detail not found")
            }

            override suspend fun fetchCafeById(cafeId: String): Cafe? {
                return dataSource.fetchCafeByIdRemote(cafeId)
            }

            override suspend fun fetchAllCafes(): List<Cafe> {
                return dataSource.fetchAllCafesRemote()
            }

            override suspend fun updateCafeInfoRemote(update: CafeInfoUpdate) =
                dataSource.updateCafeInfoRemote(update)

            override suspend fun upsertCafeMenuGoodsRemote(update: CafeMenuGoodsUpsert) =
                dataSource.upsertCafeMenuGoodsRemote(update)

            override suspend fun deleteCafeMenuGoodsRemote(cafeId: String, itemId: String) =
                dataSource.deleteCafeMenuGoodsRemote(cafeId, itemId)

            override suspend fun refreshFavoriteCafeIds(userId: String) = dataSource.refreshFavoriteCafeIds(userId)

            override suspend fun fetchFavoriteCafeIds(userId: String): List<String> {
                return dataSource.fetchFavoriteCafeIdsRemote(userId)
            }

            override suspend fun favoriteCafeRemote(userId: String, cafeId: String) =
                dataSource.favoriteCafeRemote(userId, cafeId)

            override suspend fun unfavoriteCafeRemote(userId: String, cafeId: String) =
                dataSource.unfavoriteCafeRemote(userId, cafeId)

            override suspend fun refreshCafeReviews(cafeId: String) = dataSource.refreshCafeReviews(cafeId)

            override suspend fun refreshCafeManagementData(userId: String) =
                dataSource.refreshCafeManagementData(userId)

            override suspend fun fetchOwnedCafeIds(userId: String): Set<String> {
                return dataSource.fetchOwnedCafeIdsRemote(userId)
            }

            override suspend fun fetchPendingCafeOwnerClaims(userId: String): List<CafeManagementData.PendingClaimSummary> {
                return dataSource.fetchPendingCafeOwnerClaimsRemote(userId)
            }

            override suspend fun fetchPendingCafeRegistrationClaims(userId: String): List<CafeRegistrationClaim> {
                return dataSource.fetchPendingCafeRegistrationClaimsRemote(userId)
            }

            override suspend fun fetchCafeTodayCheckInCount(cafeId: String): Int {
                return dataSource.fetchCafeTodayCheckInCountRemote(cafeId)
            }

            override suspend fun fetchCafeTodayReviewCount(cafeId: String): Int {
                return dataSource.fetchCafeTodayReviewCountRemote(cafeId)
            }

            override suspend fun fetchCafeCheckInCount(cafeId: String): Int {
                return dataSource.fetchCafeCheckInCountRemote(cafeId)
            }

            override suspend fun fetchCafeHomeBannerPreview(cafeId: String): CafeDashboardData.HomeBannerPreview? {
                return dataSource.fetchCafeHomeBannerPreviewRemote(cafeId)
            }

            override suspend fun fetchNoticeCount(cafeId: String): Int {
                return dataSource.fetchNoticeCountRemote(cafeId)
            }
        }
    }
    single<CastRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : CastRemoteDataSource {
            override suspend fun searchCastsRemote(
                query: String?,
                country: String?,
                city: String?,
                sort: CastSort,
                cursor: String?,
                pageSize: Int
            ) = dataSource.searchCastsRemote(query, country, city, sort, cursor, pageSize)

            override suspend fun getHomePopularCastPageRemote(cursor: String?, pageSize: Int) =
                dataSource.getHomePopularCastPageRemote(cursor, pageSize)

            override suspend fun fetchBirthdayCastsRemote(month: Int, dayOfMonth: Int, limit: Int) =
                dataSource.fetchBirthdayCastsRemote(month, dayOfMonth, limit)

            override suspend fun refreshCastDetailRemote(castId: String) = dataSource.refreshCastDetailRemote(castId)

            override suspend fun fetchCastDetail(castId: String): CastDetail {
                dataSource.refreshCastDetailRemote(castId)
                return dataSource.fetchCastDetailRemote(castId) ?: throw NoSuchElementException("cast detail not found")
            }

            override suspend fun getCafeCastPageRemote(cafeId: String, cursor: String?, pageSize: Int) =
                dataSource.getCafeCastPageRemote(cafeId, cursor, pageSize)

            override suspend fun upsertCastRemote(update: CastUpsert) =
                dataSource.upsertCastRemote(update)

            override suspend fun deleteCastRemote(castId: String) = dataSource.deleteCastRemote(castId)

            override suspend fun refreshCastSchedulesRemote(castId: String, fromDate: String, toDate: String) =
                dataSource.refreshCastSchedulesRemote(castId, fromDate, toDate)

            override suspend fun fetchCastSchedules(
                castId: String,
                fromDate: String,
                toDate: String
            ): List<CastSchedule> {
                return dataSource.fetchCastSchedulesRemote(castId, fromDate, toDate)
            }

            override suspend fun fetchCastScheduleStatuses(
                castId: String,
                fromDate: String,
                toDate: String
            ): Map<String, CastScheduleStatus> {
                return dataSource.fetchCastScheduleStatusesRemote(castId, fromDate, toDate)
            }

            override suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String) =
                dataSource.getWorkingCastIdsByCafeAndDate(cafeId, date)

            override suspend fun updateCastScheduleRemote(update: CastScheduleUpdate) =
                dataSource.updateCastScheduleRemote(update)

            override suspend fun refreshFollowedCastIds(userId: String) = dataSource.refreshFollowedCastIds(userId)

            override suspend fun fetchFollowedCastIds(userId: String): List<String> {
                return dataSource.fetchFollowedCastIdsRemote(userId)
            }

            override suspend fun getFollowedCastsRemote(userId: String) = dataSource.getFollowedCastsRemote(userId)

            override suspend fun refreshCastByLinkedUserId(userId: String) = dataSource.refreshCastByLinkedUserId(userId)

            override suspend fun fetchCastByLinkedUserId(userId: String): Cast? {
                return dataSource.refreshCastByLinkedUserId(userId)
            }

            override suspend fun refreshCafeCastsRemote(cafeId: String) = dataSource.refreshCafeCastsRemote(cafeId)

            override suspend fun fetchCafeCasts(cafeId: String): List<Cast> {
                return dataSource.fetchCafeCastsByCafeIdRemote(cafeId)
            }

            override suspend fun fetchCastsByIds(castIds: List<String>): List<Cast> {
                return dataSource.fetchCastsByIdsRemote(castIds)
            }

            override suspend fun fetchAllCasts(): List<Cast> {
                return dataSource.fetchAllCastsRemote()
            }

            override suspend fun fetchAffiliatedCafeId(userId: String): String? {
                return dataSource.fetchAffiliatedCafeIdRemote(userId)
            }

            override suspend fun setAffiliatedCafeId(userId: String, cafeId: String) {
                dataSource.setAffiliatedCafeIdRemote(userId, cafeId)
            }

            override suspend fun clearAffiliatedCafeId(userId: String) {
                dataSource.clearAffiliatedCafeIdRemote(userId)
            }

            override suspend fun followCastRemote(userId: String, castId: String) = dataSource.followCastRemote(userId, castId)

            override suspend fun unfollowCastRemote(userId: String, castId: String) =
                dataSource.unfollowCastRemote(userId, castId)

            override suspend fun refreshFollowerUserIds(castId: String) = dataSource.refreshFollowerUserIds(castId)

            override suspend fun fetchFollowerUserIds(castId: String): List<String> {
                return dataSource.fetchFollowerUserIdsRemote(castId)
            }

            override suspend fun getCastFollowerSnapshots(castId: String) = dataSource.getCastFollowerSnapshots(castId)
        }
    }
    single<CastClaimRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : CastClaimRemoteDataSource {
            override suspend fun refreshCastClaimsForUser(userId: String) = dataSource.refreshCastClaimsForUser(userId)

            override suspend fun fetchCastClaimsForUser(userId: String): List<CastClaim> {
                return dataSource.fetchCastClaimsForUserRemote(userId)
            }

            override suspend fun refreshCastClaimsForCafe(cafeId: String) = dataSource.refreshCastClaimsForCafe(cafeId)

            override suspend fun fetchCastClaimsForCafe(cafeId: String): List<CastClaim> {
                return dataSource.fetchCastClaimsForCafeRemote(cafeId)
            }

            override suspend fun fetchAllCastClaims(): List<CastClaim> {
                return dataSource.fetchAllCastClaimsRemote()
            }

            override suspend fun hasCastClaimCafeSyncChanged(cafeId: String) =
                dataSource.hasCastClaimCafeSyncChanged(cafeId)

            override suspend fun createCastClaimRemote(
                userId: String,
                cafeId: String,
                castId: String,
                message: String?
            ) = dataSource.createCastClaimRemote(userId, cafeId, castId, message)

            override suspend fun updateCastClaimStatusRemote(
                claimId: String,
                reviewedBy: String,
                status: CastClaimStatus
            ) = dataSource.updateCastClaimStatusRemote(claimId, reviewedBy, status)
        }
    }
    single<InquiryRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : InquiryRemoteDataSource {
            override suspend fun createInquiry(
                userId: String,
                userNickname: String,
                input: InquiryCreate
            ) = dataSource.createInquiryRemote(userId, userNickname, input)

            override suspend fun fetchInquiryPage(cursor: String?, pageSize: Int) =
                dataSource.getInquiryPageRemote(cursor, pageSize)
        }
    }
    single<NoticeRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : NoticeRemoteDataSource {
            override suspend fun fetchRecentNotices(limit: Int) = dataSource.getRecentNoticesRemote(limit)

            override suspend fun fetchCafeNoticePage(
                cafeId: String,
                query: String,
                cursor: String?,
                pageSize: Int
            ) = dataSource.getCafeNoticePageRemote(cafeId, query, cursor, pageSize)

            override suspend fun fetchCafeEventPage(
                cafeId: String,
                query: String,
                cursor: String?,
                pageSize: Int
            ) = dataSource.getCafeEventPageRemote(cafeId, query, cursor, pageSize)

            override suspend fun createCafeNotice(input: CafeNoticeCreate) =
                dataSource.createCafeNoticeRemote(input)

            override suspend fun createCafeEvent(input: CafeEventCreate) =
                dataSource.createCafeEventRemote(input)

            override suspend fun updateCafeNotice(input: CafeNoticeUpdate) =
                dataSource.updateCafeNoticeRemote(input)

            override suspend fun updateCafeEvent(input: CafeEventUpdate) =
                dataSource.updateCafeEventRemote(input)

            override suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String {
                dataSource.deleteCafeNoticeRemote(cafeId, noticeId)
                return noticeId
            }

            override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
                dataSource.deleteCafeEventRemote(cafeId, eventId)
                return eventId
            }
        }
    }
    single<RankingDataSource> { get<FirestoreConCafeDataSource>() }
    single<ReviewRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : ReviewRemoteDataSource {
            override suspend fun fetchCafeReviews(cafeId: String, cursor: String?, pageSize: Int) =
                dataSource.getCafeReviewsPageRemote(cafeId, cursor, pageSize)

            override suspend fun fetchReview(reviewId: String) = dataSource.getReviewRemote(reviewId)

            override suspend fun fetchRecentTaggedReviews(cafeId: String, castId: String, limit: Int) =
                dataSource.getRecentTaggedReviews(cafeId, castId, limit)

            override suspend fun createReview(
                userId: String,
                cafeId: String,
                visitId: String,
                rating: Float,
                content: String,
                imageUrls: List<String>,
                taggedCastIds: List<String>
            ) = dataSource.createReviewRemote(userId, cafeId, visitId, rating, content, imageUrls, taggedCastIds)

            override suspend fun updateReview(
                reviewId: String,
                requesterId: String,
                rating: Float,
                content: String,
                imageUrls: List<String>,
                taggedCastIds: List<String>
            ) = dataSource.updateReviewRemote(reviewId, requesterId, rating, content, imageUrls, taggedCastIds)

            override suspend fun hasReviewForVisit(visitId: String) = dataSource.hasReviewForVisitRemote(visitId)

            override suspend fun likeReview(reviewId: String) = dataSource.likeReviewRemote(reviewId)

            override suspend fun deleteReview(reviewId: String, requesterId: String) =
                dataSource.deleteReviewRemote(reviewId, requesterId)
        }
    }
    single<VisitRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : VisitRemoteDataSource {
            override suspend fun verifyVisit(
                cafeId: String,
                latitude: Double,
                longitude: Double
            ) = dataSource.verifyVisitRemote(cafeId, latitude, longitude)

            override suspend fun createVisit(
                userId: String,
                cafeId: String,
                visitedAt: String,
                memo: String?,
                latitude: Double,
                longitude: Double
            ): Visit {
                return dataSource.createVisitRemote(
                    userId = userId,
                    cafeId = cafeId,
                    visitedAt = visitedAt,
                    memo = memo,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            override suspend fun updateVisit(
                visitId: String,
                userId: String,
                visitedAt: String,
                memo: String?
            ) = dataSource.updateVisitRemote(visitId, userId, visitedAt, memo)

            override suspend fun deleteVisit(visitId: String, userId: String) {
                dataSource.deleteVisitRemote(visitId, userId)
            }

            override suspend fun fetchVisits(
                userId: String,
                cursor: String?,
                pageSize: Int
            ): PagedResult<Visit> {
                return dataSource.fetchVisitsByUserPageRemote(userId, cursor, pageSize)
            }

            override suspend fun fetchVerifiedVisitUserIdsByCafe(cafeId: String): Set<String> {
                return dataSource.getVerifiedVisitUserIdsByCafe(cafeId)
            }

            override suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean {
                return dataSource.hasVerifiedVisitAtCafe(userId, cafeId)
            }

            override suspend fun fetchStamps(userId: String): List<Stamp> {
                return dataSource.fetchStampsByUserRemote(userId)
            }

            override suspend fun fetchVisitCountByCafe(cafeId: String): Int {
                return dataSource.fetchVisitCountByCafeRemote(cafeId)
            }
        }
    }
    single<MyInfoRemoteDataSource> {
        val dataSource = get<FirestoreConCafeDataSource>()
        object : MyInfoRemoteDataSource {
            override suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean {
                return dataSource.isReviewPromptDismissedRemote(userId, visitId)
            }

            override suspend fun dismissReviewPrompt(userId: String, visitId: String) {
                dataSource.dismissReviewPromptRemote(userId, visitId)
            }
        }
    }
    single<NotificationDataSource> { get<FirestoreConCafeDataSource>() }
    single<FirestoreSyncDataSource> { get<FirestoreConCafeDataSource>() }
    single<NetworkStatusDataSource> { PlatformNetworkStatusDataSource() }
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
    single<VisitRepository> { VisitRepositoryImpl(get()) }
    single<ReviewRepository> { ReviewRepositoryImpl(get(), get()) }
    single<NoticeRepository> { NoticeRepositoryImpl(get()) }
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
    factory { DeleteReviewUseCase(get(), get(), get()) }
    factory { UpdateReviewUseCase(get(), get()) }
    factory { GetReviewUseCase(get()) }
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
    factory { SignOutUseCase(get(), get()) }
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
