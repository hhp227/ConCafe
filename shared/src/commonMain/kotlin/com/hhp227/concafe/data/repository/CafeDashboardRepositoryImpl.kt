package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.repository.CafeDashboardRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CafeDashboardRepositoryImpl(
    private val cafeRemoteDataSource: CafeRemoteDataSource,
    private val castRemoteDataSource: CastRemoteDataSource
) : CafeDashboardRepository {
    override suspend fun getCafeDashboardData(cafeId: String, ownerUserId: String?): CafeDashboardData {
        if (ownerUserId != null) {
            val ownedCafeIds = cafeRemoteDataSource.fetchOwnedCafeIds(ownerUserId)
            if (!ownedCafeIds.contains(cafeId)) {
                throw NoSuchElementException("cafe dashboard not found")
            }
        }
        val cafe = cafeRemoteDataSource.fetchCafeById(cafeId)
        if (cafe == null) {
            throw NoSuchElementException("cafe dashboard not found")
        }

        val todayDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        val workingCastIds = castRemoteDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = todayDate)
        val castPreviews = castRemoteDataSource.fetchCafeCasts(cafeId)
            .filter { it.cafeId == cafeId }
            .map { cast ->
                CafeDashboardData.CastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = workingCastIds.contains(cast.id)
                )
            }
        val homeBannerPreview = cafeRemoteDataSource.fetchCafeHomeBannerPreview(cafeId)
            ?: CafeDashboardData.HomeBannerPreview(
                title = "홈 배너를 등록해보세요",
                period = "설정된 배너 없음",
                statusLabel = "미등록",
                imageUrl = null
            )
        return CafeDashboardData(
            id = cafe.id,
            name = cafe.name,
            city = cafe.region.city,
            todayCheckIns = cafeRemoteDataSource.fetchCafeTodayCheckInCount(cafe.id),
            todayReviews = cafeRemoteDataSource.fetchCafeTodayReviewCount(cafe.id),
            rating = cafe.ratingAvg,
            castPreviews = castPreviews,
            homeBannerPreview = homeBannerPreview,
            socialMedia = cafe.socialMedia,
            reservationUrl = cafe.reservationUrl,
            tableCounts = cafe.tableCounts
        )
    }
}
