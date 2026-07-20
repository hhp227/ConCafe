package com.hhp227.concafe.presentation.main.admin.user

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.AdminUserFilter
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: UserManagementViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<UserManagementViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("유저 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigationAction(NavigationAction.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ConCafeColors.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { FilterRow(uiState = uiState, onAction = viewModel::onAction) }
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ConCafeColors.primary)
                    }
                }
            } else if (uiState.users.isEmpty()) {
                item { EmptyUsersCard(uiState.selectedFilter) }
            } else {
                items(uiState.users, key = { it.id }) { user ->
                    UserCard(user = user)
                }
                if (uiState.canLoadMore || uiState.isLoadingMore) {
                    item {
                        Button(
                            onClick = { viewModel.onAction(UserManagementAction.LoadMore) },
                            enabled = !uiState.isLoadingMore,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConCafeColors.surfaceVariant,
                                contentColor = ConCafeColors.textSecondary
                            )
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("더 불러오기", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            uiState.infoMessage?.let { message ->
                item {
                    InfoMessage(message = message) {
                        viewModel.onAction(UserManagementAction.DismissInfoMessage)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(uiState: UserManagementUiState, onAction: (UserManagementAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.filterChips.forEach { chip ->
            FilterChip(
                selected = chip.isSelected,
                onClick = { onAction(UserManagementAction.SelectFilter(chip.filter)) },
                label = { Text(chip.label, fontWeight = if (chip.isSelected) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = if (chip.filter == AdminUserFilter.CAFE_OWNER) Icons.Default.Storefront else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun UserCard(user: User) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(ConCafeColors.surfaceTint),
                contentAlignment = Alignment.Center
            ) {
                CompatImageDisplay(imageUrl = user.profileImage, modifier = Modifier.fillMaxSize())
                if (user.profileImage.isNullOrBlank()) {
                    Text(user.nickname.take(1), fontWeight = FontWeight.Bold, color = ConCafeColors.primary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(user.nickname, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(999.dp), color = if (user.banned) ConCafeColors.errorContainer else ConCafeColors.primaryContainer) {
                        Text(
                            text = if (user.banned) "차단" else "운영자",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.banned) ConCafeColors.error else ConCafeColors.primary
                        )
                    }
                }
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = ConCafeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("가입일 ${user.createdAt.take(10)}", style = MaterialTheme.typography.labelSmall, color = ConCafeColors.textMuted)
            }
        }
    }
}

@Composable
private fun EmptyUsersCard(filter: AdminUserFilter) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(
            text = "${filter.label} 목록이 없습니다.",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = ConCafeColors.textSecondary
        )
    }
}

@Composable
private fun InfoMessage(message: String, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = ConCafeColors.goldContainer) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = ConCafeColors.goldDeep)
            TextButton(onClick = onDismiss) {
                Text("닫기", color = ConCafeColors.goldDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}
