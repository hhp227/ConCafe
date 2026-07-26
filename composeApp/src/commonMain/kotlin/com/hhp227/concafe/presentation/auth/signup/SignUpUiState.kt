package com.hhp227.concafe.presentation.auth.signup

import com.hhp227.concafe.domain.model.Cafe

data class SignUpUiState(
    val step: Step = Step.SELECT_TYPE,
    val selectedUserType: UserType? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val name: String = "",
    val isCafeOwner: Boolean = false,
    val phone: String = "",
    val phoneVerificationId: String? = null,
    val verificationCode: String = "",
    val hasRequestedVerification: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val signupCompleted: Boolean = false,
    val isSocialFlow: Boolean = false,
    val socialProvider: SignUpProvider? = null,
    val hasAuthenticatedSocialAccount: Boolean = false,
    val selectedCafe: Cafe? = null,
    val cafeSearchQuery: String = "",
    val isCafeSearchVisible: Boolean = false,
    val cafes: List<Cafe> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    enum class Step {
        SELECT_TYPE,
        FORM
    }

    enum class UserType {
        VISITOR,
        CAST,
        CAFE_OWNER
    }

    companion object {
        fun empty() = SignUpUiState()
    }
}
