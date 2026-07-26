package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeManagementRepository

class FakeCafeManagementRepository(
    private val dataSource: ConCafeDataSource
) : CafeManagementRepository {
    override suspend fun getOwnedCafes(userId: String): List<CafeManagementData.OwnedCafeSummary> {
        val currentUser = dataSource.users.firstOrNull { it.id == userId }
        val manageableCafes = if (currentUser?.role == UserRole.ADMIN) {
            dataSource.cafes
        } else {
            dataSource.cafes.filter { dataSource.ownedCafeIdsByUser[userId].orEmpty().contains(it.id) }
        }
        return manageableCafes.map { cafe ->
            val cafeCasts = dataSource.casts.filter { it.cafeId == cafe.id }
            val cafeNotices = dataSource.notices.filter { it.cafeId == cafe.id }
            return@map CafeManagementData.OwnedCafeSummary(
                id = cafe.id,
                name = cafe.name,
                city = cafe.region.city,
                isApproved = cafe.approved,
                todayVisitors = dataSource.cafeCheckInCountById[cafe.id] ?: 0,
                todayCheckIns = (dataSource.cafeCheckInCountById[cafe.id] ?: 0) / 4,
                todayReviews = (cafe.reviewCount / 50).coerceAtLeast(0),
                rating = cafe.ratingAvg,
                castCount = cafeCasts.size,
                noticeCount = cafeNotices.size,
                externalLinkCount = 3,
                thumbnailImage = cafe.thumbnailImage
            )
        }
    }

    override suspend fun getCafeManagementData(userId: String): CafeManagementData {
        val currentUser = dataSource.users.firstOrNull { it.id == userId }
        val manageableCafes = if (currentUser?.role == UserRole.ADMIN) {
            dataSource.cafes
        } else {
            dataSource.cafes.filter { dataSource.ownedCafeIdsByUser[userId].orEmpty().contains(it.id) }
        }
        val ownedCafes = manageableCafes
            .map { cafe ->
                val cafeCasts = dataSource.casts.filter { it.cafeId == cafe.id }
                val cafeNotices = dataSource.notices.filter { it.cafeId == cafe.id }

                CafeManagementData.OwnedCafeSummary(
                    id = cafe.id,
                    name = cafe.name,
                    city = cafe.region.city,
                    isApproved = cafe.approved,
                    todayVisitors = dataSource.cafeCheckInCountById[cafe.id] ?: 0,
                    todayCheckIns = (dataSource.cafeCheckInCountById[cafe.id] ?: 0) / 4,
                    todayReviews = (cafe.reviewCount / 50).coerceAtLeast(0),
                    rating = cafe.ratingAvg,
                    castCount = cafeCasts.size,
                    noticeCount = cafeNotices.size,
                    externalLinkCount = 3,
                    thumbnailImage = cafe.thumbnailImage
                )
            }

        val searchableCafes = dataSource.cafes.map { cafe ->
            CafeManagementData.SearchableCafeSummary(
                id = cafe.id,
                name = cafe.name,
                location = "${cafe.region.city} ${cafe.region.address}"
            )
        }

        val pendingCafeOwnerClaims = dataSource.pendingCafeClaimsByUser[userId].orEmpty()
        val pendingCafeRegistrationClaims = dataSource.pendingCafeRegistrationClaimsByUser[userId]
            .orEmpty()
            .map { claim ->
                CafeManagementData.PendingClaimSummary(
                    claimId = claim.claimId,
                    cafeId = "",
                    cafeName = claim.cafeName,
                    requestedAt = claim.requestedAt,
                    status = claim.status,
                    message = claim.message
                )
            }
        val pendingClaims = (pendingCafeOwnerClaims + pendingCafeRegistrationClaims)
            .sortedByDescending { it.requestedAt }

        return CafeManagementData(
            ownedCafes = ownedCafes,
            searchableCafes = searchableCafes,
            pendingClaims = pendingClaims
        )
    }
}
