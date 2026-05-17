package com.hhp227.concafe.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.ConCafeTheme
import com.hhp227.concafe.presentation.component.NetworkStatusBanner
import com.hhp227.concafe.presentation.navigation.NavigationScreen
import com.hhp227.concafe.presentation.security.ScreenCaptureProtectionEffect
import com.hhp227.concafe.presentation.theme.AppThemeMode
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

    ConCafeTheme(darkTheme = uiState.themeMode == AppThemeMode.DARK) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ScreenCaptureProtectionEffect()
            PushRegistrationEffect(appViewModel = appViewModel)
            NavigationScreen(
                hasUnreadNotifications = uiState.hasUnreadNotifications,
                onRefreshUnreadNotificationCount = {
                    appViewModel.onAction(AppAction.RefreshUnreadNotificationCount)
                }
            )
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
