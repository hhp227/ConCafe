package com.hhp227.concafe.presentation.auth.signup

sealed interface SignUpEvent {
    data object SignedUp : SignUpEvent
    data object NavigateBack : SignUpEvent
}
