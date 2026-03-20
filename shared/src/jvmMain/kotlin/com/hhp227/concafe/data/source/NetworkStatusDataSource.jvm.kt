package com.hhp227.concafe.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun observePlatformNetworkConnection(): Flow<Boolean> {
    return flowOf(true)
}
