package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.repository.CafeDashboardRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CafeDashboardRepositoryImpl(
    private val cafeDataSource: CafeDataSource,
    private val castDataSource: CastDataSource
) : CafeDashboardRepository {
    override suspend fun getCafeDashboardData(cafeId: String, ownerUserId: String?): CafeDashboardData {
        val firestoreDataSource = cafeDataSource as? FirestoreConCafeDataSource

        if (ownerUserId != null && !cafeDataSource.ownedCafeIdsByUser[ownerUserId].orEmpty().contains(cafeId)) {
            if (firestoreDataSource != null) {
                runCatching {
                    firestoreDataSource.refreshCafeManagementData(ownerUserId)
                }
            }
            if (!cafeDataSource.ownedCafeIdsByUser[ownerUserId].orEmpty().contains(cafeId)) {
                throw NoSuchElementException("cafe dashboard not found")
            }
        }

        var cafe = cafeDataSource.cafes.firstOrNull { it.id == cafeId }

        if (cafe == null && firestoreDataSource != null) {
            runCatching {
                firestoreDataSource.refreshCafeDetail(cafeId)
            }
            cafe = cafeDataSource.cafes.firstOrNull { it.id == cafeId }
        }
        if (cafe == null) {
            throw NoSuchElementException("cafe dashboard not found")
        }

        val todayDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        if (firestoreDataSource != null) {
            runCatching { firestoreDataSource.refreshCafeReviews(cafeId) }
        }

        val workingCastIds = if (firestoreDataSource != null) {
            runCatching {
                firestoreDataSource.getWorkingCastIdsByCafeAndDate(cafeId = cafeId, date = todayDate)
            }.getOrElse {
                cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty()
            }
        } else {
            cafeDataSource.onShiftCastIdsByCafeId[cafeId].orEmpty()
        }

        val castPreviews = castDataSource.casts
            .filter { it.cafeId == cafeId }
            .map { cast ->
                CafeDashboardData.CastPreview(
                    id = cast.id,
                    name = cast.name,
                    isOnShift = workingCastIds.contains(cast.id)
                )
            }
        val homeBannerPreview = cafeDataSource.cafeHomeBannerPreviewByCafeId[cafeId]
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
            todayCheckIns = cafeDataSource.cafeTodayCheckInCountById[cafe.id] ?: 0,
            todayReviews = cafeDataSource.cafeTodayReviewCountById[cafe.id] ?: 0,
            rating = cafe.ratingAvg,
            castPreviews = castPreviews,
            homeBannerPreview = homeBannerPreview
        )
    }
}
