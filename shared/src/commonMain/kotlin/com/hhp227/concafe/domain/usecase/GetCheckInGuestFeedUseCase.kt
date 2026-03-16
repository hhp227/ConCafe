package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CheckInGuestFeed
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository

class GetCheckInGuestFeedUseCase(
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(): AppResult<CheckInGuestFeed> {
        return try {
            val popularCafes = cafeRepository.getPopularCheckInCafes(limit = GUEST_FEED_LIMIT)
            val popularCasts = castRepository.getPopularTodayCasts(limit = GUEST_FEED_LIMIT)

            AppResult.Success(
                CheckInGuestFeed(
                    currentLocationLabel = DEFAULT_LOCATION_LABEL,
                    mapCafes = popularCafes,
                    popularCafes = popularCafes,
                    popularCasts = popularCasts
                )
            )
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private companion object {
        private const val GUEST_FEED_LIMIT = 10
        private const val DEFAULT_LOCATION_LABEL = "서울 주요 메이드카페"
    }
}
