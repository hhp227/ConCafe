package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.MyInfoFeed
import com.hhp227.concafe.domain.model.ProfileBadge
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.UserRepository

class GetMyInfoUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(): AppResult<MyInfoFeed> {
        return try {
            val popularCafes = cafeRepository.searchCafes(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.RATING,
                cursor = null,
                pageSize = 3
            ).items
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Success(
                    MyInfoFeed(
                        isLoggedIn = false,
                        user = null,
                        summary = null,
                        castDetail = null,
                        ownedCafes = emptyList(),
                        badges = emptyList(),
                        popularCafes = popularCafes,
                        recentVisits = emptyList(),
                        favorites = emptyList(),
                        followedMaids = emptyList()
                    )
                )
            } else {
                val summary = userRepository.getMyPageSummary(currentUser.id)
                val castDetail = if (currentUser.role == UserRole.CAST) {
                    castRepository.searchCasts(
                        query = null,
                        country = null,
                        city = null,
                        sort = CastSort.FOLLOWERS,
                        cursor = null,
                        pageSize = 100
                    ).items.firstOrNull { cast ->
                        cast.linkedUserId == currentUser.id
                    }?.id?.let { castId ->
                        castRepository.getCastDetail(castId)
                    }
                } else {
                    null
                }
                val ownedCafes = if (currentUser.role == UserRole.CAFE_OWNER) {
                    cafeManagementRepository.getCafeManagementData(currentUser.id).ownedCafes
                } else {
                    emptyList()
                }
                val recentVisits = cafeRepository.searchCafes(
                    query = null,
                    country = null,
                    city = null,
                    sort = CafeSort.LATEST,
                    cursor = null,
                    pageSize = 3
                ).items
                val favorites = cafeRepository.searchCafes(
                    query = null,
                    country = null,
                    city = null,
                    sort = CafeSort.POPULAR,
                    cursor = null,
                    pageSize = summary.favoritesCount.coerceAtLeast(2)
                ).items
                val followedMaids = castRepository.searchCasts(
                    query = null,
                    country = null,
                    city = null,
                    sort = CastSort.FOLLOWERS,
                    cursor = null,
                    pageSize = summary.followedCastsCount.coerceAtLeast(3)
                ).items
                val unlockedBadges = summary.badgesCount.coerceAtLeast(0)
                val badges = listOf(
                    ProfileBadge("badge-1", "첫 방문", "🎉", unlockedBadges >= 1),
                    ProfileBadge("badge-2", "단골", "⭐", unlockedBadges >= 2),
                    ProfileBadge("badge-3", "탐험가", "🗺️", unlockedBadges >= 3),
                    ProfileBadge("badge-4", "콜렉터", "🏆", unlockedBadges >= 4),
                    ProfileBadge("badge-5", "매니아", "💎", unlockedBadges >= 5)
                )

                AppResult.Success(
                    MyInfoFeed(
                        isLoggedIn = true,
                        user = currentUser,
                        summary = summary,
                        castDetail = castDetail,
                        ownedCafes = ownedCafes,
                        badges = badges,
                        popularCafes = popularCafes,
                        recentVisits = recentVisits,
                        favorites = favorites,
                        followedMaids = followedMaids
                    )
                )
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
