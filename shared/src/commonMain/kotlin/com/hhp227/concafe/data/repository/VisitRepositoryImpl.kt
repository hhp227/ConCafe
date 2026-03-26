package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.VisitDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
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
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val createdVisit = firestoreDataSource.createVisitRemote(
                userId = userId,
                cafeId = cafeId,
                visitedAt = visitedAt,
                memo = memo
            )
            val refreshedVisit = runCatching {
                firestoreDataSource.refreshVisitsByUserRemote(userId)
                visitDataSource.visits.firstOrNull { visit -> visit.id == createdVisit.id }
            }.getOrNull()

            if (refreshedVisit == null) {
                val existingIndex = visitDataSource.visits.indexOfFirst { visit -> visit.id == createdVisit.id }

                if (existingIndex < 0) {
                    visitDataSource.visits.add(createdVisit)
                } else {
                    visitDataSource.visits[existingIndex] = createdVisit
                }
                return createdVisit
            } else {
                return refreshedVisit
            }
        } else {
            val localVisit = Visit(
                id = nextEntityId("visit"),
                userId = userId,
                cafeId = cafeId,
                visitedAt = visitedAt,
                memo = memo,
                verified = false
            )

            visitDataSource.visits.add(localVisit)
            return localVisit
        }
    }

    override suspend fun updateVisit(
        visitId: String,
        userId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            val updatedVisit = firestoreDataSource.updateVisitRemote(
                visitId = visitId,
                requesterId = userId,
                visitedAt = visitedAt,
                memo = memo
            )

            runCatching {
                firestoreDataSource.refreshVisitsByUserRemote(userId)
            }
            return updatedVisit
        } else {
            val visitIndex = visitDataSource.visits.indexOfFirst { visit -> visit.id == visitId && visit.userId == userId }

            if (visitIndex < 0) {
                throw NoSuchElementException("visit not found")
            } else {
                val updatedVisit = visitDataSource.visits[visitIndex].copy(
                    visitedAt = visitedAt,
                    memo = memo
                )
                visitDataSource.visits[visitIndex] = updatedVisit
                return updatedVisit
            }
        }
    }

    override suspend fun deleteVisit(visitId: String, userId: String) {
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            firestoreDataSource.deleteVisitRemote(
                visitId = visitId,
                requesterId = userId
            )

            runCatching {
                firestoreDataSource.refreshVisitsByUserRemote(userId)
            }
        } else {
            visitDataSource.visits.removeAll { visit -> visit.id == visitId && visit.userId == userId }
        }
    }

    override suspend fun getVisits(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Visit> {
        val items = visitDataSource.visits
            .filter { visit -> visit.userId == userId }
            .sortedByDescending { visit -> visit.visitedAt }

        return pagingDataSource.toPaged(items, cursor, pageSize)
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}
