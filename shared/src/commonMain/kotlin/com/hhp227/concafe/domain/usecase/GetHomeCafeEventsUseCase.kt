package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.HomeCafeEvent
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.NoticeRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetHomeCafeEventsUseCase(
    private val noticeRepository: NoticeRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(limit: Int = HOME_EVENT_LIMIT): AppResult<List<HomeCafeEvent>> {
        return try {
            val safeLimit = limit.coerceAtLeast(1)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val page = noticeRepository.getHomeCafeEventPage(
                cursor = null,
                pageSize = HOME_EVENT_QUERY_PAGE_SIZE
            )
            val displayableItems = page.items
                .filter { item -> item.isDisplayableHomeEvent(today) }
                .sortedWith(compareBy<CafeEventManagementItem> { item ->
                    item.homeEventSortGroup(today)
                }.thenBy { item ->
                    item.startDate.toLocalDateOrNull() ?: LocalDate(9999, 12, 31)
                }.thenBy { item ->
                    item.endDate.toLocalDateOrNull() ?: LocalDate(9999, 12, 31)
                })
                .take(safeLimit)
            val cafeNameById = displayableItems
                .map { item -> item.cafeId }
                .distinct()
                .takeIf { ids -> ids.isNotEmpty() }
                ?.let { ids ->
                    runCatching { cafeRepository.getCafesByIds(ids).associate { cafe -> cafe.id to cafe.name } }
                        .getOrElse { emptyMap() }
                }
                ?: emptyMap()

            AppResult.Success(
                displayableItems.map { item ->
                    item.toHomeCafeEvent(
                        cafeName = cafeNameById[item.cafeId] ?: item.cafeId,
                        today = today
                    )
                }
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private fun CafeEventManagementItem.toHomeCafeEvent(cafeName: String, today: LocalDate): HomeCafeEvent {
        return HomeCafeEvent(
            id = id,
            cafeId = cafeId,
            cafeName = cafeName,
            title = title,
            content = content,
            imageUrl = imageUrl,
            periodText = periodText,
            statusLabel = resolveHomeEventStatusLabel(today)
        )
    }

    private fun CafeEventManagementItem.isDisplayableHomeEvent(today: LocalDate): Boolean {
        if (isDimmed) return false
        val start = startDate.toLocalDateOrNull() ?: return false
        val end = endDate.toLocalDateOrNull() ?: start
        return today <= end
    }

    private fun CafeEventManagementItem.homeEventSortGroup(today: LocalDate): Int {
        val start = startDate.toLocalDateOrNull() ?: return 2
        return if (start <= today) 0 else 1
    }

    private fun CafeEventManagementItem.resolveHomeEventStatusLabel(today: LocalDate): String {
        val start = startDate.toLocalDateOrNull()
        val end = endDate.toLocalDateOrNull() ?: start
        return when {
            start != null && end != null && start <= today && today <= end -> "진행 중"
            start != null && today < start -> "진행 예정"
            else -> statusLabel
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        val normalized = trim().replace(".", "-").replace("/", "-")
        val parts = normalized.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    companion object {
        private const val HOME_EVENT_LIMIT = 8
        private const val HOME_EVENT_QUERY_PAGE_SIZE = 30
    }
}
