package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CheckInUserFeed
import com.hhp227.concafe.domain.model.CheckInVisitEntry
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.VisitRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetCheckInUserFeedUseCase(
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        cursor: String?,
        pageSize: Int = RECENT_VISIT_PAGE_SIZE
    ): AppResult<CheckInUserFeed> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                val visitsPage = visitRepository.getVisits(
                    userId = currentUser.id,
                    cursor = cursor,
                    pageSize = pageSize
                )
                val visitEntries = visitsPage.items.map { visit ->
                    val cafe = cafeRepository.getCafeDetail(visit.cafeId).cafe

                    CheckInVisitEntry(
                        id = visit.id,
                        cafeId = visit.cafeId,
                        cafeName = cafe.name,
                        cafeImage = cafe.thumbnailImage.orEmpty(),
                        visitedAt = visit.visitedAt,
                        visitedLabel = formatVisitedLabel(visit.visitedAt),
                        memo = visit.memo,
                        verified = visit.verified
                    )
                }

                AppResult.Success(
                    CheckInUserFeed(
                        todayVisits = visitEntries.filter { it.visitedAt.startsWith(todayDateText()) }.take(TODAY_VISIT_LIMIT),
                        recentVisits = visitEntries,
                        recentVisitsNextCursor = visitsPage.nextCursor,
                        canLoadMoreRecentVisits = visitsPage.hasNext
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

    private fun todayDateText(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month = today.monthNumber.toString().padStart(2, '0')
        val day = today.dayOfMonth.toString().padStart(2, '0')
        return "${today.year}-$month-$day"
    }

    private companion object {
        private const val TODAY_VISIT_LIMIT = 4
        private const val RECENT_VISIT_PAGE_SIZE = 12
    }
}
