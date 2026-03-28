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
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val bannersResult = runCatching {
            bannerRepository.getHomeBanners(HOME_FEED_LIMIT)
        }
        val nearbyCafePageResult = runCatching {
            cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = nearbyCafeCursor,
                pageSize = NEARBY_CAFE_PAGE_SIZE
            )
        }
        val popularCastPageResult = runCatching {
            castRepository.getHomePopularCastPage(
                cursor = popularCastCursor,
                pageSize = POPULAR_CAST_PAGE_SIZE
            )
        }
        val birthdayCastsResult = runCatching {
            castRepository.getBirthdayCasts(
                month = today.monthNumber,
                dayOfMonth = today.dayOfMonth,
                limit = HOME_FEED_LIMIT
            )
        }
        val noticesResult = runCatching {
            noticeRepository.getRecentNotices(HOME_FEED_LIMIT)
        }
        bannersResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        nearbyCafePageResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        popularCastPageResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        birthdayCastsResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        noticesResult.exceptionOrNull()?.let { error ->
            println("TEST, ${error.message}")
        }
        val firstError = listOf(
            bannersResult.exceptionOrNull(),
            nearbyCafePageResult.exceptionOrNull(),
            popularCastPageResult.exceptionOrNull(),
            birthdayCastsResult.exceptionOrNull(),
            noticesResult.exceptionOrNull()
        ).firstOrNull { throwable -> throwable != null }
        val allFailed = bannersResult.isFailure &&
            nearbyCafePageResult.isFailure &&
            popularCastPageResult.isFailure &&
            birthdayCastsResult.isFailure &&
            noticesResult.isFailure

        if (allFailed) {
            val throwable = firstError
            return if (throwable is NoSuchElementException) {
                AppResult.Failure(AppError.NotFound)
            } else if (throwable is IllegalArgumentException) {
                AppResult.Failure(AppError.ValidationFailed(throwable.message ?: "invalid request"))
            } else {
                AppResult.Failure(AppError.Unknown(throwable?.message))
            }
        }
        val nearbyCafePage = nearbyCafePageResult.getOrNull()
        val popularCastPage = popularCastPageResult.getOrNull()
        val popularCastCafeNames = popularCastPage
            ?.items
            ?.map { cast -> cast.cafeId }
            ?.distinct()
            ?.associateWith { cafeId ->
                runCatching { cafeRepository.getCafeDetail(cafeId).cafe.name }
                    .getOrElse { cafeId }
            }
            ?: emptyMap()
        return AppResult.Success(
            HomeFeed(
                banners = bannersResult.getOrElse { emptyList() },
                popularCasts = popularCastPage?.items ?: emptyList(),
                popularCastCafeNames = popularCastCafeNames,
                popularCastsNextCursor = popularCastPage?.nextCursor,
                hasMorePopularCasts = popularCastPage?.hasNext == true,
                nearbyCafes = nearbyCafePage?.items ?: emptyList(),
                nearbyCafesNextCursor = nearbyCafePage?.nextCursor,
                hasMoreNearbyCafes = nearbyCafePage?.hasNext == true,
                birthdayCasts = birthdayCastsResult.getOrElse { emptyList() },
                notices = noticesResult.getOrElse { emptyList() }
            )
        )
    }

    companion object {
        private const val HOME_FEED_LIMIT = 6
        private const val POPULAR_CAST_PAGE_SIZE = 10
        private const val NEARBY_CAFE_PAGE_SIZE = 6
    }
}
