package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return authRepository.observeCurrentUser()
    }
}
