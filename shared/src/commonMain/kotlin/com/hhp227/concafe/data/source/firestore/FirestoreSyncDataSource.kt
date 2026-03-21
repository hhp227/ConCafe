package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.User

interface FirestoreSyncDataSource {
    suspend fun fetchUser(userId: String): User?

    suspend fun pushUser(user: User)

    suspend fun deleteUser(userId: String)

    suspend fun pushHomeBanner(banner: HomeBanner)

    suspend fun deleteHomeBanner(bannerId: String)
}