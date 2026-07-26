package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.AdminOperationsMetrics
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AdminOperationsRepository
import com.hhp227.concafe.domain.repository.AuthRepository

class GetAdminOperationsMetricsUseCase(
    private val authRepository: AuthRepository,
    private val adminOperationsRepository: AdminOperationsRepository
) {
    suspend operator fun invoke(): AppResult<AdminOperationsMetrics> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                if (currentUser.role == UserRole.ADMIN) {
                    val metrics = adminOperationsRepository.getMetrics()
                    AppResult.Success(metrics)
                } else {
                    AppResult.Failure(AppError.PermissionDenied)
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
