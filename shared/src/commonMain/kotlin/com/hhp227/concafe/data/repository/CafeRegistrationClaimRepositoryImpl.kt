package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.CafeRegistrationDraft
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeRegistrationClaimRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CafeRegistrationClaimRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val cafeDataSource: CafeDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : CafeRegistrationClaimRepository {
    override suspend fun createCafeRegistrationClaim(
        userId: String,
        draft: CafeRegistrationDraft
    ): PendingCafeRegistrationClaimPreview {
        val user = authDataSource.findUserById(userId) ?: throw NoSuchElementException("user not found")
        if (draft.name.isBlank()) throw IllegalArgumentException("카페명을 입력해 주세요.")
        if (draft.description.isBlank()) throw IllegalArgumentException("카페 소개를 입력해 주세요.")
        if (draft.region.address.isBlank()) throw IllegalArgumentException("주소를 입력해 주세요.")

        val claims = cafeDataSource.pendingCafeRegistrationClaimsByUser.getOrPut(userId) { mutableListOf() }
        val existingPending = claims.firstOrNull { it.status == PENDING_STATUS }
        if (existingPending != null) {
            throw IllegalArgumentException("이미 승인 대기 중인 새 카페 등록 신청이 있습니다.")
        }

        val claim = CafeRegistrationClaim(
            claimId = nextEntityId("cafe-registration-claim"),
            cafeName = draft.name,
            description = draft.description,
            region = draft.region,
            thumbnailImage = draft.thumbnailImage,
            conceptType = draft.conceptType,
            businessHours = draft.businessHours,
            phoneNumber = draft.phoneNumber,
            requestedAt = todayDotText(),
            status = PENDING_STATUS,
            message = "관리자 승인 후 새 카페가 생성되고 운영 카페에 자동 연결됩니다."
        )
        claims.add(0, claim)
        try {
            firestoreSyncDataSource.pushCafeRegistrationClaim(
                requesterUserId = userId,
                claim = claim
            )
        } catch (e: Exception) {
            claims.removeAll { existing -> existing.claimId == claim.claimId }
            throw e
        }
        return PendingCafeRegistrationClaimPreview(
            claimId = claim.claimId,
            requesterUserId = userId,
            requesterNickname = user.nickname,
            cafeName = claim.cafeName,
            location = "${claim.region.city} ${claim.region.address}",
            requestedAt = claim.requestedAt,
            message = claim.message,
            imageUrl = claim.thumbnailImage
        )
    }

    override suspend fun getPendingCafeRegistrationClaims(): List<PendingCafeRegistrationClaimPreview> {
        return cafeDataSource.pendingCafeRegistrationClaimsByUser
            .flatMap { (userId, claims) ->
                claims.filter { it.status == PENDING_STATUS }.mapNotNull { claim ->
                    val user = authDataSource.findUserById(userId) ?: return@mapNotNull null
                    claim.toPreview(userId = userId, requesterNickname = user.nickname)
                }
            }
            .sortedByDescending { it.requestedAt }
    }

    override suspend fun approveCafeRegistrationClaim(claimId: String, reviewedBy: String): PendingCafeRegistrationClaimPreview {
        val resolved = resolveClaim(claimId)
        val claims = cafeDataSource.pendingCafeRegistrationClaimsByUser[resolved.requesterUserId]
            ?: throw NoSuchElementException("claim not found")
        claims.removeAll { it.claimId == claimId }

        val sourceClaim = resolved.sourceClaim
        val newCafeId = nextCafeId()
        val newCafe = Cafe(
            id = newCafeId,
            name = sourceClaim.cafeName,
            desc = sourceClaim.description,
            region = sourceClaim.region,
            thumbnailImage = sourceClaim.thumbnailImage,
            ratingAvg = 0.0,
            reviewCount = 0,
            approved = true,
            conceptType = sourceClaim.conceptType
        )
        cafeDataSource.cafes.add(newCafe)

        val ownedCafeIds = cafeDataSource.ownedCafeIdsByUser.getOrPut(resolved.requesterUserId) { mutableListOf() }
        if (!ownedCafeIds.contains(newCafeId)) {
            ownedCafeIds.add(newCafeId)
        }

        val currentUser = authDataSource.findUserById(resolved.requesterUserId)
        if (currentUser != null && currentUser.role != UserRole.ADMIN) {
            authDataSource.replaceUser(currentUser.copy(role = UserRole.CAFE_OWNER))
        }

        val detail = CafeDetail(
            cafe = newCafe,
            images = listOfNotNull(newCafe.thumbnailImage),
            casts = emptyList(),
            menus = emptyList(),
            goods = emptyList(),
            notices = emptyList(),
            businessHours = sourceClaim.businessHours,
            phoneNumber = sourceClaim.phoneNumber
        )
        cafeDataSource.cafeDetailsById[newCafeId] = detail
        cafeDataSource.cafeHomeBannerPreviewByCafeId[newCafeId] = CafeDashboardData.HomeBannerPreview(
            title = "${newCafe.name} 신규 오픈 준비 중",
            period = "승인 완료",
            statusLabel = "노출 준비"
        )
        return resolved.preview
    }

    override suspend fun rejectCafeRegistrationClaim(claimId: String, reviewedBy: String): PendingCafeRegistrationClaimPreview {
        val resolved = resolveClaim(claimId)
        val claims = cafeDataSource.pendingCafeRegistrationClaimsByUser[resolved.requesterUserId]
            ?: throw NoSuchElementException("claim not found")
        val claimIndex = claims.indexOfFirst { it.claimId == claimId }
        if (claimIndex == -1) {
            throw NoSuchElementException("claim not found")
        }
        val current = claims[claimIndex]
        claims[claimIndex] = current.copy(status = REJECTED_STATUS)
        return resolved.preview
    }

    private fun resolveClaim(claimId: String): ResolvedClaim {
        return cafeDataSource.pendingCafeRegistrationClaimsByUser
            .flatMap { (userId, claims) ->
                claims.mapNotNull { claim ->
                    if (claim.claimId != claimId) return@mapNotNull null
                    val user = authDataSource.findUserById(userId) ?: return@mapNotNull null
                    ResolvedClaim(
                        requesterUserId = userId,
                        sourceClaim = claim,
                        preview = claim.toPreview(userId = userId, requesterNickname = user.nickname)
                    )
                }
            }
            .firstOrNull()
            ?: throw NoSuchElementException("claim not found")
    }

    private fun nextCafeId(): String {
        return nextEntityId("cafe")
    }

    private fun CafeRegistrationClaim.toPreview(
        userId: String,
        requesterNickname: String
    ): PendingCafeRegistrationClaimPreview {
        return PendingCafeRegistrationClaimPreview(
            claimId = claimId,
            requesterUserId = userId,
            requesterNickname = requesterNickname,
            cafeName = cafeName,
            location = "${region.city} ${region.address}",
            requestedAt = requestedAt,
            message = message,
            imageUrl = thumbnailImage
        )
    }

    private data class ResolvedClaim(
        val requesterUserId: String,
        val sourceClaim: CafeRegistrationClaim,
        val preview: PendingCafeRegistrationClaimPreview
    )

    private companion object {
        const val PENDING_STATUS = "승인 대기 중"
        const val REJECTED_STATUS = "반려"
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
