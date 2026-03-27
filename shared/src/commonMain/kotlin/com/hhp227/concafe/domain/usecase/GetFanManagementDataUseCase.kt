package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.FanManagementData
import com.hhp227.concafe.domain.model.FanFollower
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class GetFanManagementDataUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<FanManagementData> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.CAST) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                val castId = castRepository.getCastByLinkedUserId(currentUser.id)
                    ?.id
                    ?: return AppResult.Failure(AppError.NotFound)
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val weekStart = today.toWeekStart()
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                val detail = castRepository.getCastDetail(castId)
                val weekSchedules = castRepository.getCastSchedules(
                    castId = castId,
                    fromDate = weekStart.toString(),
                    toDate = weekEnd.toString()
                )
                val followerSnapshots = castRepository.getFollowerSnapshots(castId)
                val followers = followerSnapshots.map { follower ->
                    runCatching {
                        val user = userRepository.getUser(follower.userId)
                        FanFollower(
                            id = user.id,
                            nickname = user.nickname,
                            profileImage = user.profileImage,
                            followedAt = follower.followedAt
                        )
                    }.getOrElse {
                        FanFollower(
                            id = follower.userId,
                            nickname = follower.userNickname?.takeIf { nickname -> nickname.isNotBlank() }
                                ?: "알 수 없는 팬",
                            profileImage = follower.userProfileImage,
                            followedAt = follower.followedAt
                        )
                    }
                }

                AppResult.Success(
                    FanManagementData(
                        user = currentUser,
                        detail = detail.copy(schedule = weekSchedules),
                        followers = followers
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}

private fun LocalDate.toWeekStart(): LocalDate {
    val daysFromSunday = dayOfWeek.isoDayNumber % 7
    return minus(DatePeriod(days = daysFromSunday))
}
