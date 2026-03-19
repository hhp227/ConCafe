package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.User

class SignInWithSocialProviderUseCase(
    private val signInUseCase: SignInUseCase
) {
    suspend operator fun invoke(provider: String): AppResult<User> {
        val normalized = provider.trim().lowercase()
        return if (normalized !in SUPPORTED_PROVIDERS) {
            AppResult.Failure(AppError.ValidationFailed("unsupported social provider"))
        } else {
            signInUseCase.invoke(
                email = "$normalized@mock.concafe",
                password = SOCIAL_SIGN_IN_PASSWORD
            )
        }
    }

    private companion object {
        val SUPPORTED_PROVIDERS = setOf("kakao", "google", "apple")
        const val SOCIAL_SIGN_IN_PASSWORD = "social-sign-in"
    }
}
