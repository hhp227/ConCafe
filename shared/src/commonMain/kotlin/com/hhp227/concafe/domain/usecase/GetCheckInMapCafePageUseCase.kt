package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.model.ExploreRegionFilter
import com.hhp227.concafe.domain.repository.CafeRepository

class GetCheckInMapCafePageUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        regionKey: String,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): AppResult<List<CheckInCafeSummary>> {
        return try {
            val region = ExploreRegionFilter.from(regionKey)
            val page = cafeRepository.searchCafes(
                query = null,
                country = region.country,
                city = region.city,
                sort = CafeSort.POPULAR,
                cursor = null,
                pageSize = pageSize.coerceAtLeast(1)
            )
            AppResult.Success(
                page.items.map { cafe ->
                    CheckInCafeSummary(
                        id = cafe.id,
                        name = cafe.name,
                        locationLabel = cafe.region.city,
                        geoPoint = cafe.region.location,
                        rating = cafe.ratingAvg,
                        checkInCount = 0,
                        thumbnailImage = cafe.thumbnailImage
                    )
                }
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid region"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 80
    }
}
