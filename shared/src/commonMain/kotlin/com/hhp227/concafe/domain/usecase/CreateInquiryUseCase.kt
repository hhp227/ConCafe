package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.InquiryRepository

class CreateInquiryUseCase(
    private val authRepository: AuthRepository,
    private val inquiryRepository: InquiryRepository
) {
    suspend operator fun invoke(input: InquiryCreate): AppResult<Inquiry> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            AppResult.Success(
                inquiryRepository.createInquiry(
                    userId = currentUser.id,
                    userNickname = currentUser.nickname,
                    input = input
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
