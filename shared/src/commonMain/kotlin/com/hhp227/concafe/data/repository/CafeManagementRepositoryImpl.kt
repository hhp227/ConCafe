package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.repository.CafeManagementRepository

class CafeManagementRepositoryImpl : CafeManagementRepository {
    override suspend fun getCafeManagementData(userId: String): CafeManagementData {
        TODO("Not yet implemented")
    }
}