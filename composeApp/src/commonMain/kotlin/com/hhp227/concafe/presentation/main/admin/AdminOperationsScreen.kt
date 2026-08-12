package com.hhp227.concafe.presentation.main.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.Report
import com.hhp227.concafe.domain.model.ReportTargetType
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.admin_banner_card_description
import concafe.composeapp.generated.resources.admin_banner_card_title
import concafe.composeapp.generated.resources.admin_inquiry_empty
import concafe.composeapp.generated.resources.admin_inquiry_load_more
import concafe.composeapp.generated.resources.admin_inquiry_title
import concafe.composeapp.generated.resources.admin_pending_owner_claim_title
import concafe.composeapp.generated.resources.admin_pending_section_title
import concafe.composeapp.generated.resources.admin_quick_menu_title
import concafe.composeapp.generated.resources.admin_writer
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.dashboard_action_approve
import concafe.composeapp.generated.resources.dashboard_action_create_banner
import concafe.composeapp.generated.resources.dashboard_action_reject
import concafe.composeapp.generated.resources.dashboard_action_view_all
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

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
                AdminOperationsEvent.NavigateToBanner -> {
                    onNavigationAction(NavigationAction.NavigateToBanner())
                }
                AdminOperationsEvent.NavigateToBannerEdit -> {
                    onNavigationAction(NavigationAction.NavigateToBannerEdit())
                }
                AdminOperationsEvent.NavigateToUserManagement -> {
                    onNavigationAction(NavigationAction.NavigateToUserManagement)
                }
                AdminOperationsEvent.NavigateToDormantAccount -> {
                    onNavigationAction(NavigationAction.NavigateToDormantAccount)
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isSystemInDarkTheme()) {
                    Modifier.background(ConCafeColors.background)
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(ConCafeColors.surfaceVariant, ConCafeColors.background)
                        )
                    )
                }
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
            InquirySection(uiState = uiState, onAction = viewModel::onAction)
        }
        item {
            ReportSection(uiState = uiState, onAction = viewModel::onAction)
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
private fun ReportSection(
    uiState: AdminOperationsUiState,
    onAction: (AdminOperationsAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("신고 내역", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (uiState.reports.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("등록된 신고가 없습니다.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp), color = ConCafeColors.textSecondary)
            }
        } else {
            uiState.reports.forEach { report -> ReportCard(report) }
            if (uiState.canLoadMoreReports || uiState.isLoadingMoreReports) {
                Button(
                    onClick = { onAction(AdminOperationsAction.LoadMoreReports) },
                    enabled = !uiState.isLoadingMoreReports
                ) {
                    if (uiState.isLoadingMoreReports) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("신고 더 불러오기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: Report) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(report.reportType, color = ConCafeColors.primary, fontWeight = FontWeight.Bold)
                Text(report.createdAtLabel, color = ConCafeColors.textSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Text("대상: ${report.targetLabel()} / ${report.targetId}", style = MaterialTheme.typography.labelMedium)
            Text("신고자: ${report.reporterNickname}", style = MaterialTheme.typography.labelSmall, color = ConCafeColors.textMuted)
            Text("상태: ${report.status.name}", style = MaterialTheme.typography.labelSmall, color = ConCafeColors.textSecondary)
        }
    }
}

private fun Report.targetLabel(): String = when (targetType) {
    ReportTargetType.COMMUNITY_POST -> "게시글"
    ReportTargetType.COMMUNITY_COMMENT -> "댓글"
}

@Composable
private fun BannerRegisterCard(
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(Res.string.admin_banner_card_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(Res.string.admin_banner_card_description),
                style = MaterialTheme.typography.bodyMedium,
                color = ConCafeColors.textSecondary
            )
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConCafeColors.primaryContainer,
                    contentColor = ConCafeColors.textPrimary
                )
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.dashboard_action_create_banner), fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = ConCafeColors.primaryContainer.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(metric.icon.toImageVector(), contentDescription = null, tint = ConCafeColors.primary, modifier = Modifier.size(16.dp))
                Text(metric.title, style = MaterialTheme.typography.labelMedium, color = ConCafeColors.textSecondary)
            }
            Text(metric.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (metric.trend) {
                        MetricTrend.UP -> Icons.AutoMirrored.Filled.TrendingUp
                        MetricTrend.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
                        MetricTrend.NEW -> Icons.Default.PendingActions
                    },
                    contentDescription = null,
                    tint = when (metric.trend) {
                        MetricTrend.DOWN -> ConCafeColors.success
                        MetricTrend.UP -> ConCafeColors.success
                        MetricTrend.NEW -> ConCafeColors.error
                    },
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = metric.delta,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (metric.trend) {
                        MetricTrend.DOWN -> ConCafeColors.success
                        MetricTrend.UP -> ConCafeColors.success
                        MetricTrend.NEW -> ConCafeColors.error
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
            Text(stringResource(Res.string.admin_pending_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(Res.string.dashboard_action_view_all),
                color = ConCafeColors.primary,
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
                    color = if (chip.isSelected) ConCafeColors.primaryContainer else ConCafeColors.primaryContainer.copy(alpha = 0.14f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.clickable { onAction(AdminOperationsAction.SelectPendingFilter(chip.filter)) }
                ) {
                    Text(
                        text = "${chip.label} (${chip.count})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (chip.isSelected) ConCafeColors.textPrimary else ConCafeColors.textSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (chip.isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.selectedPendingFilter == PendingFilter.CAFE_REGISTRATION) {
                uiState.pendingCafeRegistrationClaims.forEach { claim ->
                    PendingCafeRegistrationClaimCard(
                        claim = claim,
                        onApprove = { onAction(AdminOperationsAction.ApprovePending(claim.claimId)) },
                        onReject = { onAction(AdminOperationsAction.RejectPending(claim.claimId)) }
                    )
                }
            } else {
                uiState.pendingCafeOwnerClaims.forEach { claim ->
                    PendingCafeOwnerClaimCard(
                        claim = claim,
                        onApprove = { onAction(AdminOperationsAction.ApprovePending(claim.claimId)) },
                        onReject = { onAction(AdminOperationsAction.RejectPending(claim.claimId)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InquirySection(
    uiState: AdminOperationsUiState,
    onAction: (AdminOperationsAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.admin_inquiry_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (uiState.inquiries.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White
                )
            ) {
                Text(
                    text = stringResource(Res.string.admin_inquiry_empty),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    color = ConCafeColors.textSecondary
                )
            }
        } else {
            uiState.inquiries.forEach { inquiry ->
                InquiryCard(inquiry = inquiry)
            }
            if (uiState.canLoadMoreInquiries || uiState.isLoadingMoreInquiries) {
                Button(
                    onClick = { onAction(AdminOperationsAction.LoadMoreInquiries) },
                    enabled = !uiState.isLoadingMoreInquiries,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConCafeColors.surfaceVariant,
                        contentColor = ConCafeColors.textSecondary
                    )
                ) {
                    if (uiState.isLoadingMoreInquiries) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ConCafeColors.textMuted
                        )
                    } else {
                        Text(stringResource(Res.string.admin_inquiry_load_more), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InquiryCard(
    inquiry: Inquiry
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inquiry.inquiryType,
                    color = ConCafeColors.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = inquiry.createdAtLabel,
                    color = ConCafeColors.textSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                text = inquiry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = inquiry.content,
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.textSecondary
            )
            Text(
                text = stringResource(Res.string.admin_writer, inquiry.userNickname),
                style = MaterialTheme.typography.labelSmall,
                color = ConCafeColors.textMuted
            )
        }
    }
}

@Composable
private fun PendingCafeRegistrationClaimCard(
    claim: PendingCafeRegistrationClaimPreview,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    PendingClaimCard(
        title = claim.cafeName,
        subtitle = claim.location,
        requestedAt = claim.requestedAt,
        imageUrl = claim.imageUrl,
        onApprove = onApprove,
        onReject = onReject
    )
}

@Composable
private fun PendingCafeOwnerClaimCard(
    claim: PendingCafeOwnerClaimPreview,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    PendingClaimCard(
        title = stringResource(Res.string.admin_pending_owner_claim_title, claim.requesterNickname),
        subtitle = claim.location,
        requestedAt = claim.requestedAt,
        imageUrl = claim.imageUrl,
        onApprove = onApprove,
        onReject = onReject
    )
}

@Composable
private fun PendingClaimCard(
    title: String,
    subtitle: String,
    requestedAt: String,
    imageUrl: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White
        )
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
                            colors = listOf(ConCafeColors.surfaceTint, ConCafeColors.primaryContainer)
                        )
                    )
            ) {
                CompatImageDisplay(
                    imageUrl = imageUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(999.dp), color = ConCafeColors.surfaceVariant) {
                        Text(
                            text = requestedAt,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ConCafeColors.textSecondary
                        )
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ConCafeColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConCafeColors.primaryContainer,
                            contentColor = ConCafeColors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.dashboard_action_approve), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConCafeColors.surfaceVariant,
                            contentColor = ConCafeColors.textSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.dashboard_action_reject), fontWeight = FontWeight.Bold)
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
        Text(stringResource(Res.string.admin_quick_menu_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        uiState.quickMenus.forEach { menu ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onAction(AdminOperationsAction.ClickQuickMenu(menu.id)) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White
                )
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
                        Text(menu.description, style = MaterialTheme.typography.labelSmall, color = ConCafeColors.textSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ConCafeColors.outlineStrong)
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
            .background(ConCafeColors.warningContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f), color = ConCafeColors.goldDeep, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.common_close),
            color = ConCafeColors.goldDeep,
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
    QuickMenuIcon.USERS -> Icons.Default.Groups
    QuickMenuIcon.BANNER -> Icons.Default.ViewCarousel
    QuickMenuIcon.MODERATION -> Icons.Default.Gavel
    QuickMenuIcon.ANALYTICS -> Icons.Default.BarChart
    QuickMenuIcon.DORMANT -> Icons.Default.PersonOff
}

private fun QuickMenuAccent.backgroundColor() = when (this) {
    QuickMenuAccent.PRIMARY -> ConCafeColors.primaryContainer.copy(alpha = 0.32f)
    QuickMenuAccent.ROSE -> ConCafeColors.errorContainer
    QuickMenuAccent.BLUE -> ConCafeColors.infoContainer
}

private fun QuickMenuAccent.contentColor() = when (this) {
    QuickMenuAccent.PRIMARY -> ConCafeColors.textSecondary
    QuickMenuAccent.ROSE -> ConCafeColors.error
    QuickMenuAccent.BLUE -> ConCafeColors.info
}
