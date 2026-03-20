package com.hhp227.concafe.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhp227.concafe.presentation.component.NetworkStatusBanner
import com.hhp227.concafe.presentation.navigation.NavigationScreen

@Composable
fun App() {
    val appViewModel: AppViewModel = viewModel()
    val uiState by appViewModel.uiState.collectAsState()

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavigationScreen()
            NetworkStatusBanner(
                networkAlertState = uiState.networkAlertState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
