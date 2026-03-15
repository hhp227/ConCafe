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
        cafeCursor: String? = null,
        maidCursor: String? = null,
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
                cursor = cafeCursor,
                pageSize = cappedPageSize
            )
            val maids = castRepository.searchCasts(
                query = normalizedQuery,
                country = region.country,
                city = region.city,
                sort = sort.castSort,
                cursor = maidCursor,
                pageSize = cappedPageSize
            )

            AppResult.Success(
                ExploreFeed(
                    cafes = cafes.items,
                    cafesNextCursor = cafes.nextCursor,
                    hasMoreCafes = cafes.hasNext,
                    maids = maids.items,
                    maidsNextCursor = maids.nextCursor,
                    hasMoreMaids = maids.hasNext
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
