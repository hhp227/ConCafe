package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.ExploreFeed
import com.hhp227.concafe.domain.model.ExploreRegionFilter
import com.hhp227.concafe.domain.model.ExploreSortFilter
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository

class GetExploreFeedUseCase(
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(
        query: String?,
        regionKey: String,
        sortKey: String,
        pageSize: Int
    ): AppResult<ExploreFeed> {
        return try {
            val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
            val cappedPageSize = pageSize.coerceAtLeast(1)
            val region = ExploreRegionFilter.from(regionKey)
            val sort = ExploreSortFilter.from(sortKey)
            val cafes = cafeRepository.searchCafes(
                query = normalizedQuery,
                country = region.country,
                city = region.city,
                sort = sort.cafeSort,
                cursor = null,
                pageSize = cappedPageSize
            ).items
            val maids = castRepository.searchCasts(
                query = normalizedQuery,
                country = region.country,
                city = region.city,
                sort = sort.castSort,
                cursor = null,
                pageSize = cappedPageSize
            ).items

            AppResult.Success(
                ExploreFeed(
                    cafes = cafes,
                    maids = maids
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
