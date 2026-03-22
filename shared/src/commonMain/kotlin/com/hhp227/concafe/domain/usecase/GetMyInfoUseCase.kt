package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.MyInfoFeed
import com.hhp227.concafe.domain.model.ProfileBadge
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.VisitRepository
import com.hhp227.concafe.domain.repository.UserRepository

class GetMyInfoUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository,
    private val visitRepository: VisitRepository
) {
    suspend operator fun invoke(): AppResult<MyInfoFeed> {
        return try {
            val popularCafes = fetchPopularCafes(limit = 3)
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
                val firestoreUser = userRepository.getUser(currentUser.id)
                val summary = userRepository.getMyPageSummary(currentUser.id)
                val castDetail = if (firestoreUser.role == UserRole.CAST) {
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
                val ownedCafes = if (firestoreUser.role == UserRole.CAFE_OWNER) {
                    cafeManagementRepository.getCafeManagementData(currentUser.id).ownedCafes
                } else {
                    emptyList()
                }
                val recentVisitCafeIds = visitRepository.getVisits(
                    userId = currentUser.id,
                    cursor = null,
                    pageSize = 100
                ).items
                    .sortedByDescending { it.visitedAt }
                    .map { it.cafeId }
                    .distinct()
                    .take(3)

                val recentVisits = fetchCafesByIdsInOrder(recentVisitCafeIds)
                val favoriteCafeIds = cafeRepository.getFavoriteCafeIds(currentUser.id)
                val favorites = cafeRepository.getCafesByIds(favoriteCafeIds)
                    .sortedByDescending { it.ratingAvg }
                val followedCastIds = castRepository.getFollowedCastIds(currentUser.id)
                val followedMaids = castRepository.getCastsByIds(followedCastIds)
                    .sortedByDescending { it.followerCount }
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
                        user = firestoreUser,
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

    private suspend fun fetchPopularCafes(limit: Int): List<Cafe> {
        val page = cafeRepository.searchCafes(
            query = null,
            country = null,
            city = null,
            sort = CafeSort.RATING,
            cursor = null,
            pageSize = limit
        )
        return page.items
    }

    private suspend fun fetchCafesByIdsInOrder(cafeIds: List<String>): List<Cafe> {
        val cafes = cafeRepository.getCafesByIds(cafeIds)
        val cafeById = cafes.associateBy { it.id }
        return cafeIds.mapNotNull { cafeId -> cafeById[cafeId] }
    }
}
