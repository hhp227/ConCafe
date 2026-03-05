package org.hhp227.concafe.presentation.main.home

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun HomeScreen(onNavigate: (NavigationAction) -> Unit) {
    Text(" 홈 화면")

    Button(onClick = { onNavigate(NavigationAction.NavigateToDetail("id")) }) {
        Text("상세화면 이동")
    }
}