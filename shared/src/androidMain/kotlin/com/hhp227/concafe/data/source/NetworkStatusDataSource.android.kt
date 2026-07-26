package com.hhp227.concafe.data.source

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin

actual fun observePlatformNetworkConnection(): Flow<Boolean> {
    return callbackFlow {
        val context = getKoin().get<Context>()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(connectivityManager.isCurrentlyConnected())
            }

            override fun onLost(network: Network) {
                trySend(connectivityManager.isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(connectivityManager.isCurrentlyConnected())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        trySend(connectivityManager.isCurrentlyConnected())
        connectivityManager.registerDefaultNetworkCallback(callback)

        // Doze 모드나 화면 잠금 후 복귀 시 콜백이 누락될 수 있어 주기적 폴링으로 보완
        val pollingJob = launch {
            while (isActive) {
                delay(NETWORK_POLL_INTERVAL_MS)
                trySend(connectivityManager.isCurrentlyConnected())
            }
        }

        awaitClose {
            pollingJob.cancel()
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}

private const val NETWORK_POLL_INTERVAL_MS = 5_000L

private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
    val activeNetwork = activeNetwork
    val capabilities = getNetworkCapabilities(activeNetwork)
    val hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val hasValidatedCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    return hasInternetCapability && hasValidatedCapability
}
