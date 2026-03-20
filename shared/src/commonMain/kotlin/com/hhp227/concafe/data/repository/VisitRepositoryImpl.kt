package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import com.hhp227.concafe.domain.repository.VisitRepository

class VisitRepositoryImpl : VisitRepository {
    override suspend fun verifyVisit(
        cafeId: String,
        latitude: Double,
        longitude: Double,
        visitedAt: String
    ): VisitVerificationResult {
        TODO("Not yet implemented")
    }

    override suspend fun createVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        TODO("Not yet implemented")
    }

    override suspend fun getVisits(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Visit> {
        TODO("Not yet implemented")
    }
}