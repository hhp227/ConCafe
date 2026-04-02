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
                        runCatching {
                            fetchPopularCafes(limit = 3)
                        }.getOrElse { emptyList() }
                    }
                    val firestoreUserDeferred = async {
                        runCatching {
                            userRepository.getUser(currentUser.id)
                        }.getOrElse { currentUser }
                    }
                    val summaryDeferred = async {
                        runCatching {
                            userRepository.getMyPageSummary(currentUser.id)
                        }.getOrElse {
                            MyPageSummary(
                                userId = currentUser.id,
                                totalVisits = 0,
                                favoritesCount = 0,
                                followedCastsCount = 0,
                                badgesCount = 0,
                                level = 1
                            )
                        }
                    }
                    val recentVisitCafeIdsDeferred = async {
                        runCatching {
                            visitRepository.getVisits(
                                userId = currentUser.id,
                                cursor = null,
                                pageSize = 100
                            ).items
                        }.getOrElse { emptyList() }
                            .map { it.cafeId }
                            .distinct()
                            .take(3)
                    }
                    val favoriteCafeIdsDeferred = async {
                        runCatching {
                            cafeRepository.getFavoriteCafeIds(currentUser.id)
                        }.getOrElse { emptyList() }
                    }
                    val followedCastIdsDeferred = async {
                        runCatching {
                            castRepository.getFollowedCastIds(currentUser.id)
                        }.getOrElse { emptyList() }
                    }
                    val firestoreUser = firestoreUserDeferred.await()
                    val castDetailDeferred = if (firestoreUser.role == UserRole.CAST) {
                        async {
                            runCatching {
                                castRepository.getCastByLinkedUserId(currentUser.id)?.id?.let { castId ->
                                    castRepository.getCastDetail(castId)
                                }
                            }.getOrNull()
                        }
                    } else {
                        null
                    }
                    val ownedCafesDeferred = if (firestoreUser.role == UserRole.CAFE_OWNER) {
                        async {
                            runCatching {
                                cafeManagementRepository.getOwnedCafes(currentUser.id)
                            }.getOrElse { emptyList() }
                        }
                    } else {
                        null
                    }
                    val recentVisitsDeferred = async {
                        runCatching {
                            fetchCafesByIdsInOrder(recentVisitCafeIdsDeferred.await())
                        }.getOrElse { emptyList() }
                    }
                    val favoritesDeferred = async {
                        runCatching {
                            cafeRepository.getCafesByIds(favoriteCafeIdsDeferred.await())
                                .sortedByDescending { it.ratingAvg }
                        }.getOrElse { emptyList() }
                    }
                    val followedMaidsDeferred = async {
                        runCatching {
                            castRepository.getFollowedCasts(currentUser.id)
                                .sortedByDescending { it.followerCount }
                        }.getOrElse { emptyList() }
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
                val totalVisits = loaded.summary.totalVisits.coerceAtLeast(0)
                val favoritesCount = loaded.summary.favoritesCount.coerceAtLeast(0)
                val followedCastsCount = loaded.summary.followedCastsCount.coerceAtLeast(0)
                val level = loaded.summary.level.coerceAtLeast(1)
                val badges = buildActivityBadges(
                    stampCount = unlockedBadges,
                    totalVisits = totalVisits,
                    favoritesCount = favoritesCount,
                    followedCastsCount = followedCastsCount,
                    level = level
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

    private fun buildActivityBadges(
        stampCount: Int,
        totalVisits: Int,
        favoritesCount: Int,
        followedCastsCount: Int,
        level: Int
    ): List<ProfileBadge> {
        val normalizedStampCount = stampCount.coerceAtLeast(0)
        val normalizedVisitCount = totalVisits.coerceAtLeast(0)
        val normalizedFavoritesCount = favoritesCount.coerceAtLeast(0)
        val normalizedFollowedCount = followedCastsCount.coerceAtLeast(0)
        val normalizedLevel = level.coerceAtLeast(1)
        return listOf(
            ProfileBadge("badge-checkin-starter", "첫 체크인", "🎉", normalizedStampCount >= 1),
            ProfileBadge("badge-stamp-collector", "스탬프 수집가", "🧷", normalizedStampCount >= 3),
            ProfileBadge("badge-regular-visitor", "단골 방문자", "🏡", normalizedVisitCount >= 5),
            ProfileBadge("badge-checkin-veteran", "체크인 베테랑", "🗺️", normalizedVisitCount >= 10),
            ProfileBadge("badge-favorite-curator", "취향 큐레이터", "❤️", normalizedFavoritesCount >= 3),
            ProfileBadge("badge-favorite-master", "취향 마스터", "💘", normalizedFavoritesCount >= 10),
            ProfileBadge("badge-cast-supporter", "캐스트 서포터", "📣", normalizedFollowedCount >= 3),
            ProfileBadge("badge-cast-ambassador", "캐스트 앰버서더", "🫶", normalizedFollowedCount >= 10),
            ProfileBadge("badge-level-up", "레벨 성장", "🌱", normalizedLevel >= 3),
            ProfileBadge("badge-concafe-master", "ConCafe 마스터", "👑", normalizedStampCount >= 10)
        )
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
