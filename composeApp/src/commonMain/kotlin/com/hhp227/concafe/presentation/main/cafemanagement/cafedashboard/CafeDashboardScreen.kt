package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.PendingCastClaimPreview
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.main.cafemanagement.CafeManagementQrCode
import com.hhp227.concafe.presentation.main.cafemanagement.rememberCafeManagementQrCodeSaver
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeDashboardScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CafeDashboardViewModel = viewModel(
        key = "cafe-dashboard-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CafeDashboardViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val externalLinkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val socialMediaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reservationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isQrSheetVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeDashboardEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                CafeDashboardEvent.NavigateToBanner -> {
                    onNavigationAction(NavigationAction.NavigateToBanner(cafeId))
                }
                CafeDashboardEvent.NavigateToBannerEdit -> {
                    onNavigationAction(NavigationAction.NavigateToBannerEdit(cafeId))
                }
                is CafeDashboardEvent.NavigateToCafeInfoEdit -> {
                    onNavigationAction(NavigationAction.NavigateToCafeInfoEdit(event.cafeId))
                }
                is CafeDashboardEvent.NavigateToNoticeEvent -> {
                    onNavigationAction(NavigationAction.NavigateToNoticeEvent(event.cafeId))
                }
                is CafeDashboardEvent.NavigateToMenuGoods -> {
                    onNavigationAction(NavigationAction.NavigateToMenuGoods(event.cafeId))
                }
                is CafeDashboardEvent.NavigateToCastEdit -> {
                    onNavigationAction(NavigationAction.NavigateToCastEdit(event.cafeId, event.castId))
                }
                is CafeDashboardEvent.NavigateToSchedule -> {
                    onNavigationAction(NavigationAction.NavigateToSchedule(event.castId))
                }
                is CafeDashboardEvent.NavigateToExternalLink -> {
                    onNavigationAction(
                        NavigationAction.NavigateToExternalLink(
                            title = event.title,
                            url = event.url
                        )
                    )
                }
            }
        }
    }
    if (uiState.isDeleteCastDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(CafeDashboardAction.DismissDeleteCastDialog) },
            title = { Text(stringResource(Res.string.dashboard_delete_cast_title)) },
            text = { Text(stringResource(Res.string.dashboard_delete_cast_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(CafeDashboardAction.ConfirmDeleteCast) }) {
                    Text(stringResource(Res.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(CafeDashboardAction.DismissDeleteCastDialog) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    if (uiState.isExternalLinkSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(CafeDashboardAction.DismissExternalLinkSheet) },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = externalLinkSheetState
        ) {
            ExternalLinkSheetContent(
                uiState = uiState,
                onAction = viewModel::onAction,
                onSubmit = { viewModel.onAction(CafeDashboardAction.SubmitExternalLink) }
            )
        }
    }
    if (uiState.isSocialMediaSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(CafeDashboardAction.DismissSocialMediaSheet) },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = socialMediaSheetState
        ) {
            SocialMediaSheetContent(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
    if (uiState.isReservationSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(CafeDashboardAction.DismissReservationSheet) },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = reservationSheetState
        ) {
            ReservationSheetContent(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
    if (isQrSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isQrSheetVisible = false },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = qrSheetState
        ) {
            DashboardQrSheetContent(
                payload = buildCafeCheckInQrPayload(uiState.cafe?.id ?: cafeId)
            )
        }
    }
    CafeDashboardContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onQrMetricClick = { isQrSheetVisible = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CafeDashboardContentScreen(
    uiState: CafeDashboardUiState,
    onAction: (CafeDashboardAction) -> Unit,
    onQrMetricClick: () -> Unit
) {
    val cafe = uiState.cafe

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(cafe?.name ?: stringResource(Res.string.dashboard_title))
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CafeDashboardAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cafe_accessibility_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), MaterialTheme.colorScheme.background)
                    )
                )
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    cafe?.let {
                        item {
                            DashboardHeroCard(cafe = it)
                        }
                    }
                    uiState.infoMessage?.let { message ->
                        item {
                            InfoBanner(
                                message = when (message) {
                                    "dashboard_info_cast_list_load_failed" -> stringResource(Res.string.dashboard_info_cast_list_load_failed)
                                    "dashboard_info_select_cast_for_schedule" -> stringResource(Res.string.dashboard_info_select_cast_for_schedule)
                                    "dashboard_info_external_link_input_required" -> stringResource(Res.string.dashboard_info_external_link_input_required)
                                    "dashboard_info_external_link_updated" -> stringResource(Res.string.dashboard_info_external_link_updated)
                                    "dashboard_info_external_link_added" -> stringResource(Res.string.dashboard_info_external_link_added)
                                    "dashboard_info_external_link_deleted" -> stringResource(Res.string.dashboard_info_external_link_deleted)
                                    "dashboard_info_select_cast_for_delete" -> stringResource(Res.string.dashboard_info_select_cast_for_delete)
                                    "dashboard_info_cast_deleted" -> stringResource(Res.string.dashboard_info_cast_deleted)
                                    "dashboard_info_cast_claim_approved" -> stringResource(Res.string.dashboard_info_cast_claim_approved)
                                    "dashboard_info_cast_claim_rejected" -> stringResource(Res.string.dashboard_info_cast_claim_rejected)
                                    "dashboard_info_social_media_saved" -> stringResource(Res.string.dashboard_info_social_media_saved)
                                    "dashboard_info_reservation_saved" -> stringResource(Res.string.dashboard_info_reservation_saved)
                                    else -> message
                                },
                                onDismiss = { onAction(CafeDashboardAction.DismissInfoMessage) }
                            )
                        }
                    }
                    if (cafe != null) {
                        item {
                            DashboardMetricGrid(cafe = cafe)
                        }
                        if (uiState.pendingCastClaims.isNotEmpty()) {
                            item {
                                PendingCastClaimSection(
                                    claims = uiState.pendingCastClaims,
                                    onApprove = { onAction(CafeDashboardAction.ClickApproveCastClaim(it)) },
                                    onReject = { onAction(CafeDashboardAction.ClickRejectCastClaim(it)) }
                                )
                            }
                        }
                        item {
                            ShortcutGrid(
                                onQrMetricClick = onQrMetricClick,
                                onShortcutClick = { shortcut ->
                                    onAction(CafeDashboardAction.ClickShortcut(shortcut))
                                }
                            )
                        }
                        item {
                            CastManagementSection(
                                casts = uiState.castPreviews,
                                hasMoreCasts = uiState.hasMoreCasts,
                                isLoadingMoreCasts = uiState.isLoadingMoreCasts,
                                onCastManagementClick = {
                                    onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.CAST_MANAGEMENT))
                                },
                                onDeleteClick = {
                                    onAction(CafeDashboardAction.ClickDeleteCast)
                                },
                                canDelete = uiState.selectedCastId != null,
                                onScheduleClick = {
                                    onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.CAST_SCHEDULE))
                                },
                                selectedCastId = uiState.selectedCastId,
                                onCastScheduleSelect = { castId ->
                                    onAction(CafeDashboardAction.ClickCastSchedule(castId))
                                },
                                onLoadMoreClick = {
                                    onAction(CafeDashboardAction.ClickLoadMoreCasts)
                                }
                            )
                        }
                        if (uiState.externalLinks.isNotEmpty()) {
                            item {
                                ExternalLinkSection(
                                    links = uiState.externalLinks,
                                    onAddClick = {
                                        onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.EXTERNAL_LINKS))
                                    },
                                    onItemClick = { linkId ->
                                        onAction(CafeDashboardAction.ClickExternalLinkItem(linkId))
                                    },
                                    onEditClick = { linkId ->
                                        onAction(CafeDashboardAction.ClickEditExternalLink(linkId))
                                    },
                                    onDeleteClick = { linkId ->
                                        onAction(CafeDashboardAction.ClickDeleteExternalLink(linkId))
                                    }
                                )
                            }
                        }
                        item {
                            HomeBannerSection(
                                banner = cafe.homeBannerPreview,
                                onBannerClick = {
                                    onAction(CafeDashboardAction.ClickShortcut(CafeDashboardShortcut.HOME_BANNER))
                                },
                                onCreateBannerClick = {
                                    onAction(CafeDashboardAction.ClickCreateBanner)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalLinkSheetContent(
    uiState: CafeDashboardUiState,
    onAction: (CafeDashboardAction) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .keyboardBottomInsets(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.dashboard_external_link_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ConCafeFormField(
            label = stringResource(Res.string.dashboard_external_link_label_title),
            value = uiState.externalLinkTitle,
            onValueChange = { onAction(CafeDashboardAction.ChangeExternalLinkTitle(it)) },
            placeholder = stringResource(Res.string.dashboard_external_link_placeholder_title)
        )
        ConCafeFormField(
            label = stringResource(Res.string.dashboard_external_link_label_url),
            value = uiState.externalLinkUrl,
            onValueChange = { onAction(CafeDashboardAction.ChangeExternalLinkUrl(it)) },
            placeholder = "https://"
        )
        Button(
            onClick = onSubmit,
            enabled = uiState.isExternalLinkSubmitEnabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorFromHex("FFD1DC"),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = colorFromHex("F4D7DF"),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                stringResource(if (uiState.editingExternalLinkId == null) {
                    Res.string.dashboard_external_link_add
                } else {
                    Res.string.dashboard_external_link_save
                }),
                fontWeight = FontWeight.Bold
            )
        }
        TextButton(
            onClick = { onAction(CafeDashboardAction.DismissExternalLinkSheet) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.common_close))
        }
    }
}

@Composable
private fun SocialMediaSheetContent(
    uiState: CafeDashboardUiState,
    onAction: (CafeDashboardAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .keyboardBottomInsets(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.dashboard_social_media_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(Res.string.dashboard_social_media_section_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ConCafeFormField(
            label = "Instagram",
            value = uiState.instagramId,
            onValueChange = { onAction(CafeDashboardAction.ChangeSocialMediaInstagram(it)) },
            placeholder = "@account_id"
        )
        ConCafeFormField(
            label = "X (Twitter)",
            value = uiState.twitterId,
            onValueChange = { onAction(CafeDashboardAction.ChangeSocialMediaTwitter(it)) },
            placeholder = "@account_id"
        )
        ConCafeFormField(
            label = "TikTok",
            value = uiState.tiktokId,
            onValueChange = { onAction(CafeDashboardAction.ChangeSocialMediaTiktok(it)) },
            placeholder = "@account_id"
        )
        ConCafeFormField(
            label = "YouTube",
            value = uiState.youtubeId,
            onValueChange = { onAction(CafeDashboardAction.ChangeSocialMediaYoutube(it)) },
            placeholder = "@channel_id"
        )
        Button(
            onClick = { onAction(CafeDashboardAction.SubmitSocialMedia) },
            enabled = !uiState.isSavingSocialMedia,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorFromHex("FFD1DC"),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = colorFromHex("F4D7DF"),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (uiState.isSavingSocialMedia) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(stringResource(Res.string.dashboard_social_media_save), fontWeight = FontWeight.Bold)
            }
        }
        TextButton(
            onClick = { onAction(CafeDashboardAction.DismissSocialMediaSheet) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.common_close))
        }
    }
}

@Composable
private fun ReservationSheetContent(
    uiState: CafeDashboardUiState,
    onAction: (CafeDashboardAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .keyboardBottomInsets(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.dashboard_reservation_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(Res.string.dashboard_reservation_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ConCafeFormField(
            label = stringResource(Res.string.dashboard_reservation_label_url),
            value = uiState.reservationUrl,
            onValueChange = { onAction(CafeDashboardAction.ChangeReservationUrl(it)) },
            placeholder = "https://"
        )
        Button(
            onClick = { onAction(CafeDashboardAction.SubmitReservation) },
            enabled = !uiState.isSavingReservation,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorFromHex("FFD1DC"),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = colorFromHex("F4D7DF"),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (uiState.isSavingReservation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(stringResource(Res.string.dashboard_reservation_save), fontWeight = FontWeight.Bold)
            }
        }
        TextButton(
            onClick = { onAction(CafeDashboardAction.DismissReservationSheet) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.common_close))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalLinkSection(
    links: List<CafeDashboardExternalLink>,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, colorFromHex("E8DFE7"))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.dashboard_shortcut_external_links),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorFromHex("8C7A83")
                    )
                    Text(
                        text = stringResource(Res.string.dashboard_external_link_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorFromHex("7E7480")
                    )
                }
                TextButton(onClick = onAddClick) {
                    Text(stringResource(Res.string.dashboard_action_add), color = colorFromHex("EF6797"))
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colorFromHex("F0E6EC"))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    links.forEach { link ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, colorFromHex("F0E6EC")),
                                onClick = { onItemClick(link.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(colorFromHex("FFD1DC"), colorFromHex("FFE4EC"))
                                                ),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Text(
                                        text = link.title,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            IconButton(onClick = { onEditClick(link.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(Res.string.dashboard_accessibility_external_link_edit),
                                    tint = colorFromHex("8F848F")
                                )
                            }
                            IconButton(onClick = { onDeleteClick(link.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.dashboard_accessibility_external_link_delete),
                                    tint = colorFromHex("8F848F")
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorFromHex("FFD1DC"),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(Res.string.dashboard_external_link_add), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
private fun PendingCastClaimSection(
    claims: List<PendingCastClaimPreview>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.dashboard_pending_claim_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        claims.take(3).forEach { claim ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${claim.requesterNickname} → ${claim.castName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(claim.requestedAtLabel, style = MaterialTheme.typography.labelSmall, color = colorFromHex("8A808A"))
                    }
                    claim.message?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = colorFromHex("5C5760"))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onApprove(claim.claimId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorFromHex("FFD1DC"),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(stringResource(Res.string.dashboard_action_approve), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onReject(claim.claimId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(Res.string.dashboard_action_reject), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroCard(
    cafe: CafeDashboardData
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(colorFromHex("2F1B3A"), colorFromHex("7C3F67"), colorFromHex("F06A9D"))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = cafe.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = cafe.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.dashboard_hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorFromHex("FFF6D7"),
        border = BorderStroke(1.dp, colorFromHex("F1D88D"))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colorFromHex("6B5320")
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_close), tint = colorFromHex("6B5320"))
            }
        }
    }
}

@Composable
private fun DashboardMetricGrid(
    cafe: CafeDashboardData
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(Res.string.dashboard_section_metrics_title),
            subtitle = stringResource(Res.string.dashboard_section_metrics_subtitle)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.dashboard_metric_today_checkin),
                value = cafe.todayCheckIns.toString(),
                accent = colorFromHex("EF6797")
            )
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.dashboard_metric_today_review),
                value = cafe.todayReviews.toString(),
                accent = colorFromHex("47A88B")
            )
            DashboardMetricCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.dashboard_metric_rating),
                value = formatRating(cafe.rating),
                accent = colorFromHex("F59E0B")
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colorFromHex("E8DFE7"))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape)
            )
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutGrid(
    onQrMetricClick: () -> Unit,
    onShortcutClick: (CafeDashboardShortcut) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = stringResource(Res.string.dashboard_section_menu_title),
            subtitle = stringResource(Res.string.dashboard_section_menu_subtitle)
        )
        DashboardQrMetricCard(
            onClick = onQrMetricClick
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.EVENT_MANAGEMENT,
                    onClick = { onShortcutClick(CafeDashboardShortcut.EVENT_MANAGEMENT) }
                )
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.CAFE_SETTINGS,
                    onClick = { onShortcutClick(CafeDashboardShortcut.CAFE_SETTINGS) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.MENU_GOODS,
                    onClick = { onShortcutClick(CafeDashboardShortcut.MENU_GOODS) }
                )
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.EXTERNAL_LINKS,
                    onClick = { onShortcutClick(CafeDashboardShortcut.EXTERNAL_LINKS) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.SOCIAL_MEDIA,
                    onClick = { onShortcutClick(CafeDashboardShortcut.SOCIAL_MEDIA) }
                )
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    shortcut = CafeDashboardShortcut.RESERVATION,
                    onClick = { onShortcutClick(CafeDashboardShortcut.RESERVATION) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardQrMetricCard(
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colorFromHex("E8DFE7")),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorFromHex("FCE6EF")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = colorFromHex("EF6797"),
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.dashboard_metric_checkin_qr),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.dashboard_metric_checkin_qr_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colorFromHex("B8ACB4"),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f }
            )
        }
    }
}

@Composable
private fun DashboardQrSheetContent(
    payload: String
) {
    val saveQrToGallery = rememberCafeManagementQrCodeSaver()
    val coroutineScope = rememberCoroutineScope()
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val saveSuccessText = stringResource(Res.string.dashboard_qr_sheet_save_success)
    val saveFailedText = stringResource(Res.string.dashboard_qr_sheet_save_failed)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.dashboard_qr_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.dashboard_qr_sheet_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, Color(0x1AFFD1DC))
        ) {
            CafeManagementQrCode(
                payload = payload,
                modifier = Modifier
                    .size(240.dp)
                    .padding(14.dp)
            )
        }
        Button(
            onClick = {
                coroutineScope.launch {
                    val isSaved = saveQrToGallery(payload)
                    saveMessage = if (isSaved) saveSuccessText else saveFailedText
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorFromHex("FFD1DC"),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Text(
                text = stringResource(Res.string.dashboard_qr_sheet_save_button),
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }
        saveMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun buildCafeCheckInQrPayload(cafeId: String): String {
    return "concafe://checkin?cafeId=$cafeId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutCard(
    modifier: Modifier = Modifier,
    shortcut: CafeDashboardShortcut,
    onClick: () -> Unit
) {
    val icon = when (shortcut) {
        CafeDashboardShortcut.CAST_MANAGEMENT -> Icons.Default.Groups
        CafeDashboardShortcut.CAST_SCHEDULE -> Icons.Default.CalendarMonth
        CafeDashboardShortcut.EVENT_MANAGEMENT -> Icons.Default.AutoAwesome
        CafeDashboardShortcut.CAFE_SETTINGS -> Icons.Default.Settings
        CafeDashboardShortcut.MENU_GOODS -> Icons.Default.RestaurantMenu
        CafeDashboardShortcut.HOME_BANNER -> Icons.Default.Campaign
        CafeDashboardShortcut.EXTERNAL_LINKS -> Icons.Default.Link
        CafeDashboardShortcut.SOCIAL_MEDIA -> Icons.Default.Share
        CafeDashboardShortcut.RESERVATION -> Icons.Default.BookmarkAdd
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colorFromHex("E8DFE7")),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorFromHex("FCE6EF")
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorFromHex("EF6797"),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = stringResource(when (shortcut) {
                    CafeDashboardShortcut.CAST_MANAGEMENT -> Res.string.dashboard_shortcut_cast_management
                    CafeDashboardShortcut.CAST_SCHEDULE -> Res.string.dashboard_shortcut_cast_schedule
                    CafeDashboardShortcut.EVENT_MANAGEMENT -> Res.string.dashboard_shortcut_event_management
                    CafeDashboardShortcut.CAFE_SETTINGS -> Res.string.dashboard_shortcut_cafe_settings
                    CafeDashboardShortcut.MENU_GOODS -> Res.string.dashboard_shortcut_menu_goods
                    CafeDashboardShortcut.HOME_BANNER -> Res.string.dashboard_shortcut_home_banner
                    CafeDashboardShortcut.EXTERNAL_LINKS -> Res.string.dashboard_shortcut_external_links
                    CafeDashboardShortcut.SOCIAL_MEDIA -> Res.string.dashboard_shortcut_social_media
                    CafeDashboardShortcut.RESERVATION -> Res.string.dashboard_shortcut_reservation
                }),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CastManagementSection(
    casts: List<CafeCastPreview>,
    hasMoreCasts: Boolean,
    isLoadingMoreCasts: Boolean,
    onCastManagementClick: () -> Unit,
    onDeleteClick: () -> Unit,
    canDelete: Boolean,
    onScheduleClick: () -> Unit,
    selectedCastId: String?,
    onCastScheduleSelect: (String) -> Unit,
    onLoadMoreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colorFromHex("E8DFE7"))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.dashboard_section_cast_management),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("8C7A83")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteClick,
                        enabled = canDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = colorFromHex("FCE6EF"),
                            contentColor = colorFromHex("EF6797"),
                            disabledContainerColor = colorFromHex("F6EEF2"),
                            disabledContentColor = colorFromHex("C8B7C0")
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.dashboard_accessibility_cast_delete)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = colorFromHex("FCE6EF"),
                        onClick = onScheduleClick
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = colorFromHex("EF6797"),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(Res.string.dashboard_action_schedule_management),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorFromHex("EF6797"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AddCastItem(onClick = onCastManagementClick)
                }
                items(casts, key = { it.id }) { cast ->
                    CastPreviewItem(
                        cast = cast,
                        isSelected = selectedCastId == cast.id,
                        onClick = { onCastScheduleSelect(cast.id) }
                    )
                }
                if (hasMoreCasts) {
                    item {
                        LoadMoreCastItem(
                            isLoading = isLoadingMoreCasts,
                            onClick = onLoadMoreClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastPreviewItem(
    cast: CafeCastPreview,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(78.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    if (isSelected) 2.dp else 0.dp,
                    if (isSelected) colorFromHex("EF6797") else Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(72.dp)
                )
            }
            Box(
                    modifier = Modifier
                        .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(colorFromHex("FFD7E3"), colorFromHex("FFF0F5"))
                                ),
                                shape = CircleShape
                            )
                    )
                    if (!cast.profileImage.isNullOrBlank()) {
                        CompatImageDisplay(
                            imageUrl = cast.profileImage,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                        )
                    }
                }
                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp),
                        shape = CircleShape,
                        color = colorFromHex("EF6797")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(16.dp)
                        .background(
                            if (cast.isOnShift) colorFromHex("35C26B") else colorFromHex("C7CBD3"),
                            CircleShape
                        )
                )
            }
        }
        Text(
            text = cast.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) colorFromHex("EF6797") else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LoadMoreCastItem(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = colorFromHex("F7F2F6"),
            border = BorderStroke(1.dp, colorFromHex("E3DCE3")),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colorFromHex("B8AEB7")
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(Res.string.dashboard_accessibility_cast_load_more),
                        tint = colorFromHex("8F848F")
                    )
                }
            }
        }
        Text(
            text = if (isLoading) stringResource(Res.string.dashboard_action_loading) else stringResource(Res.string.dashboard_action_load_more),
            style = MaterialTheme.typography.labelMedium,
            color = colorFromHex("8F848F"),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AddCastItem(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, colorFromHex("E3DCE3")),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.dashboard_accessibility_cast_add),
                    tint = colorFromHex("B8AEB7")
                )
            }
        }
        Text(
            text = stringResource(Res.string.dashboard_action_add),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colorFromHex("8F848F")
        )
    }
}

@Composable
private fun HomeBannerSection(
    banner: CafeDashboardData.HomeBannerPreview,
    onBannerClick: () -> Unit,
    onCreateBannerClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colorFromHex("E8DFE7"))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.dashboard_section_home_banner),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("8C7A83")
                )
                TextButton(onClick = onBannerClick) {
                    Text(stringResource(Res.string.dashboard_action_view_all))
                }
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, colorFromHex("F0E6EC"))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imageUrl = banner.imageUrl?.trim().takeUnless { it.isNullOrEmpty() }

                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colorFromHex("FFD1DC"), colorFromHex("FFE4EC"))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl != null) {
                                CompatImageDisplay(
                                    imageUrl = imageUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    applyRoundedClip = false
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (banner.period.startsWith("dashboard_banner_period_days:")) {
                                    val days = banner.period.substringAfter(':').toIntOrNull() ?: 0
                                    stringResource(Res.string.dashboard_banner_period_days, days)
                                } else {
                                    banner.period
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorFromHex("7E7480")
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = colorFromHex("E8F7EE")
                            ) {
                                Text(
                                    text = when (banner.statusLabel) {
                                        "dashboard_banner_status_active" -> stringResource(Res.string.dashboard_banner_status_active)
                                        "dashboard_banner_status_scheduled" -> stringResource(Res.string.dashboard_banner_status_scheduled)
                                        "dashboard_banner_status_hidden" -> stringResource(Res.string.dashboard_banner_status_hidden)
                                        else -> banner.statusLabel
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorFromHex("2F8B57"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onCreateBannerClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorFromHex("FFD1DC"),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(Res.string.dashboard_action_create_banner),
                            modifier = Modifier.padding(start = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colorFromHex("786E7A")
        )
    }
}

private fun formatRating(rating: Double): String {
    return if (rating <= 0) {
        "-"
    } else {
        val normalized = (rating * 10).toInt() / 10.0
        normalized.toString()
    }
}
