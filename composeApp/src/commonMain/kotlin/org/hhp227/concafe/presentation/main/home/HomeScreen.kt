package org.hhp227.concafe.presentation.main.home

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Text(" 홈 화면")

    Button(onClick = { onNavigate("id") }) {
        Text("상세화면 이동")
    }
}