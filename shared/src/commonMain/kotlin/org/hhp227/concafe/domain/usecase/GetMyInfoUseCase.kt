package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeSort
import org.hhp227.concafe.domain.model.CastSort
import org.hhp227.concafe.domain.model.MyInfoFeed
import org.hhp227.concafe.domain.model.ProfileBadge
import org.hhp227.concafe.domain.repository.AuthRepository
import org.hhp227.concafe.domain.repository.CafeRepository
import org.hhp227.concafe.domain.repository.CastRepository
import org.hhp227.concafe.domain.repository.UserRepository

class GetMyInfoUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
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
                        badges = emptyList(),
                        popularCafes = popularCafes,
                        recentVisits = emptyList(),
                        favorites = emptyList(),
                        followedMaids = emptyList()
                    )
                )
            } else {
                val summary = userRepository.getMyPageSummary(currentUser.id)
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
