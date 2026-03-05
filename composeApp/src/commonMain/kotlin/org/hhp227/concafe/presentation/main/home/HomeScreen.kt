package org.hhp227.concafe.presentation.main.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun HomeScreen(onNavigate: (NavigationAction) -> Unit) {
    Column {
        Text(" 홈 화면")
        Button(onClick = { onNavigate(NavigationAction.NavigateToCastDetail("id")) }) {
            Text("캐스트 상세화면 이동")
        }
        Button(onClick = { onNavigate(NavigationAction.NavigateToCafeDetail("id")) }) {
            Text("카페 상세화면 이동")
        }
    }
}