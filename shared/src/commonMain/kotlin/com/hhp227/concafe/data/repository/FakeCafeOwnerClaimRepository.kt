package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeOwnerClaimRepository

class FakeCafeOwnerClaimRepository(
    private val dataSource: ConCafeDataSource
) : CafeOwnerClaimRepository {
    override suspend fun createCafeOwnerClaim(userId: String, cafeId: String): PendingCafeOwnerClaimPreview {
        val user = dataSource.users.firstOrNull { it.id == userId } ?: throw NoSuchElementException("user not found")
        val cafe = dataSource.cafes.firstOrNull { it.id == cafeId } ?: throw NoSuchElementException("cafe not found")

        if (dataSource.ownedCafeIdsByUser[userId].orEmpty().contains(cafeId)) {
            throw IllegalArgumentException("이미 운영 중인 카페입니다.")
        }

        val claims = dataSource.pendingCafeClaimsByUser.getOrPut(userId) { mutableListOf() }
        val existingPending = claims.firstOrNull { it.cafeId == cafeId && it.status == "승인 대기 중" }
        if (existingPending != null) {
            throw IllegalArgumentException("이미 승인 대기 중인 카페 신청입니다.")
        }

        val claim = com.hhp227.concafe.domain.model.CafeManagementData.PendingClaimSummary(
            claimId = "cafe-claim-${dataSource.pendingCafeClaimsByUser.values.sumOf { it.size } + 1}",
            cafeId = cafeId,
            cafeName = cafe.name,
            requestedAt = "2026.03.14",
            status = "승인 대기 중",
            message = "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
        )
        claims.add(0, claim)
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
        return dataSource.pendingCafeClaimsByUser
            .flatMap { (userId, claims) ->
                claims.filter { it.status == "승인 대기 중" }.mapNotNull { claim ->
                    val user = dataSource.users.firstOrNull { it.id == userId } ?: return@mapNotNull null
                    val cafe = dataSource.cafes.firstOrNull { it.id == claim.cafeId } ?: return@mapNotNull null
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
            .sortedByDescending { it.requestedAt }
    }

    override suspend fun approveCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview {
        val resolved = resolveClaim(claimId)
        val claims = dataSource.pendingCafeClaimsByUser[resolved.requesterUserId]
            ?: throw NoSuchElementException("claim not found")
        claims.removeAll { it.claimId == claimId }

        val ownedCafeIds = dataSource.ownedCafeIdsByUser.getOrPut(resolved.requesterUserId) { mutableListOf() }
        if (!ownedCafeIds.contains(resolved.cafeId)) {
            ownedCafeIds.add(resolved.cafeId)
        }

        val userIndex = dataSource.users.indexOfFirst { it.id == resolved.requesterUserId }

        if (userIndex != -1) {
            val currentUser = dataSource.users[userIndex]
            if (currentUser.role != UserRole.ADMIN) {
                dataSource.users[userIndex] = currentUser.copy(role = UserRole.CAFE_OWNER)
            }
        }
        return resolved
    }

    override suspend fun rejectCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview {
        val resolved = resolveClaim(claimId)
        val claims = dataSource.pendingCafeClaimsByUser[resolved.requesterUserId]
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
        return dataSource.pendingCafeClaimsByUser
            .flatMap { (userId, claims) ->
                claims.mapNotNull { claim ->
                    if (claim.claimId != claimId) return@mapNotNull null
                    val user = dataSource.users.firstOrNull { it.id == userId } ?: return@mapNotNull null
                    val cafe = dataSource.cafes.firstOrNull { it.id == claim.cafeId } ?: return@mapNotNull null
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
