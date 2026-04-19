package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.AuthDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreAuthDataSource(
) : AuthDataSource {
    private val currentUserState = MutableStateFlow<String?>(null)

    override var currentUserId: String?
        get() = currentUserState.value
        set(value) {
            currentUserState.value = value
        }

    override val currentUserIdFlow: StateFlow<String?> = currentUserState.asStateFlow()
}
