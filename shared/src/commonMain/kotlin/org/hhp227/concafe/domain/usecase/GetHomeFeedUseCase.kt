package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.HomeFeed
import org.hhp227.concafe.domain.repository.BannerRepository
import org.hhp227.concafe.domain.repository.CafeRepository
import org.hhp227.concafe.domain.repository.CastRepository
import org.hhp227.concafe.domain.repository.NoticeRepository
import org.hhp227.concafe.domain.model.CafeSort
import org.hhp227.concafe.domain.model.CastSort

class GetHomeFeedUseCase(
    private val bannerRepository: BannerRepository,
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository,
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(limit: Int): AppResult<HomeFeed> {
        return try {
            val cappedLimit = limit.coerceAtLeast(1)

            val nearbyCafes = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = null,
                pageSize = cappedLimit
            ).items

            val popularCasts = castRepository.searchCasts(
                query = null,
                country = null,
                city = null,
                sort = CastSort.POPULAR,
                cursor = null,
                pageSize = cappedLimit
            ).items

            val birthdayCasts = castRepository.searchCasts(
                query = null,
                country = null,
                city = null,
                sort = CastSort.LATEST,
                cursor = null,
                pageSize = cappedLimit * 3
            ).items
                .filter { !it.birthday.isNullOrBlank() }
                .take(cappedLimit)

            val notices = noticeRepository.getRecentNotices(cappedLimit)

            AppResult.Success(
                HomeFeed(
                    banners = bannerRepository.getHomeBanners(cappedLimit),
                    popularCasts = popularCasts,
                    nearbyCafes = nearbyCafes,
                    birthdayCasts = birthdayCasts,
                    notices = notices
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
}
