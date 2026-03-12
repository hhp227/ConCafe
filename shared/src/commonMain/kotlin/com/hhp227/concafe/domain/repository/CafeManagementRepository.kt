package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.CafeManagementData

interface CafeManagementRepository {
    suspend fun getCafeManagementData(userId: String): CafeManagementData
}
