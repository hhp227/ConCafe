package org.hhp227.concafe.presentation.auth.signup

import org.hhp227.concafe.domain.model.Cafe

sealed interface SignUpAction {
    data object ClickBack : SignUpAction
    data class ClickUserType(val type: SignUpUiState.UserType) : SignUpAction
    data object ClickBackToTypeSelection : SignUpAction
    data class ChangeEmail(val value: String) : SignUpAction
    data class ChangePassword(val value: String) : SignUpAction
    data class ChangeConfirmPassword(val value: String) : SignUpAction
    data class ChangeNickname(val value: String) : SignUpAction
    data class ChangeName(val value: String) : SignUpAction
    data class ChangePhone(val value: String) : SignUpAction
    data class ChangeVerificationCode(val value: String) : SignUpAction
    data class ChangeCafeSearchQuery(val value: String) : SignUpAction
    data object ClickSendVerification : SignUpAction
    data object ClickVerifyCode : SignUpAction
    data object ClickToggleCafeSearch : SignUpAction
    data class ClickCafe(val cafe: Cafe) : SignUpAction
    data object ClickClearCafe : SignUpAction
    data object ClickSubmit : SignUpAction
    data class ClickSocialSignUp(val provider: SignUpProvider) : SignUpAction
    data object ClickSignInInstead : SignUpAction
}

enum class SignUpProvider {
    KAKAO,
    GOOGLE,
    APPLE
}
