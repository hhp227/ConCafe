package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.AdminOperationsMetrics
import com.hhp227.concafe.domain.repository.AdminOperationsRepository

class AdminOperationsRepositoryImpl(
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : AdminOperationsRepository {
    override suspend fun getMetrics(): AdminOperationsMetrics {
        return firestoreSyncDataSource.fetchAdminOperationsMetrics()
    }
}
