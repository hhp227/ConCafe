package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Stamp
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult

interface VisitRemoteDataSource {
    suspend fun verifyVisit(
        cafeId: String,
        latitude: Double,
        longitude: Double
    ): VisitVerificationResult

    suspend fun createVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?,
        latitude: Double,
        longitude: Double
    ): Visit

    suspend fun updateVisit(
        visitId: String,
        userId: String,
        visitedAt: String,
        memo: String?
    ): Visit

    suspend fun deleteVisit(visitId: String, userId: String)

    suspend fun fetchVisits(userId: String, cursor: String?, pageSize: Int): PagedResult<Visit>

    suspend fun fetchVerifiedVisitUserIdsByCafe(cafeId: String): Set<String>

    suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean

    suspend fun fetchStamps(userId: String): List<Stamp>

    suspend fun fetchVisitCountByCafe(cafeId: String): Int
}
