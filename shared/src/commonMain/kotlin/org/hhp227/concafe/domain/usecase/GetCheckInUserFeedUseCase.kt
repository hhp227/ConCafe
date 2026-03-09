package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CheckInUserFeed
import org.hhp227.concafe.domain.model.CheckInVisitEntry
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.CafeRepository
import org.hhp227.concafe.domain.repository.VisitRepository

class GetCheckInUserFeedUseCase(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(): AppResult<CheckInUserFeed> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                val visits = visitRepository.getVisits(
                    userId = currentUser.id,
                    cursor = null,
                    pageSize = VISIT_PAGE_SIZE
                ).items
                val visitEntries = visits.map { visit ->
                    val cafe = cafeRepository.getCafeDetail(visit.cafeId).cafe

                    CheckInVisitEntry(
                        id = visit.id,
                        cafeId = visit.cafeId,
                        cafeName = cafe.name,
                        visitedAt = visit.visitedAt,
                        visitedLabel = formatVisitedLabel(visit.visitedAt),
                        memo = visit.memo,
                        verified = visit.verified
                    )
                }

                AppResult.Success(
                    CheckInUserFeed(
                        todayVisits = visitEntries.filter { it.visitedAt.startsWith(TODAY_DATE) }.take(TODAY_VISIT_LIMIT),
                        recentVisits = visitEntries.take(RECENT_VISIT_LIMIT)
                    )
                )
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private fun formatVisitedLabel(visitedAt: String): String {
        return if (visitedAt.length >= 16) {
            val date = visitedAt.substring(5, 10).replace("-", ".")
            val time = visitedAt.substring(11, 16)
            "$date $time"
        } else {
            visitedAt
        }
    }

    private companion object {
        private const val TODAY_DATE = "2026-03-09"
        private const val TODAY_VISIT_LIMIT = 4
        private const val RECENT_VISIT_LIMIT = 5
        private const val VISIT_PAGE_SIZE = 12
    }
}
