package com.hhp227.concafe.data.source

import kotlinx.cinterop.alloc
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
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
    return memScoped {
        val flagsVar = alloc<SCNetworkReachabilityFlagsVar>()
        val didGetFlags = SCNetworkReachabilityGetFlags(reachability, flagsVar.ptr)

        if (!didGetFlags) {
            false
        } else {
            val flags = flagsVar.ptr.pointed.value.toULong()
            val reachableFlag = kSCNetworkReachabilityFlagsReachable.toULong()
            val connectionRequiredFlag = kSCNetworkReachabilityFlagsConnectionRequired.toULong()
            val isReachable = (flags and reachableFlag) != 0uL
            val requiresConnection = (flags and connectionRequiredFlag) != 0uL

            isReachable && !requiresConnection
        }
    }
}

private const val REACHABILITY_HOST = "www.apple.com"

private const val NETWORK_POLL_INTERVAL_MS = 2_000L
