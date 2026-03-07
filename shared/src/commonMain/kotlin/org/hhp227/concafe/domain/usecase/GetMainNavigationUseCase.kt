package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.MainNavigationState
import org.hhp227.concafe.domain.policy.MainNavigationPolicy
import org.hhp227.concafe.domain.repository.AuthRepository

class GetMainNavigationUseCase(
    private val authRepository: AuthRepository
) {
    private val policy = MainNavigationPolicy()

    suspend operator fun invoke(preferredRoute: String? = null): AppResult<MainNavigationState> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            val thirdTab = policy.resolveThirdTab(currentUser)
            val tabs = policy.resolveMainTabs(currentUser)
            val selectedTab = policy.normalizeMainTab(preferredRoute, currentUser)

            AppResult.Success(
                MainNavigationState(
                    currentUser = currentUser,
                    tabs = tabs,
                    selectedTab = selectedTab,
                    thirdTab = thirdTab
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid route"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
