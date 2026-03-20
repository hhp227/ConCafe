package com.hhp227.concafe.data.source

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
    val activeNetwork = activeNetwork
    val capabilities = getNetworkCapabilities(activeNetwork)
    val hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val hasValidatedCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    return hasInternetCapability && hasValidatedCapability
}
