package com.hhp227.concafe.data.source

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.NWPathMonitor
import platform.Network.NWPathStatusSatisfied
import platform.darwin.dispatch_get_main_queue

actual fun observePlatformNetworkConnection(): Flow<Boolean> {
    return callbackFlow {
        val monitor = NWPathMonitor()
        val queue = dispatch_get_main_queue()

        monitor.pathUpdateHandler = { path ->
            trySend(path.status == NWPathStatusSatisfied)
        }
        trySend(monitor.currentPath.status == NWPathStatusSatisfied)
        monitor.startWithQueue(queue)
        awaitClose {
            monitor.cancel()
        }
    }
}
