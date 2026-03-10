package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.CafeManagementData

interface CafeManagementRepository {
    suspend fun getCafeManagementData(userId: String): CafeManagementData
}
