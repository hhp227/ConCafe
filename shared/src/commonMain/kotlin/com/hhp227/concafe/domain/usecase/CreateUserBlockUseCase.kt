package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserBlock
import com.hhp227.concafe.domain.model.UserBlockCreate
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.UserBlockRepository

class CreateUserBlockUseCase(
    private val authRepository: AuthRepository,
    private val userBlockRepository: UserBlockRepository
) {
    suspend operator fun invoke(blockedUserId: String, blockedNickname: String): AppResult<UserBlock> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            AppResult.Success(
                userBlockRepository.createUserBlock(
                    blockerUserId = currentUser.id,
                    blockerNickname = currentUser.nickname,
                    input = UserBlockCreate(
                        blockedUserId = blockedUserId,
                        blockedNickname = blockedNickname
                    )
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
