package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.*
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.*

class GetCastDetailUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(castId: String): AppResult<CastDetailFeed> {
        return try {
            val initialLoaded = coroutineScope {
                val currentUserDeferred = async {
                    authRepository.getCurrentUser()
                }
                val detailDeferred = async {
                    normalizeDetail(castRepository.getCastDetail(castId))
                }

                currentUserDeferred.await() to detailDeferred.await()
            }
            val currentUser = initialLoaded.first
            val detail = initialLoaded.second
            val taggedReviews = reviewRepository.getRecentTaggedReviews(
                cafeId = detail.cafe.id,
                castId = castId,
                limit = 3
            )
            val taggedCastIds = taggedReviews
                .flatMap { review -> review.taggedCastIds }
                .distinct()
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val today = now.date
            val weekStart = today.toWeekStart()
            val weekEnd = weekStart.plus(DatePeriod(days = 6))
            val todayDate = today.toString()
            val isSelfCast = currentUser != null && detail.cast.linkedUserId == currentUser.id
            val secondaryLoaded = coroutineScope {
                val taggedCastNamesByIdDeferred = async {
                    castRepository.getCastsByIds(taggedCastIds)
                        .associate { cast -> cast.id to cast.name }
                }
                val isFollowingDeferred = async {
                    if (currentUser != null) {
                        castRepository.isFollowing(currentUser.id, castId)
                    } else {
                        false
                    }
                }
                val todayScheduleStatusDeferred = async {
                    castRepository.getCastScheduleStatuses(
                        castId = castId,
                        fromDate = todayDate,
                        toDate = todayDate
                    )
                }

                Triple(taggedCastNamesByIdDeferred.await(), isFollowingDeferred.await(), todayScheduleStatusDeferred.await())
            }
            val taggedCastNamesById = secondaryLoaded.first
            val isFollowing = secondaryLoaded.second
            val todayScheduleStatuses = secondaryLoaded.third
            val todayStatus = todayScheduleStatuses[todayDate]
            val weekStartStr = weekStart.toString()
            val weekEndStr = weekEnd.toString()
            val weeklyDetail = detail.copy(
                schedule = detail.schedule.filter { it.date >= weekStartStr && it.date <= weekEndStr }
            )
            val todaySchedule = weeklyDetail.schedule.firstOrNull { it.date == todayDate }
            val todayAttendanceStatus = computeAttendanceStatus(todayStatus, todaySchedule?.startTime, todaySchedule?.endTime, now.hour, now.minute)
            val reviewUserIds = taggedReviews
                .map { review -> review.userId }
                .distinct()
            val userNicknameById = coroutineScope {
                reviewUserIds.associateWith { userId ->
                    async {
                        runCatching { userRepository.getUser(userId).nickname }
                            .getOrNull()
                    }
                }.mapValues { (_, deferredNickname) ->
                    deferredNickname.await()
                }
            }
            val recentReviews = taggedReviews.map { review ->
                val userNickname = review.userNickname
                    .takeIf { nickname -> nickname.isNotBlank() }
                    ?: userNicknameById[review.userId]
                    ?: CAST_UNKNOWN_USER_NICKNAME
                val taggedCastNames = review.taggedCastIds.mapNotNull { taggedCastId ->
                    taggedCastNamesById[taggedCastId]
                }
                return@map CastRecentReview(
                    id = review.id,
                    userNickname = userNickname,
                    rating = review.rating,
                    content = review.content,
                    taggedCastNames = taggedCastNames,
                    createdDateLabel = review.createdAt.toRelativeDateLabel()
                )
            }
            AppResult.Success(
                CastDetailFeed(
                    detail = weeklyDetail,
                    recentReviews = recentReviews,
                    isFollowing = isFollowing,
                    isLoggedIn = currentUser != null,
                    todayAttendanceStatus = todayAttendanceStatus,
                    isSelfCast = isSelfCast
                )
            )
        } catch (e: NoSuchElementException) {
            println("--ConCafe--, loadCastDetail, Failure ${e.message}")
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private fun normalizeDetail(detail: CastDetail): CastDetail {
        val filteredImages = detail.images.filter { it.isNotBlank() }
        val normalizedImages = if (filteredImages.isNotEmpty()) {
            filteredImages
        } else if (!detail.cast.profileImage.isNullOrBlank()) {
            listOfNotNull(detail.cast.profileImage)
        } else if (!detail.cafe.thumbnailImage.isNullOrBlank()) {
            listOfNotNull(detail.cafe.thumbnailImage)
        } else {
            listOf("")
        }

        return detail.copy(images = normalizedImages)
    }
}

private fun LocalDate.toWeekStart(): LocalDate {
    val daysFromSunday = dayOfWeek.isoDayNumber % 7
    return minus(DatePeriod(days = daysFromSunday))
}

private fun computeAttendanceStatus(
    status: CastScheduleStatus?,
    startTime: String?,
    endTime: String?,
    currentHour: Int,
    currentMinute: Int
): CastAttendanceStatus {
    if (status != CastScheduleStatus.WORK || startTime == null || endTime == null) {
        return CastAttendanceStatus.OFF
    }
    val currentTotal = currentHour * 60 + currentMinute
    val startTotal = (startTime.substringBefore(':').toIntOrNull() ?: 0) * 60 +
        (startTime.substringAfter(':').toIntOrNull() ?: 0)
    val endTotal = (endTime.substringBefore(':').toIntOrNull() ?: 0) * 60 +
        (endTime.substringAfter(':').toIntOrNull() ?: 0)
    return when {
        currentTotal < startTotal -> CastAttendanceStatus.UPCOMING
        currentTotal < endTotal -> CastAttendanceStatus.ON_SHIFT
        else -> CastAttendanceStatus.COMPLETED
    }
}

private const val CAST_UNKNOWN_USER_NICKNAME = "알 수 없음"

private fun String.toRelativeDateLabel(): String {
    val date = take(10)
    val currentDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    val today = currentDate.toString()
    val oneDayAgo = (currentDate + DatePeriod(days = -1)).toString()
    val twoDaysAgo = (currentDate + DatePeriod(days = -2)).toString()
    val threeDaysAgo = (currentDate + DatePeriod(days = -3)).toString()
    return when (date) {
        today -> "오늘"
        oneDayAgo -> "1일 전"
        twoDaysAgo -> "2일 전"
        threeDaysAgo -> "3일 전"
        else -> date
    }
}
