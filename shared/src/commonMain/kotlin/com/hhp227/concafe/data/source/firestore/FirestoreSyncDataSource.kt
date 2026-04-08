package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.AdminOperationsMetrics
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.User

interface FirestoreSyncDataSource {
    suspend fun fetchUser(userId: String): User?

    suspend fun fetchMyPageSummary(userId: String): MyPageSummary?

    suspend fun updateUserProfile(
        userId: String,
        nickname: String,
        profileImage: String?
    )

    suspend fun pushUser(user: User)

    suspend fun deleteUser(userId: String)

    suspend fun deleteCurrentUserCascade(idToken: String)

    suspend fun pushCafeRegistrationClaim(
        requesterUserId: String,
        claim: CafeRegistrationClaim
    )

    suspend fun pushCafeOwnerClaim(
        requesterUserId: String,
        claim: CafeManagementData.PendingClaimSummary,
        location: String,
        imageUrl: String?
    )

    suspend fun pushHomeBanner(banner: HomeBanner)

    suspend fun deleteHomeBanner(bannerId: String)

    suspend fun refreshHomeBanners()

    suspend fun refreshCafeManagementData(userId: String)

    suspend fun fetchPendingCafeOwnerClaimsForAdmin(): List<PendingCafeOwnerClaimPreview>

    suspend fun fetchPendingCafeRegistrationClaimsForAdmin(): List<PendingCafeRegistrationClaimPreview>

    suspend fun approveCafeOwnerClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview

    suspend fun rejectCafeOwnerClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview

    suspend fun approveCafeRegistrationClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview

    suspend fun rejectCafeRegistrationClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview

    suspend fun fetchAdminOperationsMetrics(): AdminOperationsMetrics
}
