package com.hhp227.concafe.presentation.auth.signin

sealed interface SignInAction {
    data class ChangeEmail(val value: String) : SignInAction

    data class ChangePassword(val value: String) : SignInAction

    data object ClickEmailSignIn : SignInAction

    data class ClickSocialSignIn(val provider: SignInProvider) : SignInAction
}
