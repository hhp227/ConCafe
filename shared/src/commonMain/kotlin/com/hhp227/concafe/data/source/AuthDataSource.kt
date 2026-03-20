package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.User

interface AuthDataSource {
    var currentUserId: String?
    val users: MutableList<User>
}