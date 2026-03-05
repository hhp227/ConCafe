package org.hhp227.concafe.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.hhp227.concafe.presentation.main.checkin.CheckInScreen
import org.hhp227.concafe.presentation.main.explore.ExploreScreen
import org.hhp227.concafe.presentation.main.home.HomeScreen
import org.hhp227.concafe.presentation.main.myinfo.MyInfoScreen
import org.hhp227.concafe.presentation.main.ranking.RankingScreen
import org.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialTab: String? = null,
    onNavigationAction: (NavigationAction) -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab ?: "home") }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            selectedTab = initialTab
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ConCafe") }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                "home" -> {
                    HomeScreen(
                        onNavigate = onNavigationAction
                    )
                }
                "explore" -> {
                    ExploreScreen()
                }
                "ranking" -> {
                    RankingScreen()
                }
                "checkin" -> {
                    CheckInScreen()
                }
                "myinfo" -> {
                    MyInfoScreen()
                }
            }
        }
    }
}
