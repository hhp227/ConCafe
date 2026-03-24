package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult

interface VisitRepository {
    suspend fun verifyVisit(cafeId: String, latitude: Double, longitude: Double, visitedAt: String): VisitVerificationResult

    suspend fun createVisit(userId: String, cafeId: String, visitedAt: String, memo: String?): Visit

    suspend fun updateVisit(visitId: String, userId: String, visitedAt: String, memo: String?): Visit

    suspend fun deleteVisit(visitId: String, userId: String)

    suspend fun getVisits(userId: String, cursor: String?, pageSize: Int): PagedResult<Visit>
}
