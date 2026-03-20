package com.hhp227.concafe.domain.repository

import kotlinx.coroutines.flow.Flow

interface NetworkStatusRepository {
    fun observeIsConnected(): Flow<Boolean>
}
