package com.hhp227.concafe.data.source

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable

actual fun observePlatformNetworkConnection(): Flow<Boolean> {
    return flow {
        while (currentCoroutineContext().isActive) {
            emit(isCurrentlyConnected())
            delay(NETWORK_POLL_INTERVAL_MS)
        }
    }
        .distinctUntilChanged()
}

@OptIn(ExperimentalForeignApi::class)
private fun isCurrentlyConnected(): Boolean {
    val reachability = SCNetworkReachabilityCreateWithName(null, REACHABILITY_HOST) ?: return false

    val flagsHolder = UIntArray(1)
    val didGetFlags = flagsHolder.usePinned { pinned ->
        SCNetworkReachabilityGetFlags(reachability, pinned.addressOf(0))
    }
    if (!didGetFlags) {
        return false
    }

    val flags = flagsHolder[0].toULong()
    val reachableFlag = kSCNetworkReachabilityFlagsReachable.toULong()
    val connectionRequiredFlag = kSCNetworkReachabilityFlagsConnectionRequired.toULong()
    val isReachable = (flags and reachableFlag) != 0uL
    val requiresConnection = (flags and connectionRequiredFlag) != 0uL
    return isReachable && !requiresConnection
}

private const val REACHABILITY_HOST = "www.apple.com"

private const val NETWORK_POLL_INTERVAL_MS = 2_000L
