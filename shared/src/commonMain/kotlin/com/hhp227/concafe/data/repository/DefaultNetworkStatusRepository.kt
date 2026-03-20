package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.NetworkStatusDataSource
import com.hhp227.concafe.domain.repository.NetworkStatusRepository
import kotlinx.coroutines.flow.Flow

class DefaultNetworkStatusRepository(
    private val networkStatusDataSource: NetworkStatusDataSource
) : NetworkStatusRepository {
    override fun observeIsConnected(): Flow<Boolean> {
        return networkStatusDataSource.observeIsConnected()
    }
}
