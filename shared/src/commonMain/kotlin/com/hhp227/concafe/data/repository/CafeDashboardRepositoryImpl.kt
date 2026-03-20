package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.repository.CafeDashboardRepository

class CafeDashboardRepositoryImpl : CafeDashboardRepository {
    override suspend fun getCafeDashboardData(
        cafeId: String,
        ownerUserId: String?
    ): CafeDashboardData {
        TODO("Not yet implemented")
    }
}