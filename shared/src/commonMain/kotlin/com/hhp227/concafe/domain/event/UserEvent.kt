package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.User

sealed class UserEvent {
    data class ProfileUpdated(val user: User) : UserEvent()
}
