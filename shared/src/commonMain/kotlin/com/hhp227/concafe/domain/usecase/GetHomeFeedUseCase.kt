package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomeFeed
import com.hhp227.concafe.domain.model.HomePopularCastPage
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.NoticeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetHomeFeedUseCase(
    private val bannerRepository: BannerRepository,
    private val castRepository: CastRepository,
    private val noticeRepository: NoticeRepository,
    private val getNearbyCafePageUseCase: GetNearbyCafePageUseCase,
    private val getPopularCastPageUseCase: GetPopularCastPageUseCase
) {
    suspend operator fun invoke(): AppResult<HomeFeed> = coroutineScope {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val bannersDeferred = async {
            runCatching { bannerRepository.getHomeBanners(HOME_FEED_LIMIT) }
        }
        val nearbyCafePageDeferred = async {
            runCatching {
                getNearbyCafePageUseCase.invoke(cursor = null).let { result ->
                    check(result is AppResult.Success) { "nearby cafe load failed" }
                    result.data
                }
            }
        }
        val popularCastPageDeferred = async {
            runCatching {
                getPopularCastPageUseCase.invoke(cursor = null).let { result ->
                    check(result is AppResult.Success) { "popular cast load failed" }
                    result.data
                }
            }
        }
        val birthdayCastsDeferred = async {
            runCatching {
                castRepository.getBirthdayCasts(
                    month = today.monthNumber,
                    dayOfMonth = today.dayOfMonth,
                    limit = HOME_FEED_LIMIT
                )
            }
        }
        val noticesDeferred = async {
            runCatching { noticeRepository.getRecentNotices(HOME_FEED_LIMIT) }
        }
        val bannersResult = bannersDeferred.await()
        val nearbyCafePageResult = nearbyCafePageDeferred.await()
        val popularCastPageResult = popularCastPageDeferred.await()
        val birthdayCastsResult = birthdayCastsDeferred.await()
        val noticesResult = noticesDeferred.await()
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
            return@coroutineScope when (throwable) {
                is NoSuchElementException -> {
                    AppResult.Failure(AppError.NotFound)
                }
                is IllegalArgumentException -> {
                    AppResult.Failure(AppError.ValidationFailed(throwable.message ?: "invalid request"))
                }
                else -> {
                    AppResult.Failure(AppError.Unknown(throwable?.message))
                }
            }
        }
        val nearbyCafePage = nearbyCafePageResult.getOrNull()
        val popularCastPage = popularCastPageResult.getOrNull()
        val popularCastCafeNames = popularCastPage?.cafeNames ?: emptyMap()
        return@coroutineScope AppResult.Success(
            HomeFeed(
                banners = bannersResult.getOrElse { emptyList() },
                popularCasts = popularCastPage?.casts ?: emptyList(),
                popularCastCafeNames = popularCastCafeNames,
                popularCastsNextCursor = popularCastPage?.nextCursor,
                hasMorePopularCasts = popularCastPage?.hasNext == true,
                nearbyCafes = nearbyCafePage?.items ?: emptyList(),
                nearbyCafesNextCursor = nearbyCafePage?.nextCursor,
                hasMoreNearbyCafes = nearbyCafePage?.hasNext == true,
                birthdayCasts = birthdayCastsResult.getOrElse { emptyList() },
                notices = noticesResult.getOrElse { emptyList() },
                cafeEvents = emptyList()
            )
        )
    }

    companion object {
        private const val HOME_FEED_LIMIT = 6
    }
}
