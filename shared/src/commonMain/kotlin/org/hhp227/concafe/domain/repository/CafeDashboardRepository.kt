package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.CafeDashboardData

interface CafeDashboardRepository {
    suspend fun getCafeDashboardData(cafeId: String, ownerUserId: String?): CafeDashboardData
}
