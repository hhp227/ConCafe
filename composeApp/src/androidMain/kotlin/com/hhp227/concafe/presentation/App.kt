package com.hhp227.concafe.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.NetworkStatusBanner
import com.hhp227.concafe.presentation.navigation.NavigationScreen
import com.hhp227.concafe.push.resolveAndroidPushTokenClient
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.GlobalContext

@Composable
fun App() {
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<AppViewModel>()
            }
        }
    )
    val uiState by appViewModel.uiState.collectAsState()

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            PushRegistrationEffect(appViewModel = appViewModel)
            NavigationScreen()
            NetworkStatusBanner(
                networkAlertState = uiState.networkAlertState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PushRegistrationEffect(appViewModel: AppViewModel) {
    val context = LocalContext.current
    val pushTokenClient = remember { resolveAndroidPushTokenClient() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pushTokenClient.requestAndStoreToken { token ->
            appViewModel.onAction(AppAction.SyncPushToken(token))
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            pushTokenClient.requestAndStoreToken { token ->
                appViewModel.onAction(AppAction.SyncPushToken(token))
            }
        } else {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                pushTokenClient.requestAndStoreToken { token ->
                    appViewModel.onAction(AppAction.SyncPushToken(token))
                }
            } else {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
    LaunchedEffect(appViewModel) {
        appViewModel.event.collectLatest { event ->
            when (event) {
                is AppEvent.SyncPushToken -> {
                    val token = pushTokenClient.currentStoredToken()
                    appViewModel.onAction(AppAction.SyncPushToken(token))
                }
            }
        }
    }
}
