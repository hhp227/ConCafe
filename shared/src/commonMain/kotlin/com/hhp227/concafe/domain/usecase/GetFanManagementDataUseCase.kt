package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.FanManagementData
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.UserRepository

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
                val castId = castRepository.searchCasts(
                    query = null,
                    country = null,
                    city = null,
                    sort = CastSort.FOLLOWERS,
                    cursor = null,
                    pageSize = 100
                ).items.firstOrNull { cast ->
                    cast.linkedUserId == currentUser.id
                }?.id ?: return AppResult.Failure(AppError.NotFound)

                AppResult.Success(
                    FanManagementData(
                        user = currentUser,
                        detail = castRepository.getCastDetail(castId),
                        followers = castRepository.getFollowerUserIds(castId).map { userId ->
                            userRepository.getUser(userId)
                        }
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
