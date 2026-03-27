package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomeFeed
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.NoticeRepository
import com.hhp227.concafe.domain.model.CafeSort
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetHomeFeedUseCase(
    private val bannerRepository: BannerRepository,
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository,
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(
        popularCastCursor: String? = null,
        nearbyCafeCursor: String? = null
    ): AppResult<HomeFeed> {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val nearbyCafePage = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = nearbyCafeCursor,
                pageSize = NEARBY_CAFE_PAGE_SIZE
            )
            val popularCastPage = castRepository.getHomePopularCastPage(
                cursor = popularCastCursor,
                pageSize = POPULAR_CAST_PAGE_SIZE
            )
            val popularCastCafeNames = popularCastPage.items
                .map { it.cafeId }
                .distinct()
                .associateWith { cafeId ->
                    runCatching { cafeRepository.getCafeDetail(cafeId).cafe.name }
                        .getOrElse { cafeId }
                }
            val birthdayCasts = castRepository.getBirthdayCasts(
                month = today.monthNumber,
                dayOfMonth = today.dayOfMonth,
                limit = HOME_FEED_LIMIT
            )
            val notices = noticeRepository.getRecentNotices(HOME_FEED_LIMIT)

            AppResult.Success(
                HomeFeed(
                    banners = bannerRepository.getHomeBanners(HOME_FEED_LIMIT),
                    popularCasts = popularCastPage.items,
                    popularCastCafeNames = popularCastCafeNames,
                    popularCastsNextCursor = popularCastPage.nextCursor,
                    hasMorePopularCasts = popularCastPage.hasNext,
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
        private const val POPULAR_CAST_PAGE_SIZE = 10
        private const val NEARBY_CAFE_PAGE_SIZE = 6
    }
}
