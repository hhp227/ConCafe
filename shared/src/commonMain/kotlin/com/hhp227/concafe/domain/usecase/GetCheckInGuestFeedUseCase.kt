package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CheckInGuestFeed
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GetCheckInGuestFeedUseCase(
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(): AppResult<CheckInGuestFeed> {
        return try {
            val loaded = coroutineScope {
                val popularCafesDeferred = async {
                    cafeRepository.getPopularCheckInCafes(limit = GUEST_FEED_LIMIT)
                }
                val popularCastsDeferred = async {
                    castRepository.getPopularTodayCasts(limit = GUEST_FEED_LIMIT)
                }

                popularCafesDeferred.await() to popularCastsDeferred.await()
            }
            val popularCafes = loaded.first
            val popularCasts = loaded.second

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
