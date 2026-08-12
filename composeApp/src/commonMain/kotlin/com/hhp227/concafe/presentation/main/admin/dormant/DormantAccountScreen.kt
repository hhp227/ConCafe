package com.hhp227.concafe.presentation.main.admin.dormant

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
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeColors
import com.hhp227.concafe.presentation.component.ShimmerListSkeleton
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@Composable
fun DormantAccountScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: DormantAccountViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<DormantAccountViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    DormantAccountContentScreen(
        uiState = uiState,
        onNavigateBack = { onNavigationAction(NavigationAction.NavigateBack) },
        onAction = viewModel::onAction
    )
    uiState.confirmTarget?.let { request ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(DormantAccountAction.CancelDormantChange) },
            title = { Text(if (request.dormant) "휴면 전환" else "휴면 해제", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (request.dormant) {
                        "${request.user.nickname} 계정을 휴면 상태로 전환할까요? 휴면 계정은 다시 로그인하면 자동으로 해제됩니다."
                    } else {
                        "${request.user.nickname} 계정의 휴면 상태를 해제할까요?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(DormantAccountAction.ConfirmDormantChange) },
                    enabled = !uiState.isUpdating
                ) {
                    if (uiState.isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (request.dormant) "전환" else "해제", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onAction(DormantAccountAction.CancelDormantChange) },
                    enabled = !uiState.isUpdating
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DormantAccountContentScreen(
    uiState: DormantAccountUiState,
    onNavigateBack: () -> Unit,
    onAction: (DormantAccountAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("휴면계정 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            item { FilterRow(uiState = uiState, onAction = onAction) }
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ShimmerListSkeleton(itemCount = 8)
                    }
                }
            } else if (uiState.users.isEmpty()) {
                item { EmptyDormantAccountCard(uiState.selectedFilter) }
            } else {
                items(uiState.users, key = { it.id }) { user ->
                    DormantAccountCard(
                        user = user,
                        filter = uiState.selectedFilter,
                        onAction = onAction
                    )
                }
                if (uiState.canLoadMore || uiState.isLoadingMore) {
                    item {
                        Button(
                            onClick = { onAction(DormantAccountAction.LoadMore) },
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
                        onAction(DormantAccountAction.DismissInfoMessage)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(uiState: DormantAccountUiState, onAction: (DormantAccountAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.filterChips.forEach { chip ->
            FilterChip(
                selected = chip.isSelected,
                onClick = { onAction(DormantAccountAction.SelectFilter(chip.filter)) },
                label = { Text(chip.label, fontWeight = if (chip.isSelected) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = if (chip.filter == DormantAccountFilter.DORMANT) Icons.Default.PersonOff else Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DormantAccountCard(
    user: User,
    filter: DormantAccountFilter,
    onAction: (DormantAccountAction) -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ConCafeColors.surface)) {
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
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (user.dormant) ConCafeColors.errorContainer else ConCafeColors.warningContainer
                    ) {
                        Text(
                            text = if (user.dormant) "휴면" else "휴면 예정",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.dormant) ConCafeColors.error else ConCafeColors.warning
                        )
                    }
                }
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = ConCafeColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "마지막 로그인 ${user.lastLoginAt?.take(10) ?: "기록 없음"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ConCafeColors.textMuted
                )
                if (filter == DormantAccountFilter.DORMANT) {
                    Text(
                        text = "휴면 전환일 ${user.dormantAt?.take(10) ?: "-"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConCafeColors.textMuted
                    )
                }
            }
            OutlinedButton(
                onClick = { onAction(DormantAccountAction.RequestDormantChange(user = user, dormant = !user.dormant)) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (filter == DormantAccountFilter.DORMANT) "휴면 해제" else "휴면 전환",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyDormantAccountCard(filter: DormantAccountFilter) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ConCafeColors.surface)) {
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
