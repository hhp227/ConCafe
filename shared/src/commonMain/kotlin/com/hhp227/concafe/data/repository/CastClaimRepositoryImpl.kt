package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastClaimDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
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
    private val lastCafeCastsRefreshEpochMillisByCafeId = mutableMapOf<String, Long>()
    private val lastUserClaimsRefreshEpochMillisByUserId = mutableMapOf<String, Long>()
    private val lastCafeClaimsRefreshEpochMillisByCafeId = mutableMapOf<String, Long>()

    private suspend fun refreshAffiliatedCafeCasts(affiliatedCafeId: String?) {
        if (!affiliatedCafeId.isNullOrBlank()) {
            val firestoreDataSource = castDataSource as? FirestoreConCafeDataSource

            if (firestoreDataSource != null) {
                val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
                val lastRefreshEpochMillis = lastCafeCastsRefreshEpochMillisByCafeId[affiliatedCafeId] ?: 0L

                if (nowEpochMillis - lastRefreshEpochMillis >= CAFE_CASTS_REFRESH_INTERVAL_MILLIS) {
                    firestoreDataSource.refreshCafeCastsRemote(affiliatedCafeId)
                    lastCafeCastsRefreshEpochMillisByCafeId[affiliatedCafeId] = nowEpochMillis
                }
            }
        }
    }

    private fun resolveAffiliatedCafeId(userId: String, linkedCast: Cast?): String? {
        val mappedAffiliatedCafeId = castDataSource.affiliatedCafeIdByUser[userId]
        val userClaims = castClaimDataSource.castClaims
            .asSequence()
            .filter { claim -> claim.userId == userId }
            .toList()
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
        refreshClaimsForUser(userId)
        val linkedCast = castDataSource.casts.firstOrNull { it.linkedUserId == userId }
        return resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)
    }

    private suspend fun refreshClaimsForUser(userId: String) {
        val firestoreDataSource = castClaimDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
            val lastRefreshEpochMillis = lastUserClaimsRefreshEpochMillisByUserId[userId] ?: 0L

            if (nowEpochMillis - lastRefreshEpochMillis >= USER_CLAIMS_REFRESH_INTERVAL_MILLIS) {
                firestoreDataSource.refreshCastClaimsForUser(userId)
                lastUserClaimsRefreshEpochMillisByUserId[userId] = nowEpochMillis
            }
        }
    }

    private suspend fun refreshClaimsForCafe(cafeId: String) {
        val firestoreDataSource = castClaimDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
            val lastRefreshEpochMillis = lastCafeClaimsRefreshEpochMillisByCafeId[cafeId] ?: 0L

            if (nowEpochMillis - lastRefreshEpochMillis >= CAFE_CLAIMS_REFRESH_INTERVAL_MILLIS) {
                val isChanged = try {
                    firestoreDataSource.hasCastClaimCafeSyncChanged(cafeId)
                } catch (_: Throwable) {
                    true
                }

                if (isChanged) {
                    firestoreDataSource.refreshCastClaimsForCafe(cafeId)
                    lastCafeClaimsRefreshEpochMillisByCafeId[cafeId] = nowEpochMillis
                } else {
                    lastCafeClaimsRefreshEpochMillisByCafeId[cafeId] = nowEpochMillis
                }
            }
        }
    }

    override suspend fun getMyCastClaimStatus(userId: String): MyCastClaimStatus {
        refreshClaimsForUser(userId)
        val linkedCast = castDataSource.casts.firstOrNull { it.linkedUserId == userId }
        val affiliatedCafeId = resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)

        refreshAffiliatedCafeCasts(affiliatedCafeId)
        if (affiliatedCafeId != null) {
            refreshClaimsForCafe(affiliatedCafeId)
        }

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
        refreshClaimsForUser(userId)
        val linkedCast = castDataSource.casts.firstOrNull { it.linkedUserId == userId }
        val affiliatedCafeId = resolveAffiliatedCafeId(userId = userId, linkedCast = linkedCast)

        refreshAffiliatedCafeCasts(affiliatedCafeId)
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
        refreshClaimsForCafe(cafeId)
        return castClaimDataSource.castClaims
            .filter { it.cafeId == cafeId && it.status == CastClaimStatus.PENDING }
            .sortedByDescending { it.createdAt }
            .map { claim ->
                val requester = authDataSource.findUserById(claim.userId)
                val cast = castDataSource.casts.firstOrNull { it.id == claim.castId }
                val requesterNickname = claim.requesterNickname
                    ?: requester?.nickname
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
        refreshClaimsForUser(userId)

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

        val firestoreDataSource = castClaimDataSource as? FirestoreConCafeDataSource
        return if (firestoreDataSource != null) {
            firestoreDataSource.createCastClaimRemote(
                userId = userId,
                cafeId = cafeId,
                castId = castId,
                message = message
            )
        } else {
            val requester = authDataSource.findUserById(userId)
            val claim = CastClaim(
                id = nextEntityId("cast-claim"),
                userId = userId,
                cafeId = cafeId,
                castId = castId,
                castName = cast.name,
                requesterNickname = requester?.nickname,
                requesterProfileImage = requester?.profileImage,
                status = CastClaimStatus.PENDING,
                message = message?.takeIf { it.isNotBlank() },
                createdAt = nowIsoUtc(),
                createdAtLabel = "방금 전"
            )
            castClaimDataSource.castClaims.add(0, claim)
            castDataSource.affiliatedCafeIdByUser[userId] = cafeId
            claim
        }
    }

    override suspend fun approveCastClaim(claimId: String, reviewedBy: String): CastClaim {
        return updateCastClaimStatus(claimId, reviewedBy, CastClaimStatus.APPROVED)
    }

    override suspend fun rejectCastClaim(claimId: String, reviewedBy: String): CastClaim {
        return updateCastClaimStatus(claimId, reviewedBy, CastClaimStatus.REJECTED)
    }

    private suspend fun updateCastClaimStatus(
        claimId: String,
        reviewedBy: String,
        status: CastClaimStatus
    ): CastClaim {
        val firestoreDataSource = castClaimDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            return firestoreDataSource.updateCastClaimStatusRemote(
                claimId = claimId,
                reviewedBy = reviewedBy,
                status = status
            )
        }
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

private const val CAFE_CASTS_REFRESH_INTERVAL_MILLIS = 30_000L
private const val USER_CLAIMS_REFRESH_INTERVAL_MILLIS = 30_000L
private const val CAFE_CLAIMS_REFRESH_INTERVAL_MILLIS = 5_000L
