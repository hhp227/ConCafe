package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.MyInfoFeed
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.ProfileBadge
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.VisitRepository
import com.hhp227.concafe.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                val popularCafes = fetchPopularCafes(limit = 3)
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
                val loaded = coroutineScope {
                    val popularCafesDeferred = async {
                        fetchPopularCafes(limit = 3)
                    }
                    val firestoreUserDeferred = async {
                        userRepository.getUser(currentUser.id)
                    }
                    val summaryDeferred = async {
                        userRepository.getMyPageSummary(currentUser.id)
                    }
                    val recentVisitCafeIdsDeferred = async {
                        visitRepository.getVisits(
                            userId = currentUser.id,
                            cursor = null,
                            pageSize = 100
                        ).items
                            .map { it.cafeId }
                            .distinct()
                            .take(3)
                    }
                    val favoriteCafeIdsDeferred = async {
                        cafeRepository.getFavoriteCafeIds(currentUser.id)
                    }
                    val followedCastIdsDeferred = async {
                        castRepository.getFollowedCastIds(currentUser.id)
                    }
                    val firestoreUser = firestoreUserDeferred.await()
                    val castDetailDeferred = if (firestoreUser.role == UserRole.CAST) {
                        async {
                            castRepository.getCastByLinkedUserId(currentUser.id)?.id?.let { castId ->
                                castRepository.getCastDetail(castId)
                            }
                        }
                    } else {
                        null
                    }
                    val ownedCafesDeferred = if (firestoreUser.role == UserRole.CAFE_OWNER) {
                        async {
                            cafeManagementRepository.getOwnedCafes(currentUser.id)
                        }
                    } else {
                        null
                    }
                    val recentVisitsDeferred = async {
                        fetchCafesByIdsInOrder(recentVisitCafeIdsDeferred.await())
                    }
                    val favoritesDeferred = async {
                        cafeRepository.getCafesByIds(favoriteCafeIdsDeferred.await())
                            .sortedByDescending { it.ratingAvg }
                    }
                    val followedMaidsDeferred = async {
                        castRepository.getCastsByIds(followedCastIdsDeferred.await())
                            .sortedByDescending { it.followerCount }
                    }
                    val followedCastCountDeferred = async {
                        followedCastIdsDeferred.await().size
                    }
                    LoadedMyInfoDependencies(
                        popularCafes = popularCafesDeferred.await(),
                        firestoreUser = firestoreUser,
                        summary = summaryDeferred.await().copy(
                            followedCastsCount = followedCastCountDeferred.await()
                        ),
                        castDetail = castDetailDeferred?.await(),
                        ownedCafes = ownedCafesDeferred?.await().orEmpty(),
                        recentVisits = recentVisitsDeferred.await(),
                        favorites = favoritesDeferred.await(),
                        followedMaids = followedMaidsDeferred.await()
                    )
                }
                val unlockedBadges = loaded.summary.badgesCount.coerceAtLeast(0)
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
                        user = loaded.firestoreUser,
                        summary = loaded.summary,
                        castDetail = loaded.castDetail,
                        ownedCafes = loaded.ownedCafes,
                        badges = badges,
                        popularCafes = loaded.popularCafes,
                        recentVisits = loaded.recentVisits,
                        favorites = loaded.favorites,
                        followedMaids = loaded.followedMaids
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
        if (cafeIds.isEmpty()) {
            return emptyList()
        }
        val uniqueCafeIds = cafeIds.distinct()
        val cafeById = coroutineScope {
            uniqueCafeIds.associateWith { cafeId ->
                async {
                    runCatching {
                        cafeRepository.getCafeDetail(cafeId).cafe
                    }.getOrNull()
                }
            }.mapValues { (_, deferredCafe) ->
                deferredCafe.await()
            }
        }
        return cafeIds.mapNotNull { cafeId -> cafeById[cafeId] }
    }
}

private data class LoadedMyInfoDependencies(
    val popularCafes: List<Cafe>,
    val firestoreUser: User,
    val summary: MyPageSummary,
    val castDetail: CastDetail?,
    val ownedCafes: List<CafeManagementData.OwnedCafeSummary>,
    val recentVisits: List<Cafe>,
    val favorites: List<Cafe>,
    val followedMaids: List<Cast>
)
