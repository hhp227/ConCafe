package org.hhp227.concafe.presentation.auth.signin

sealed interface SignInEvent {
    data object SignedIn : SignInEvent
}
