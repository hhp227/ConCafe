package com.hhp227.concafe.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.ConCafeTheme
import com.hhp227.concafe.presentation.component.NetworkStatusBanner
import com.hhp227.concafe.presentation.navigation.NavigationScreen
import kotlinx.coroutines.delay
import org.koin.core.context.GlobalContext

@Composable
fun App() {
    var isLaunchScreenVisible by remember { mutableStateOf(true) }
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<AppViewModel>()
            }
        }
    )
    val uiState by appViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        delay(800)
        isLaunchScreenVisible = false
    }
    ConCafeTheme {
        if (isLaunchScreenVisible) {
            DesktopLaunchScreen()
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
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
}
