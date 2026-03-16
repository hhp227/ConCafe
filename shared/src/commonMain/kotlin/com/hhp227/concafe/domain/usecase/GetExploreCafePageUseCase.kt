package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.ExploreRegionFilter
import com.hhp227.concafe.domain.model.ExploreSortFilter
import com.hhp227.concafe.domain.repository.CafeRepository

class GetExploreCafePageUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        query: String?,
        regionKey: String,
        sortKey: String,
        cursor: String?,
        pageSize: Int
    ): AppResult<PagedResult<Cafe>> {
        return try {
            val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
            val cappedPageSize = pageSize.coerceAtLeast(1)
            val region = ExploreRegionFilter.from(regionKey)
            val sort = ExploreSortFilter.from(sortKey)

            AppResult.Success(
                cafeRepository.searchCafes(
                    query = normalizedQuery,
                    country = region.country,
                    city = region.city,
                    sort = sort.cafeSort,
                    cursor = cursor,
                    pageSize = cappedPageSize
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
