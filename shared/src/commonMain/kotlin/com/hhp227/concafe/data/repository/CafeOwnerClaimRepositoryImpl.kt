package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeOwnerClaimRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CafeOwnerClaimRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val cafeDataSource: CafeDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : CafeOwnerClaimRepository {
    override suspend fun createCafeOwnerClaim(userId: String, cafeId: String): PendingCafeOwnerClaimPreview {
        val user = authDataSource.findUserById(userId) ?: throw NoSuchElementException("user not found")
        val cafe = cafeDataSource.cafes.firstOrNull { it.id == cafeId } ?: throw NoSuchElementException("cafe not found")

        if (cafeDataSource.ownedCafeIdsByUser[userId].orEmpty().contains(cafeId)) {
            throw IllegalArgumentException("이미 운영 중인 카페입니다.")
        }

        val claims = cafeDataSource.pendingCafeClaimsByUser.getOrPut(userId) { mutableListOf() }
        val existingPending = claims.firstOrNull { it.cafeId == cafeId && it.status == "승인 대기 중" }
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
        claims.add(0, claim)
        try {
            firestoreSyncDataSource.pushCafeOwnerClaim(
                requesterUserId = userId,
                claim = claim,
                location = "${cafe.region.city} ${cafe.region.address}",
                imageUrl = cafe.thumbnailImage
            )
        } catch (e: Exception) {
            claims.removeAll { existing -> existing.claimId == claim.claimId }
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
        val resolved = resolveClaim(claimId)
        val claims = cafeDataSource.pendingCafeClaimsByUser[resolved.requesterUserId]
            ?: throw NoSuchElementException("claim not found")
        claims.removeAll { it.claimId == claimId }

        val ownedCafeIds = cafeDataSource.ownedCafeIdsByUser.getOrPut(resolved.requesterUserId) { mutableListOf() }
        if (!ownedCafeIds.contains(resolved.cafeId)) {
            ownedCafeIds.add(resolved.cafeId)
        }

        val currentUser = authDataSource.findUserById(resolved.requesterUserId)
        if (currentUser != null && currentUser.role != UserRole.ADMIN) {
            authDataSource.replaceUser(currentUser.copy(role = UserRole.CAFE_OWNER))
        }
        return resolved
    }

    override suspend fun rejectCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview {
        val resolved = resolveClaim(claimId)
        val claims = cafeDataSource.pendingCafeClaimsByUser[resolved.requesterUserId]
            ?: throw NoSuchElementException("claim not found")
        val claimIndex = claims.indexOfFirst { it.claimId == claimId }
        if (claimIndex == -1) {
            throw NoSuchElementException("claim not found")
        }
        val current = claims[claimIndex]
        claims[claimIndex] = current.copy(status = "반려", message = current.message)
        return resolved
    }

    private fun resolveClaim(claimId: String): PendingCafeOwnerClaimPreview {
        return cafeDataSource.pendingCafeClaimsByUser
            .flatMap { (userId, claims) ->
                claims.mapNotNull { claim ->
                    if (claim.claimId != claimId) return@mapNotNull null
                    val user = authDataSource.findUserById(userId) ?: return@mapNotNull null
                    val cafe = cafeDataSource.cafes.firstOrNull { it.id == claim.cafeId } ?: return@mapNotNull null
                    PendingCafeOwnerClaimPreview(
                        claimId = claim.claimId,
                        requesterUserId = userId,
                        requesterNickname = user.nickname,
                        cafeId = claim.cafeId,
                        cafeName = claim.cafeName,
                        location = "${cafe.region.city} ${cafe.region.address}",
                        requestedAt = claim.requestedAt,
                        message = claim.message,
                        imageUrl = cafe.thumbnailImage
                    )
                }
            }
            .firstOrNull()
            ?: throw NoSuchElementException("claim not found")
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
