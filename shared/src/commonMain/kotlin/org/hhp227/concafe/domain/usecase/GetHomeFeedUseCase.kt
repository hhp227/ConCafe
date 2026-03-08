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
    suspend operator fun invoke(
        nearbyCafeCursor: String? = null
    ): AppResult<HomeFeed> {
        return try {
            val nearbyCafePage = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = nearbyCafeCursor,
                pageSize = NEARBY_CAFE_PAGE_SIZE
            )
            val popularCasts = castRepository.searchCasts(
                query = null,
                country = null,
                city = null,
                sort = CastSort.POPULAR,
                cursor = null,
                pageSize = HOME_FEED_LIMIT
            ).items
            val birthdayCasts = castRepository.searchCasts(
                query = null,
                country = null,
                city = null,
                sort = CastSort.LATEST,
                cursor = null,
                pageSize = HOME_FEED_LIMIT * 3
            ).items
                .filter { !it.birthday.isNullOrBlank() }
                .take(HOME_FEED_LIMIT)
            val notices = noticeRepository.getRecentNotices(HOME_FEED_LIMIT)

            AppResult.Success(
                HomeFeed(
                    banners = bannerRepository.getHomeBanners(HOME_FEED_LIMIT),
                    popularCasts = popularCasts,
                    nearbyCafes = nearbyCafePage.items,
                    nearbyCafesNextCursor = nearbyCafePage.nextCursor,
                    hasMoreNearbyCafes = nearbyCafePage.hasNext,
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

    companion object {
        private const val HOME_FEED_LIMIT = 6
        private const val NEARBY_CAFE_PAGE_SIZE = 6
    }
}
