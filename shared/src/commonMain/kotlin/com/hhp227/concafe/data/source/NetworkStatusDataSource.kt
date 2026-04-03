package com.hhp227.concafe.data.source

import kotlinx.coroutines.flow.Flow

interface NetworkStatusDataSource {
    fun observeIsConnected(): Flow<Boolean>
}

class PlatformNetworkStatusDataSource : NetworkStatusDataSource {
    override fun observeIsConnected(): Flow<Boolean> {
        return observePlatformNetworkConnection()
    }
}

expect fun observePlatformNetworkConnection(): Flow<Boolean>
