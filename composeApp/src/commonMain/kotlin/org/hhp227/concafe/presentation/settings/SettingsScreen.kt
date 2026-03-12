package com.hhp227.concafe.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<SettingsViewModel>()
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = listOf(
        SettingsItem("account", "계정 관리", "프로필과 로그인 정보를 관리합니다.", Icons.Default.PersonOutline, false),
        SettingsItem("notification", "알림 설정", "출근, 생일, 공지 알림 설정 영역입니다.", Icons.Default.Notifications, false),
        SettingsItem("app", "앱 정보", "버전 및 고객지원 안내를 제공합니다.", Icons.Default.Info, false),
        SettingsItem("signout", "로그아웃", "현재 계정에서 로그아웃합니다.", Icons.Default.Logout, true)
    )

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                SettingsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(SettingsAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "내정보 탭의 설정 바로가기에서 진입한 화면입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7C7480)
                )
            }
            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1436F)
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    onClick = {
                        if (item.isSignOut) {
                            viewModel.onAction(SettingsAction.ClickSignOut)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.isSignOut) Color(0xFFD1436F) else Color(0xFFEF6797)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7C7480)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFB3ACB7)
                        )
                    }
                }
            }
        }
    }
}

private data class SettingsItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isSignOut: Boolean
)
