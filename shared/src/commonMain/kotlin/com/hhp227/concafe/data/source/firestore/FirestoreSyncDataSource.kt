package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.User

interface FirestoreSyncDataSource {
    suspend fun fetchUser(userId: String): User?

    suspend fun fetchMyPageSummary(userId: String): MyPageSummary?

    suspend fun pushUser(user: User)

    suspend fun deleteUser(userId: String)

    suspend fun pushCafeRegistrationClaim(
        requesterUserId: String,
        claim: CafeRegistrationClaim
    )

    suspend fun pushHomeBanner(banner: HomeBanner)

    suspend fun deleteHomeBanner(bannerId: String)

    suspend fun refreshCafeManagementData(userId: String)

    suspend fun fetchPendingCafeOwnerClaimsForAdmin(): List<PendingCafeOwnerClaimPreview>

    suspend fun fetchPendingCafeRegistrationClaimsForAdmin(): List<PendingCafeRegistrationClaimPreview>
}
