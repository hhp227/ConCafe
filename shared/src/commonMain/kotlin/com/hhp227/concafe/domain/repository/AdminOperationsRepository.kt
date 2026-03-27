package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.AdminOperationsMetrics

interface AdminOperationsRepository {
    suspend fun getMetrics(): AdminOperationsMetrics
}
