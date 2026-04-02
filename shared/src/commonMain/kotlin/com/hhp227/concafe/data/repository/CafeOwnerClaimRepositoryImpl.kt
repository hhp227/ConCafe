package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.repository.CafeOwnerClaimRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CafeOwnerClaimRepositoryImpl(
    private val cafeRemoteDataSource: CafeRemoteDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : CafeOwnerClaimRepository {
    override suspend fun createCafeOwnerClaim(userId: String, cafeId: String): PendingCafeOwnerClaimPreview {
        val user = firestoreSyncDataSource.fetchUser(userId) ?: throw NoSuchElementException("user not found")
        val cafe = cafeRemoteDataSource.fetchCafeById(cafeId) ?: throw NoSuchElementException("cafe not found")
        val ownedCafeIds = cafeRemoteDataSource.fetchOwnedCafeIds(userId)
        val pendingClaims = cafeRemoteDataSource.fetchPendingCafeOwnerClaims(userId)

        if (ownedCafeIds.contains(cafeId)) {
            throw IllegalArgumentException("이미 운영 중인 카페입니다.")
        }

        val existingPending = pendingClaims.firstOrNull { it.cafeId == cafeId && it.status == "승인 대기 중" }
        if (existingPending != null) {
            throw IllegalArgumentException("이미 승인 대기 중인 카페 신청입니다.")
        }

        val claim = CafeManagementData.PendingClaimSummary(
            claimId = nextEntityId("cafe-claim"),
            cafeId = cafeId,
            cafeName = cafe.name,
            requestedAt = todayDotText(),
            status = "승인 대기 중",
            message = "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
        )
        try {
            firestoreSyncDataSource.pushCafeOwnerClaim(
                requesterUserId = userId,
                claim = claim,
                location = "${cafe.region.city} ${cafe.region.address}",
                imageUrl = cafe.thumbnailImage
            )
        } catch (e: Exception) {
            throw e
        }
        return PendingCafeOwnerClaimPreview(
            claimId = claim.claimId,
            requesterUserId = userId,
            requesterNickname = user.nickname,
            cafeId = cafe.id,
            cafeName = cafe.name,
            location = "${cafe.region.city} ${cafe.region.address}",
            requestedAt = claim.requestedAt,
            message = claim.message,
            imageUrl = cafe.thumbnailImage
        )
    }

    override suspend fun getPendingCafeOwnerClaims(): List<PendingCafeOwnerClaimPreview> {
        return firestoreSyncDataSource.fetchPendingCafeOwnerClaimsForAdmin()
    }

    override suspend fun approveCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview {
        return firestoreSyncDataSource.approveCafeOwnerClaimForAdmin(
            claimId = claimId,
            reviewedBy = reviewedBy
        )
    }

    override suspend fun rejectCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview {
        return firestoreSyncDataSource.rejectCafeOwnerClaimForAdmin(
            claimId = claimId,
            reviewedBy = reviewedBy
        )
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun todayDotText(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return "${date.year}.$month.$day"
}
