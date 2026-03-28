package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.StampDataSource
import com.hhp227.concafe.data.source.VisitDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Stamp
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
        memo: String?,
        latitude: Double,
        longitude: Double
    ): Visit {
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource
        val stampDataSource = visitDataSource as? StampDataSource
        val normalizedMemo = memo?.trim().takeIf { !it.isNullOrBlank() }
        val verification = visitDataSource.verifyVisitResult(cafeId, latitude, longitude)

        if (!verification.verified) {
            throw IllegalArgumentException(verification.message)
        }
        if (firestoreDataSource != null) {
            val createdVisit = firestoreDataSource.createVisitRemote(
                userId = userId,
                cafeId = cafeId,
                visitedAt = visitedAt,
                memo = normalizedMemo,
                latitude = latitude,
                longitude = longitude
            )
            val stampResult = runCatching {
                firestoreDataSource.createStampRemote(
                    userId = userId,
                    cafeId = cafeId,
                    visitId = createdVisit.id
                )
            }

            if (stampResult.isFailure) {
                runCatching {
                    firestoreDataSource.deleteVisitRemote(
                        visitId = createdVisit.id,
                        requesterId = userId
                    )
                }
                throw stampResult.exceptionOrNull()
                    ?: IllegalStateException("failed to issue stamp")
            }
            stampDataSource?.stamps?.removeAll { stamp -> stamp.id == createdVisit.id }
            stampDataSource?.stamps?.add(
                Stamp(
                    id = createdVisit.id,
                    userId = userId,
                    cafeId = cafeId,
                    visitId = createdVisit.id,
                    earnedAt = Clock.System.now().toString()
                )
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
                memo = normalizedMemo,
                verified = true
            )

            visitDataSource.visits.add(localVisit)
            stampDataSource?.stamps?.removeAll { stamp -> stamp.id == localVisit.id }
            stampDataSource?.stamps?.add(
                Stamp(
                    id = localVisit.id,
                    userId = userId,
                    cafeId = cafeId,
                    visitId = localVisit.id,
                    earnedAt = Clock.System.now().toString()
                )
            )
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
                firestoreDataSource.deleteStampRemote(visitId = visitId)
            }
            runCatching {
                firestoreDataSource.refreshVisitsByUserRemote(userId)
            }
        } else {
            visitDataSource.visits.removeAll { visit -> visit.id == visitId && visit.userId == userId }
        }
        (visitDataSource as? StampDataSource)?.stamps?.removeAll { stamp ->
            stamp.id == visitId && stamp.userId == userId
        }
    }

    override suspend fun getVisits(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Visit> {
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource

        firestoreDataSource?.refreshVisitsByUserRemote(userId)
        val items = visitDataSource.visits
            .filter { visit -> visit.userId == userId }
            .sortedByDescending { visit -> visit.visitedAt }

        return pagingDataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun getVerifiedVisitUserIdsByCafe(cafeId: String): Set<String> {
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource
        return firestoreDataSource?.getVerifiedVisitUserIdsByCafe(cafeId)
            ?: visitDataSource.visits
                .asSequence()
                .filter { visit -> visit.cafeId == cafeId && visit.verified }
                .map { visit -> visit.userId }
                .toSet()
    }

    override suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean {
        val firestoreDataSource = visitDataSource as? FirestoreConCafeDataSource
        return firestoreDataSource?.hasVerifiedVisitAtCafe(userId = userId, cafeId = cafeId)
            ?: visitDataSource.visits.any { visit ->
                visit.userId == userId && visit.cafeId == cafeId && visit.verified
            }
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}
