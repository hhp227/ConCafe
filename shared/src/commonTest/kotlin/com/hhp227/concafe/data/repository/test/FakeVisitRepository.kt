package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import com.hhp227.concafe.domain.repository.VisitRepository

class FakeVisitRepository(
    private val dataSource: ConCafeDataSource
) : VisitRepository {
    override suspend fun verifyVisit(cafeId: String, latitude: Double, longitude: Double, visitedAt: String): VisitVerificationResult {
        return dataSource.verifyVisitResult(cafeId, latitude, longitude)
    }

    override suspend fun createVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?,
        latitude: Double,
        longitude: Double
    ): Visit {
        val verification = dataSource.verifyVisitResult(cafeId, latitude, longitude)
        val visit = Visit(
            id = "visit-${dataSource.visits.size + 1}",
            userId = userId,
            cafeId = cafeId,
            visitedAt = visitedAt,
            memo = memo,
            verified = verification.verified
        )
        dataSource.visits.add(visit)
        return visit
    }

    override suspend fun createQrVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        val visit = Visit(
            id = "visit-${dataSource.visits.size + 1}",
            userId = userId,
            cafeId = cafeId,
            visitedAt = visitedAt,
            memo = memo,
            verified = true
        )
        dataSource.visits.add(visit)
        return visit
    }

    override suspend fun updateVisit(visitId: String, userId: String, visitedAt: String, memo: String?): Visit {
        val visitIndex = dataSource.visits.indexOfFirst { visit -> visit.id == visitId && visit.userId == userId }

        if (visitIndex < 0) {
            throw NoSuchElementException("visit not found")
        }

        val updated = dataSource.visits[visitIndex].copy(
            visitedAt = visitedAt,
            memo = memo
        )
        dataSource.visits[visitIndex] = updated
        return updated
    }

    override suspend fun deleteVisit(visitId: String, userId: String) {
        dataSource.visits.removeAll { visit -> visit.id == visitId && visit.userId == userId }
    }

    override suspend fun getVisits(userId: String, cursor: String?, pageSize: Int): PagedResult<Visit> {
        val items = dataSource.visits.filter { it.userId == userId }.sortedByDescending { it.visitedAt }
        return dataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun getVerifiedVisitUserIdsByCafe(cafeId: String): Set<String> {
        return dataSource.visits
            .asSequence()
            .filter { visit -> visit.cafeId == cafeId && visit.verified }
            .map { visit -> visit.userId }
            .toSet()
    }

    override suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean {
        return dataSource.visits.any { visit ->
            visit.userId == userId && visit.cafeId == cafeId && visit.verified
        }
    }
}
