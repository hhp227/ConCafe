package com.hhp227.concafe.data.source

import kotlinx.coroutines.flow.StateFlow

interface AuthDataSource {
    var currentUserId: String?

    val currentUserIdFlow: StateFlow<String?>
}
