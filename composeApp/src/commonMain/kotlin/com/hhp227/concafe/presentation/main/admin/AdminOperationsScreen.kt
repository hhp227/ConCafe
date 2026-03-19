package com.hhp227.concafe.presentation.main.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@Composable
fun AdminOperationsScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: AdminOperationsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<AdminOperationsViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                AdminOperationsEvent.NavigateToBannerEdit -> {
                    onNavigationAction(NavigationAction.NavigateToBannerEdit())
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFFCFD))
                )
            ),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            MetricsGrid(metrics = uiState.metrics)
        }
        item {
            PendingSection(uiState = uiState, onAction = viewModel::onAction)
        }
        item {
            QuickMenuSection(uiState = uiState, onAction = viewModel::onAction)
        }
        item {
            BannerRegisterCard(onClick = { viewModel.onAction(AdminOperationsAction.ClickBannerRegister) })
        }
        uiState.infoMessage?.let { message ->
            item {
                InfoBanner(message = message) {
                    viewModel.onAction(AdminOperationsAction.DismissInfoMessage)
                }
            }
        }
    }
}

@Composable
private fun BannerRegisterCard(
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("배너 등록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "플랫폼 공지 또는 프로모션 배너를 바로 등록합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A707A)
            )
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD1DC),
                    contentColor = Color(0xFF2B2330)
                )
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("새 배너 등록", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricsGrid(metrics: List<AdminMetricCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { metric ->
                    MetricCard(metric = metric, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: AdminMetricCard, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD1DC).copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(metric.icon.toImageVector(), contentDescription = null, tint = Color(0xFFEF6797), modifier = Modifier.size(16.dp))
                Text(metric.title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF7A707A))
            }
            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (metric.trend) {
                        MetricTrend.UP -> Icons.Default.TrendingUp
                        MetricTrend.DOWN -> Icons.Default.TrendingDown
                        MetricTrend.NEW -> Icons.Default.PendingActions
                    },
                    contentDescription = null,
                    tint = when (metric.trend) {
                        MetricTrend.DOWN -> Color(0xFF2E9E5B)
                        MetricTrend.UP -> Color(0xFF2E9E5B)
                        MetricTrend.NEW -> Color(0xFFEF5350)
                    },
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = metric.delta,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (metric.trend) {
                        MetricTrend.DOWN -> Color(0xFF2E9E5B)
                        MetricTrend.UP -> Color(0xFF2E9E5B)
                        MetricTrend.NEW -> Color(0xFFEF5350)
                    }
                )
            }
        }
    }
}

@Composable
private fun PendingSection(
    uiState: AdminOperationsUiState,
    onAction: (AdminOperationsAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("승인 대기 요청", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "전체보기",
                color = Color(0xFFEF6797),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAction(AdminOperationsAction.ClickSeeAllPending) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.pendingFilters.forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (chip.isSelected) Color(0xFFFFD1DC) else Color(0xFFFFD1DC).copy(alpha = 0.14f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.clickable { onAction(AdminOperationsAction.SelectPendingFilter(chip.filter)) }
                ) {
                    Text(
                        text = "${chip.label} (${chip.count})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (chip.isSelected) Color(0xFF2B2330) else Color(0xFF6F6670),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (chip.isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.filteredPendingRequests.forEach { request ->
                PendingRequestCard(
                    request = request,
                    onApprove = { onAction(AdminOperationsAction.ApprovePending(request.id)) },
                    onReject = { onAction(AdminOperationsAction.RejectPending(request.id)) }
                )
            }
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: AdminPendingRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFE7EF), Color(0xFFF4D8E2))
                        )
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(request.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF5F2F4)) {
                        Text(
                            text = request.requestedAt,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7A707A)
                        )
                    }
                }
                Text(request.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A707A))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("승인", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F2F4),
                            contentColor = Color(0xFF6F6670)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("반려", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMenuSection(
    uiState: AdminOperationsUiState,
    onAction: (AdminOperationsAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("운영 퀵메뉴", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        uiState.quickMenus.forEach { menu ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onAction(AdminOperationsAction.ClickQuickMenu(menu.id)) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(menu.accent.backgroundColor()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(menu.icon.toImageVector(), contentDescription = null, tint = menu.accent.contentColor())
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(menu.title, fontWeight = FontWeight.Bold)
                        Text(menu.description, style = MaterialTheme.typography.labelSmall, color = Color(0xFF7A707A))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB5AEB5))
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF2D8))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f), color = Color(0xFF6B5320), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "닫기",
            color = Color(0xFF6B5320),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onDismiss)
        )
    }
}

private fun AdminMetricIcon.toImageVector() = when (this) {
    AdminMetricIcon.USERS -> Icons.Default.Groups
    AdminMetricIcon.CAFE -> Icons.Default.LocalCafe
    AdminMetricIcon.PENDING -> Icons.Default.PendingActions
    AdminMetricIcon.REPORT -> Icons.Default.Report
}

private fun QuickMenuIcon.toImageVector() = when (this) {
    QuickMenuIcon.BANNER -> Icons.Default.ViewCarousel
    QuickMenuIcon.MODERATION -> Icons.Default.Gavel
    QuickMenuIcon.ANALYTICS -> Icons.Default.BarChart
}

private fun QuickMenuAccent.backgroundColor() = when (this) {
    QuickMenuAccent.PRIMARY -> Color(0xFFFFD1DC).copy(alpha = 0.32f)
    QuickMenuAccent.ROSE -> Color(0xFFFFE5EA)
    QuickMenuAccent.BLUE -> Color(0xFFE6F0FF)
}

private fun QuickMenuAccent.contentColor() = when (this) {
    QuickMenuAccent.PRIMARY -> Color(0xFF5E535C)
    QuickMenuAccent.ROSE -> Color(0xFFE05A78)
    QuickMenuAccent.BLUE -> Color(0xFF4F7DFF)
}
