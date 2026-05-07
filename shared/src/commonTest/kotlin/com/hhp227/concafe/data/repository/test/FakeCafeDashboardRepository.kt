package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.repository.CafeDashboardRepository

class FakeCafeDashboardRepository(
    private val dataSource: ConCafeDataSource
) : CafeDashboardRepository {
    override suspend fun getCafeDashboardData(cafeId: String, ownerUserId: String?): CafeDashboardData {
        if (ownerUserId != null && !dataSource.ownedCafeIdsByUser[ownerUserId].orEmpty().contains(cafeId)) {
            throw NoSuchElementException("cafe dashboard not found")
        }
        val cafe = dataSource.cafes.firstOrNull { it.id == cafeId }
            ?: throw NoSuchElementException("cafe dashboard not found")
        val castPreviews = dataSource.casts
            .filter { it.cafeId == cafeId }
            .map { cast ->
                CafeDashboardData.CastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = dataSource.onShiftCastIdsByCafeId[cafeId].orEmpty().contains(cast.id)
                )
            }
        val homeBannerPreview = dataSource.cafeHomeBannerPreviewByCafeId[cafeId]
            ?: throw NoSuchElementException("home banner preview not found")
        return CafeDashboardData(
            id = cafe.id,
            name = cafe.name,
            city = cafe.region.city,
            todayCheckIns = dataSource.cafeTodayCheckInCountById[cafe.id] ?: 0,
            followerCount = cafe.favoriteCount,
            rating = cafe.ratingAvg,
            castPreviews = castPreviews,
            homeBannerPreview = homeBannerPreview
        )
    }
}
