package com.hhp227.concafe.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hhp227.concafe.di.doInitConCafeAppKoin
import com.hhp227.concafe.presentation.navigation.NavigationScreen

@Composable
fun App() {
    MaterialTheme {
        NavigationScreen()
    }
}
