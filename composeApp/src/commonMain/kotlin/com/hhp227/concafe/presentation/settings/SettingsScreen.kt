package com.hhp227.concafe.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                SettingsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is SettingsEvent.NavigateToExternalLink -> {
                    onNavigationAction(
                        NavigationAction.NavigateToExternalLink(event.title, event.url)
                    )
                }
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
        SettingsContentScreen(
            uiState = uiState,
            innerPadding = innerPadding,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun SettingsContentScreen(
    uiState: SettingsUiState,
    innerPadding: PaddingValues,
    onAction: (SettingsAction) -> Unit
) {
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
        items(settingsItems, key = { it.id }) { item ->
            SettingsItemCard(
                item = item,
                onAction = onAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItemCard(
    item: SettingsItem,
    onAction: (SettingsAction) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        enabled = item.action != null,
        onClick = {
            item.action?.let(onAction)
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
                tint = if (item.action == SettingsAction.ClickSignOut) {
                    Color(0xFFD1436F)
                } else {
                    Color(0xFFEF6797)
                }
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
            if (item.action != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFB3ACB7)
                )
            }
        }
    }
}

private val settingsItems = listOf(
    SettingsItem(
        id = "account",
        title = "계정 관리",
        description = "프로필과 로그인 정보를 관리합니다.",
        icon = Icons.Default.PersonOutline,
        action = null
    ),
    SettingsItem(
        id = "notification",
        title = "알림 설정",
        description = "출근, 생일, 공지 알림 설정 영역입니다.",
        icon = Icons.Default.Notifications,
        action = null
    ),
    SettingsItem(
        id = "app",
        title = "앱 정보",
        description = "버전 및 고객지원 안내를 제공합니다.",
        icon = Icons.Default.Info,
        action = null
    ),
    SettingsItem(
        id = "privacyPolicy",
        title = "개인정보 처리방침",
        description = "개인정보 처리방침 외부 링크를 확인합니다.",
        icon = Icons.Default.Policy,
        action = SettingsAction.ClickPrivacyPolicy
    ),
    SettingsItem(
        id = "signout",
        title = "로그아웃",
        description = "현재 계정에서 로그아웃합니다.",
        icon = Icons.Default.Logout,
        action = SettingsAction.ClickSignOut
    )
)

private data class SettingsItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: SettingsAction?
)
