package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.repository.CafeDashboardRepository

class CafeDashboardRepositoryImpl(
    private val cafeDataSource: CafeDataSource,
    private val castDataSource: CastDataSource
) : CafeDashboardRepository {
    override suspend fun getCafeDashboardData(cafeId: String, ownerUserId: String?): CafeDashboardData {
        if (ownerUserId != null && !cafeDataSource.ownedCafeIdsByUser[ownerUserId].orEmpty().contains(cafeId)) {
            throw NoSuchElementException("cafe dashboard not found")
        }

        val cafe = cafeDataSource.cafes.firstOrNull { it.id == cafeId }
            ?: throw NoSuchElementException("cafe dashboard not found")

        val castPreviews = castDataSource.casts
            .filter { it.cafeId == cafeId }
            .map { cast ->
                CafeDashboardData.CastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(cast.id)
                )
            }

        val homeBannerPreview = cafeDataSource.cafeHomeBannerPreviewByCafeId[cafeId]
            ?: throw NoSuchElementException("home banner preview not found")

        return CafeDashboardData(
            id = cafe.id,
            name = cafe.name,
            city = cafe.region.city,
            todayCheckIns = cafeDataSource.cafeTodayCheckInCountById[cafe.id] ?: 0,
            todayReviews = cafeDataSource.cafeTodayReviewCountById[cafe.id] ?: 0,
            rating = cafe.ratingAvg,
            castPreviews = castPreviews,
            homeBannerPreview = homeBannerPreview
        )
    }
}
