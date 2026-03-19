package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.common.PagedResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimCandidate
import com.hhp227.concafe.domain.event.CastClaimEvent
import com.hhp227.concafe.domain.model.CastClaimStatus
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.MyCastClaimStatus
import com.hhp227.concafe.domain.model.PendingCastClaimPreview
import com.hhp227.concafe.domain.repository.CastClaimRepository

class FakeCastClaimRepository(
    private val dataSource: ConCafeDataSource
) : CastClaimRepository {
    override suspend fun getAffiliatedCafeId(userId: String): String? {
        return dataSource.affiliatedCafeIdByUser[userId]
            ?: dataSource.casts.firstOrNull { it.linkedUserId == userId }?.cafeId
    }

    override suspend fun getMyCastClaimStatus(userId: String): MyCastClaimStatus {
        val linkedCast = dataSource.casts.firstOrNull { it.linkedUserId == userId }
        val affiliatedCafeId = dataSource.affiliatedCafeIdByUser[userId] ?: linkedCast?.cafeId
        val affiliatedCafe = affiliatedCafeId?.let { cafeId ->
            dataSource.cafes.firstOrNull { it.id == cafeId }
        }
        val userClaims = dataSource.castClaims
            .filter { it.userId == userId && (affiliatedCafeId == null || it.cafeId == affiliatedCafeId) }
            .sortedByDescending { it.createdAt }
        return MyCastClaimStatus(
            userId = userId,
            affiliatedCafeId = affiliatedCafeId,
            affiliatedCafeName = affiliatedCafe?.name,
            linkedCastId = linkedCast?.id,
            linkedCastName = linkedCast?.name,
            pendingClaim = userClaims.firstOrNull { it.status == CastClaimStatus.PENDING },
            latestRejectedClaim = userClaims.firstOrNull { it.status == CastClaimStatus.REJECTED },
            hasRequestableCasts = linkedCast == null &&
                affiliatedCafeId != null &&
                dataSource.casts.any { it.cafeId == affiliatedCafeId && it.linkedUserId == null }
        )
    }

    override suspend fun getMyRequestableCastPage(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CastClaimCandidate> {
        val linkedCast = dataSource.casts.firstOrNull { it.linkedUserId == userId }
        val affiliatedCafeId = dataSource.affiliatedCafeIdByUser[userId] ?: linkedCast?.cafeId
        if (linkedCast != null || affiliatedCafeId == null) {
            return PagedResult(emptyList(), nextCursor = null, hasNext = false)
        }
        val items = dataSource.casts
            .filter { it.cafeId == affiliatedCafeId && it.linkedUserId == null }
            .sortedBy { it.name }
            .map { CastClaimCandidate(it.id, it.name) }
        return dataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun getPendingCastClaimsForCafe(cafeId: String): List<PendingCastClaimPreview> {
        return dataSource.castClaims
            .filter { it.cafeId == cafeId && it.status == CastClaimStatus.PENDING }
            .sortedByDescending { it.createdAt }
            .mapNotNull { claim ->
                val requester = dataSource.users.firstOrNull { it.id == claim.userId } ?: return@mapNotNull null
                val cast = dataSource.casts.firstOrNull { it.id == claim.castId } ?: return@mapNotNull null
                PendingCastClaimPreview(
                    claimId = claim.id,
                    requesterUserId = requester.id,
                    requesterNickname = requester.nickname,
                    castId = cast.id,
                    castName = cast.name,
                    requestedAtLabel = claim.createdAtLabel,
                    message = claim.message
                )
            }
    }

    override suspend fun createCastClaim(userId: String, cafeId: String, castId: String, message: String?): CastClaim {
        val cast = dataSource.casts.firstOrNull { it.id == castId && it.cafeId == cafeId }
            ?: throw NoSuchElementException("cast not found")
        if (cast.linkedUserId != null) {
            throw IllegalArgumentException("이미 연결된 캐스트 프로필입니다.")
        }
        val existingPending = dataSource.castClaims.firstOrNull {
            it.userId == userId && it.cafeId == cafeId && it.status == CastClaimStatus.PENDING
        }
        if (existingPending != null) {
            throw IllegalArgumentException("이미 승인 대기 중인 요청이 있습니다.")
        }

        val claim = CastClaim(
            id = "cast-claim-${dataSource.castClaims.size + 1}",
            userId = userId,
            cafeId = cafeId,
            castId = castId,
            status = CastClaimStatus.PENDING,
            message = message?.takeIf { it.isNotBlank() },
            createdAt = "2026-03-13T09:00:00Z",
            createdAtLabel = "방금 전"
        )
        dataSource.castClaims.add(0, claim)
        dataSource.affiliatedCafeIdByUser[userId] = cafeId
        return claim
    }

    override suspend fun approveCastClaim(claimId: String, reviewedBy: String): CastClaim {
        return updateCastClaimStatus(claimId, reviewedBy, CastClaimStatus.APPROVED)
    }

    override suspend fun rejectCastClaim(claimId: String, reviewedBy: String): CastClaim {
        return updateCastClaimStatus(claimId, reviewedBy, CastClaimStatus.REJECTED)
    }

    private fun updateCastClaimStatus(
        claimId: String,
        reviewedBy: String,
        status: CastClaimStatus
    ): CastClaim {
        val index = dataSource.castClaims.indexOfFirst { it.id == claimId }
        if (index == -1) {
            throw NoSuchElementException("claim not found")
        }
        val current = dataSource.castClaims[index]
        val updated = current.copy(
            status = status,
            reviewedBy = reviewedBy,
            reviewedAt = "2026-03-13T09:30:00Z"
        )
        dataSource.castClaims[index] = updated

        if (status == CastClaimStatus.APPROVED) {
            val casts = dataSource.casts as MutableList<Cast>
            val castIndex = casts.indexOfFirst { it.id == current.castId }
            if (castIndex != -1) {
                val cast = casts[castIndex]
                casts[castIndex] = cast.copy(linkedUserId = current.userId)
            }
        }
        return updated
    }
}
