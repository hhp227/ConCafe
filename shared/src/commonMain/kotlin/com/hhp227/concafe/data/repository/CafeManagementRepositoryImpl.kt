package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.NoticeDataSource
import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeManagementRepository

class CafeManagementRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val cafeDataSource: CafeDataSource,
    private val castDataSource: CastDataSource,
    private val noticeDataSource: NoticeDataSource
) : CafeManagementRepository {
    override suspend fun getCafeManagementData(userId: String): CafeManagementData {
        val currentUser = authDataSource.findUserById(userId)
        val manageableCafes = if (currentUser?.role == UserRole.ADMIN) {
            cafeDataSource.cafes
        } else {
            cafeDataSource.cafes.filter { cafeDataSource.ownedCafeIdsByUser[userId].orEmpty().contains(it.id) }
        }

        val ownedCafes = manageableCafes
            .map { cafe ->
                val cafeCasts = castDataSource.casts.filter { it.cafeId == cafe.id }
                val cafeNotices = noticeDataSource.notices.filter { it.cafeId == cafe.id }

                CafeManagementData.OwnedCafeSummary(
                    id = cafe.id,
                    name = cafe.name,
                    city = cafe.region.city,
                    isApproved = cafe.approved,
                    todayVisitors = cafeDataSource.cafeCheckInCountById[cafe.id] ?: 0,
                    todayCheckIns = (cafeDataSource.cafeCheckInCountById[cafe.id] ?: 0) / 4,
                    todayReviews = (cafe.reviewCount / 50).coerceAtLeast(0),
                    rating = cafe.ratingAvg,
                    castCount = cafeCasts.size,
                    noticeCount = cafeNotices.size,
                    externalLinkCount = 3
                )
            }

        val searchableCafes = cafeDataSource.cafes.map { cafe ->
            CafeManagementData.SearchableCafeSummary(
                id = cafe.id,
                name = cafe.name,
                location = "${cafe.region.city} ${cafe.region.address}"
            )
        }

        val pendingCafeOwnerClaims = cafeDataSource.pendingCafeClaimsByUser[userId].orEmpty()
        val pendingCafeRegistrationClaims = cafeDataSource.pendingCafeRegistrationClaimsByUser[userId]
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
