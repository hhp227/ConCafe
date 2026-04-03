package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.VisitRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import com.hhp227.concafe.domain.repository.VisitRepository

class VisitRepositoryImpl(
    private val visitRemoteDataSource: VisitRemoteDataSource
) : VisitRepository {
    override suspend fun verifyVisit(
        cafeId: String,
        latitude: Double,
        longitude: Double,
        visitedAt: String
    ): VisitVerificationResult {
        return visitRemoteDataSource.verifyVisit(cafeId, latitude, longitude)
    }

    override suspend fun createVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?,
        latitude: Double,
        longitude: Double
    ): Visit {
        val normalizedMemo = memo?.trim().takeIf { !it.isNullOrBlank() }
        val verification = visitRemoteDataSource.verifyVisit(cafeId, latitude, longitude)

        if (!verification.verified) {
            throw IllegalArgumentException(verification.message)
        } else {
            return visitRemoteDataSource.createVisit(
                userId = userId,
                cafeId = cafeId,
                visitedAt = visitedAt,
                memo = normalizedMemo,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    override suspend fun updateVisit(
        visitId: String,
        userId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        return visitRemoteDataSource.updateVisit(
            visitId = visitId,
            userId = userId,
            visitedAt = visitedAt,
            memo = memo
        )
    }

    override suspend fun deleteVisit(visitId: String, userId: String) {
        visitRemoteDataSource.deleteVisit(visitId = visitId, userId = userId)
    }

    override suspend fun getVisits(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Visit> {
        return visitRemoteDataSource.fetchVisits(userId = userId, cursor = cursor, pageSize = pageSize)
    }

    override suspend fun getVerifiedVisitUserIdsByCafe(cafeId: String): Set<String> {
        return visitRemoteDataSource.fetchVerifiedVisitUserIdsByCafe(cafeId)
    }

    override suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean {
        return visitRemoteDataSource.hasVerifiedVisitAtCafe(userId = userId, cafeId = cafeId)
    }
}
