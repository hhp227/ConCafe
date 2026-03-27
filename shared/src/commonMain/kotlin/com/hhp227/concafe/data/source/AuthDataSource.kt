package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthDataSource {
    var currentUserId: String?

    val currentUserIdFlow: StateFlow<String?>

    fun findUserById(userId: String): User?

    fun findUserByEmail(email: String): User?

    fun isEmailTaken(email: String): Boolean

    fun addUser(user: User)

    fun removeUser(userId: String): Boolean

    fun replaceUser(user: User): Boolean

    fun replaceAllUsers(users: List<User>)
}
