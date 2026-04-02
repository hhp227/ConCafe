package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeManagementRepository

class CafeManagementRepositoryImpl(
    private val cafeRemoteDataSource: CafeRemoteDataSource,
    private val castRemoteDataSource: CastRemoteDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : CafeManagementRepository {
    override suspend fun getOwnedCafes(userId: String): List<CafeManagementData.OwnedCafeSummary> {
        val currentUser = firestoreSyncDataSource.fetchUser(userId)
        val allCafes = cafeRemoteDataSource.fetchAllCafes()
        val ownedCafeIds = cafeRemoteDataSource.fetchOwnedCafeIds(userId)
        val manageableCafes = if (currentUser?.role == UserRole.ADMIN) {
            allCafes
        } else {
            allCafes.filter { ownedCafeIds.contains(it.id) }
        }
        return manageableCafes.map { cafe ->
            val cafeCasts = castRemoteDataSource.fetchCafeCasts(cafe.id)
            val noticeCount = cafeRemoteDataSource.fetchNoticeCount(cafe.id)
            val cafeCheckInCount = cafeRemoteDataSource.fetchCafeCheckInCount(cafe.id)

            CafeManagementData.OwnedCafeSummary(
                id = cafe.id,
                name = cafe.name,
                city = cafe.region.city,
                isApproved = cafe.approved,
                todayVisitors = cafeCheckInCount,
                todayCheckIns = cafeCheckInCount / 4,
                todayReviews = (cafe.reviewCount / 50).coerceAtLeast(0),
                rating = cafe.ratingAvg,
                castCount = cafeCasts.size,
                noticeCount = noticeCount,
                externalLinkCount = 3,
                thumbnailImage = cafe.thumbnailImage
            )
        }
    }

    override suspend fun getCafeManagementData(userId: String): CafeManagementData {
        val currentUser = firestoreSyncDataSource.fetchUser(userId)
        val allCafes = cafeRemoteDataSource.fetchAllCafes()
        val ownedCafeIds = cafeRemoteDataSource.fetchOwnedCafeIds(userId)
        val manageableCafes = if (currentUser?.role == UserRole.ADMIN) {
            allCafes
        } else {
            allCafes.filter { ownedCafeIds.contains(it.id) }
        }
        val ownedCafes = manageableCafes.map { cafe ->
            val cafeCasts = castRemoteDataSource.fetchCafeCasts(cafe.id)
            val noticeCount = cafeRemoteDataSource.fetchNoticeCount(cafe.id)
            val cafeCheckInCount = cafeRemoteDataSource.fetchCafeCheckInCount(cafe.id)

            CafeManagementData.OwnedCafeSummary(
                id = cafe.id,
                name = cafe.name,
                city = cafe.region.city,
                isApproved = cafe.approved,
                todayVisitors = cafeCheckInCount,
                todayCheckIns = cafeCheckInCount / 4,
                todayReviews = (cafe.reviewCount / 50).coerceAtLeast(0),
                rating = cafe.ratingAvg,
                castCount = cafeCasts.size,
                noticeCount = noticeCount,
                externalLinkCount = 3,
                thumbnailImage = cafe.thumbnailImage
            )
        }
        val searchableCafes = allCafes.map { cafe ->
            CafeManagementData.SearchableCafeSummary(
                id = cafe.id,
                name = cafe.name,
                location = "${cafe.region.city} ${cafe.region.address}"
            )
        }
        val pendingCafeOwnerClaims = cafeRemoteDataSource.fetchPendingCafeOwnerClaims(userId)
        val pendingCafeRegistrationClaims = cafeRemoteDataSource.fetchPendingCafeRegistrationClaims(userId)
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
