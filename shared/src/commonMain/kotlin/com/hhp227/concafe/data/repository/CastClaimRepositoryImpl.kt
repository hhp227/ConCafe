package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastClaimDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimCandidate
import com.hhp227.concafe.domain.model.CastClaimStatus
import com.hhp227.concafe.domain.model.MyCastClaimStatus
import com.hhp227.concafe.domain.model.PendingCastClaimPreview
import com.hhp227.concafe.domain.repository.CastClaimRepository
import kotlinx.datetime.Clock

class CastClaimRepositoryImpl(
    private val castClaimDataSource: CastClaimDataSource,
    private val castDataSource: CastDataSource,
    private val authDataSource: AuthDataSource,
    private val cafeDataSource: CafeDataSource,
    private val pagingDataSource: PagingDataSource
) : CastClaimRepository {
    private fun resolveAffiliatedCafeId(userId: String, linkedCast: Cast?): String? {
        val mappedAffiliatedCafeId = castDataSource.affiliatedCafeIdByUser[userId]

        if (!mappedAffiliatedCafeId.isNullOrBlank()) {
            return mappedAffiliatedCafeId
        } else if (linkedCast != null) {
            return linkedCast.cafeId
        } else {
            val requestableCafeIds = castDataSource.casts
                .asSequence()
                .filter { cast -> cast.linkedUserId == null }
                .map { cast -> cast.cafeId }
                .distinct()
                .toList()

            return if (requestableCafeIds.size == 1) {
                requestableCafeIds.first()
            } else {
                null
            }
        }
    }

    override suspend fun getAffiliatedCafeId(userId: String): String? {
        val linkedCast = castDataSource.casts.firstOrNull { it.linkedUserId == userId }
        return resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)
    }

    override suspend fun getMyCastClaimStatus(userId: String): MyCastClaimStatus {
        val linkedCast = castDataSource.casts.firstOrNull { it.linkedUserId == userId }
        val affiliatedCafeId = resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)
        val affiliatedCafe = affiliatedCafeId?.let { cafeId ->
            cafeDataSource.cafes.firstOrNull { it.id == cafeId }
        }
        val userClaims = castClaimDataSource.castClaims
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
                castDataSource.casts.any { it.cafeId == affiliatedCafeId && it.linkedUserId == null }
        )
    }

    override suspend fun getMyRequestableCastPage(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CastClaimCandidate> {
        val linkedCast = castDataSource.casts.firstOrNull { it.linkedUserId == userId }
        val affiliatedCafeId = resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)
        if (linkedCast != null || affiliatedCafeId == null) {
            return PagedResult(emptyList(), nextCursor = null, hasNext = false)
        }
        val items = castDataSource.casts
            .filter { it.cafeId == affiliatedCafeId && it.linkedUserId == null }
            .sortedBy { it.name }
            .map { CastClaimCandidate(it.id, it.name) }
        return pagingDataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun getPendingCastClaimsForCafe(cafeId: String): List<PendingCastClaimPreview> {
        return castClaimDataSource.castClaims
            .filter { it.cafeId == cafeId && it.status == CastClaimStatus.PENDING }
            .sortedByDescending { it.createdAt }
            .mapNotNull { claim ->
                val requester = authDataSource.findUserById(claim.userId) ?: return@mapNotNull null
                val cast = castDataSource.casts.firstOrNull { it.id == claim.castId } ?: return@mapNotNull null
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
        val cast = castDataSource.casts.firstOrNull { it.id == castId && it.cafeId == cafeId }
            ?: throw NoSuchElementException("cast not found")
        if (cast.linkedUserId != null) {
            throw IllegalArgumentException("이미 연결된 캐스트 프로필입니다.")
        }
        val existingPending = castClaimDataSource.castClaims.firstOrNull {
            it.userId == userId && it.cafeId == cafeId && it.status == CastClaimStatus.PENDING
        }
        if (existingPending != null) {
            throw IllegalArgumentException("이미 승인 대기 중인 요청이 있습니다.")
        }

        val claim = CastClaim(
            id = nextEntityId("cast-claim"),
            userId = userId,
            cafeId = cafeId,
            castId = castId,
            status = CastClaimStatus.PENDING,
            message = message?.takeIf { it.isNotBlank() },
            createdAt = nowIsoUtc(),
            createdAtLabel = "방금 전"
        )
        castClaimDataSource.castClaims.add(0, claim)
        castDataSource.affiliatedCafeIdByUser[userId] = cafeId
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
        val index = castClaimDataSource.castClaims.indexOfFirst { it.id == claimId }
        if (index == -1) {
            throw NoSuchElementException("claim not found")
        }
        val current = castClaimDataSource.castClaims[index]
        val updated = current.copy(
            status = status,
            reviewedBy = reviewedBy,
            reviewedAt = nowIsoUtc()
        )
        castClaimDataSource.castClaims[index] = updated

        if (status == CastClaimStatus.APPROVED) {
            val casts = castDataSource.casts as MutableList<Cast>
            val castIndex = casts.indexOfFirst { it.id == current.castId }
            if (castIndex != -1) {
                val cast = casts[castIndex]
                casts[castIndex] = cast.copy(linkedUserId = current.userId)
            }
        }
        return updated
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
