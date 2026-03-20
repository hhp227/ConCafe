package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.VisitDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import com.hhp227.concafe.domain.repository.VisitRepository
import kotlinx.datetime.Clock

class VisitRepositoryImpl(
    private val visitDataSource: VisitDataSource,
    private val pagingDataSource: PagingDataSource
) : VisitRepository {
    override suspend fun verifyVisit(
        cafeId: String,
        latitude: Double,
        longitude: Double,
        visitedAt: String
    ): VisitVerificationResult {
        return visitDataSource.verifyVisitResult(cafeId, latitude, longitude)
    }

    override suspend fun createVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        val visit = Visit(
            id = nextEntityId("visit"),
            userId = userId,
            cafeId = cafeId,
            visitedAt = visitedAt,
            memo = memo,
            verified = false
        )
        visitDataSource.visits.add(visit)
        return visit
    }

    override suspend fun getVisits(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Visit> {
        val items = visitDataSource.visits.filter { it.userId == userId }.sortedByDescending { it.visitedAt }
        return pagingDataSource.toPaged(items, cursor, pageSize)
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}
