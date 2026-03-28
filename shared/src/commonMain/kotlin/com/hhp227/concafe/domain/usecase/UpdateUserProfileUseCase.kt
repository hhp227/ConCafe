package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.UserEvent
import com.hhp227.concafe.domain.event.publisher.UserEventPublisher
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.UserRepository

class UpdateUserProfileUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userEventPublisher: UserEventPublisher
) {
    suspend operator fun invoke(nickname: String, profileImage: String?): AppResult<User> {
        val normalizedNickname = nickname.trim()
        val normalizedProfileImage = profileImage?.trim()?.ifBlank { null }
        val currentUser = authRepository.getCurrentUser()
        return try {
            if (normalizedNickname.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("닉네임을 입력해 주세요."))
            } else if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                val updatedUser = currentUser.copy(
                    nickname = normalizedNickname,
                    profileImage = normalizedProfileImage
                )

                userRepository.updateProfile(
                    userId = currentUser.id,
                    nickname = normalizedNickname,
                    profileImage = normalizedProfileImage
                )
                userEventPublisher.publish(
                    UserEvent.ProfileUpdated(updatedUser)
                )
                AppResult.Success(updatedUser)
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
