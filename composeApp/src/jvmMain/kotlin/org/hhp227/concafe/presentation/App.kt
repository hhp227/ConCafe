package org.hhp227.concafe.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.hhp227.concafe.di.doInitKoin
import org.hhp227.concafe.presentation.navigation.NavigationScreen

@Composable
fun App() {
    remember {
        doInitKoin()
    }

    MaterialTheme {
        NavigationScreen()
    }
}
