package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.CastClaimRemoteDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimCandidate
import com.hhp227.concafe.domain.model.CastClaimStatus
import com.hhp227.concafe.domain.model.MyCastClaimStatus
import com.hhp227.concafe.domain.model.PendingCastClaimPreview
import com.hhp227.concafe.domain.repository.CastClaimRepository

class CastClaimRepositoryImpl(
    private val castClaimRemoteDataSource: CastClaimRemoteDataSource,
    private val castRemoteDataSource: CastRemoteDataSource,
    private val cafeRemoteDataSource: CafeRemoteDataSource
) : CastClaimRepository {
    private suspend fun resolveAffiliatedCafeId(userId: String, linkedCast: Cast?): String? {
        val mappedAffiliatedCafeId = castRemoteDataSource.fetchAffiliatedCafeId(userId)
        val userClaims = castClaimRemoteDataSource.fetchCastClaimsForUser(userId)
        val latestPendingCafeId = userClaims
            .filter { claim -> claim.status == CastClaimStatus.PENDING }
            .maxByOrNull { claim -> claim.createdAt }
            ?.cafeId
        val latestClaimCafeId = userClaims
            .maxByOrNull { claim -> claim.createdAt }
            ?.cafeId

        if (!mappedAffiliatedCafeId.isNullOrBlank()) {
            return mappedAffiliatedCafeId
        } else if (linkedCast != null) {
            return linkedCast.cafeId
        } else if (!latestPendingCafeId.isNullOrBlank()) {
            return latestPendingCafeId
        } else if (!latestClaimCafeId.isNullOrBlank()) {
            return latestClaimCafeId
        } else {
            val requestableCafeIds = castRemoteDataSource.fetchAllCasts()
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
        val linkedCast = castRemoteDataSource.refreshCastByLinkedUserId(userId)
        return resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)
    }

    override suspend fun getMyCastClaimStatus(userId: String): MyCastClaimStatus {
        val linkedCast = castRemoteDataSource.refreshCastByLinkedUserId(userId)
        val affiliatedCafeId = resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)
        val affiliatedCafe = affiliatedCafeId?.let { cafeId -> cafeRemoteDataSource.fetchCafeById(cafeId) }
        val userClaims = castClaimRemoteDataSource.fetchCastClaimsForUser(userId)
            .filter { claim -> affiliatedCafeId == null || claim.cafeId == affiliatedCafeId }
            .sortedByDescending { claim -> claim.createdAt }
        val hasRequestableCasts = if (linkedCast == null && affiliatedCafeId != null) {
            castRemoteDataSource.fetchCafeCasts(affiliatedCafeId)
                .any { cast -> cast.linkedUserId == null }
        } else {
            false
        }

        return MyCastClaimStatus(
            userId = userId,
            affiliatedCafeId = affiliatedCafeId,
            affiliatedCafeName = affiliatedCafe?.name,
            linkedCastId = linkedCast?.id,
            linkedCastName = linkedCast?.name,
            pendingClaim = userClaims.firstOrNull { claim -> claim.status == CastClaimStatus.PENDING },
            latestRejectedClaim = userClaims.firstOrNull { claim -> claim.status == CastClaimStatus.REJECTED },
            hasRequestableCasts = hasRequestableCasts
        )
    }

    override suspend fun getMyRequestableCastPage(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CastClaimCandidate> {
        val linkedCast = castRemoteDataSource.refreshCastByLinkedUserId(userId)
        val affiliatedCafeId = resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)

        if (linkedCast != null || affiliatedCafeId == null) {
            return PagedResult(emptyList(), nextCursor = null, hasNext = false)
        } else {
            val items = castRemoteDataSource.fetchCafeCasts(affiliatedCafeId)
                .filter { cast -> cast.linkedUserId == null }
                .sortedBy { cast -> cast.name }
                .map { cast -> CastClaimCandidate(cast.id, cast.name) }
            return toPaged(items = items, cursor = cursor, pageSize = pageSize)
        }
    }

    override suspend fun getPendingCastClaimsForCafe(cafeId: String): List<PendingCastClaimPreview> {
        val castsById = castRemoteDataSource.fetchCafeCasts(cafeId)
            .associateBy { cast -> cast.id }
        return castClaimRemoteDataSource.fetchCastClaimsForCafe(cafeId)
            .filter { claim -> claim.status == CastClaimStatus.PENDING }
            .sortedByDescending { claim -> claim.createdAt }
            .map { claim ->
                val cast = castsById[claim.castId]
                val requesterNickname = claim.requesterNickname
                    ?: "알 수 없음"
                PendingCastClaimPreview(
                    claimId = claim.id,
                    requesterUserId = claim.userId,
                    requesterNickname = requesterNickname,
                    castId = claim.castId,
                    castName = cast?.name ?: claim.castName.ifBlank { claim.castId },
                    requestedAtLabel = claim.createdAtLabel,
                    message = claim.message
                )
            }
    }

    override suspend fun createCastClaim(userId: String, cafeId: String, castId: String, message: String?): CastClaim {
        val cast = castRemoteDataSource.fetchCafeCasts(cafeId)
            .firstOrNull { candidate -> candidate.id == castId }
            ?: throw NoSuchElementException("cast not found")
        if (cast.linkedUserId != null) {
            throw IllegalArgumentException("이미 연결된 캐스트 프로필입니다.")
        } else {
            val existingPending = castClaimRemoteDataSource.fetchCastClaimsForUser(userId)
                .firstOrNull { claim ->
                    claim.userId == userId &&
                        claim.cafeId == cafeId &&
                        claim.status == CastClaimStatus.PENDING
                }
            if (existingPending != null) {
                throw IllegalArgumentException("이미 승인 대기 중인 요청이 있습니다.")
            } else {
                return castClaimRemoteDataSource.createCastClaimRemote(
                    userId = userId,
                    cafeId = cafeId,
                    castId = castId,
                    message = message
                )
            }
        }
    }

    private fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val startIndex = cursor?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val safeStartIndex = startIndex.coerceAtMost(items.size)
        val endIndex = (safeStartIndex + safePageSize).coerceAtMost(items.size)
        val pagedItems = items.subList(safeStartIndex, endIndex)
        val hasNext = endIndex < items.size
        val nextCursor = if (hasNext) endIndex.toString() else null

        return PagedResult(
            items = pagedItems,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }

    override suspend fun approveCastClaim(claimId: String, reviewedBy: String): CastClaim {
        return castClaimRemoteDataSource.updateCastClaimStatusRemote(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = CastClaimStatus.APPROVED
        )
    }

    override suspend fun rejectCastClaim(claimId: String, reviewedBy: String): CastClaim {
        return castClaimRemoteDataSource.updateCastClaimStatusRemote(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = CastClaimStatus.REJECTED
        )
    }
}
