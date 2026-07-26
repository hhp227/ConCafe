package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.RatingUtils
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.model.CheckInCastSummary
import com.hhp227.concafe.domain.model.CheckInVisitEntry
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.presentation.component.CafeSummaryCard
import com.hhp227.concafe.presentation.component.CheckInCafeMap
import com.hhp227.concafe.presentation.component.CheckInMapCameraTarget
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.ShimmerCardListSkeleton
import com.hhp227.concafe.presentation.component.ShimmerListSkeleton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.localizedRegionCity
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.main.explore.ExploreUiState
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.auth_login_required_message
import concafe.composeapp.generated.resources.auth_login_required_title
import concafe.composeapp.generated.resources.checkin_button
import concafe.composeapp.generated.resources.checkin_count_label
import concafe.composeapp.generated.resources.checkin_load_more_visits
import concafe.composeapp.generated.resources.checkin_location_permission_desc
import concafe.composeapp.generated.resources.checkin_location_permission_open_settings
import concafe.composeapp.generated.resources.checkin_location_permission_title
import concafe.composeapp.generated.resources.checkin_login_promo_feature_badge
import concafe.composeapp.generated.resources.checkin_login_promo_feature_fan_level
import concafe.composeapp.generated.resources.checkin_login_promo_feature_visit
import concafe.composeapp.generated.resources.checkin_login_promo_title
import concafe.composeapp.generated.resources.checkin_login_required_desc
import concafe.composeapp.generated.resources.checkin_login_required_title
import concafe.composeapp.generated.resources.checkin_main_cafe_label
import concafe.composeapp.generated.resources.checkin_map_title
import concafe.composeapp.generated.resources.checkin_more_visit_label
import concafe.composeapp.generated.resources.checkin_nearby_label
import concafe.composeapp.generated.resources.checkin_new_visit_cafe_label
import concafe.composeapp.generated.resources.checkin_new_visit_cafe_unavailable_placeholder
import concafe.composeapp.generated.resources.checkin_new_visit_cta
import concafe.composeapp.generated.resources.checkin_new_visit_desc
import concafe.composeapp.generated.resources.checkin_new_visit_memo_label
import concafe.composeapp.generated.resources.checkin_new_visit_memo_placeholder
import concafe.composeapp.generated.resources.checkin_new_visit_no_cafe
import concafe.composeapp.generated.resources.checkin_new_visit_submit
import concafe.composeapp.generated.resources.checkin_new_visit_title
import concafe.composeapp.generated.resources.checkin_partial_load_error
import concafe.composeapp.generated.resources.checkin_popular_cafe_empty_desc
import concafe.composeapp.generated.resources.checkin_popular_cafe_empty_title
import concafe.composeapp.generated.resources.checkin_popular_cast_empty_desc
import concafe.composeapp.generated.resources.checkin_popular_cast_empty_title
import concafe.composeapp.generated.resources.checkin_qr_sheet_desc
import concafe.composeapp.generated.resources.checkin_qr_sheet_title
import concafe.composeapp.generated.resources.checkin_review_prompt_desc
import concafe.composeapp.generated.resources.checkin_review_prompt_later
import concafe.composeapp.generated.resources.checkin_review_prompt_primary
import concafe.composeapp.generated.resources.checkin_review_prompt_title
import concafe.composeapp.generated.resources.checkin_section_popular_cafe_title
import concafe.composeapp.generated.resources.checkin_section_popular_cast_title
import concafe.composeapp.generated.resources.checkin_section_timeline_title
import concafe.composeapp.generated.resources.checkin_section_today_visit_title
import concafe.composeapp.generated.resources.checkin_timeline_empty_desc
import concafe.composeapp.generated.resources.checkin_timeline_empty_title
import concafe.composeapp.generated.resources.checkin_today_visit_count
import concafe.composeapp.generated.resources.checkin_today_visit_empty_desc
import concafe.composeapp.generated.resources.checkin_today_visit_empty_title
import concafe.composeapp.generated.resources.checkin_visit_memo_empty
import concafe.composeapp.generated.resources.checkin_visit_qr_label
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.signin_submit
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CheckInViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val openLocationSettings = rememberCheckInLocationSettingsOpener()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLocationSettingsAlertVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CheckInEvent.NavigateToCafe -> onNavigate(NavigationAction.NavigateToCafe(event.id))
                is CheckInEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
                is CheckInEvent.NavigateToReviewEdit -> onNavigate(NavigationAction.NavigateToReviewEdit(event.cafeId))
                CheckInEvent.NavigateToMap -> {
                    onNavigate(
                        NavigationAction.NavigateToCheckInMap(
                            initialRegionKey = uiState.selectedMapRegion.name
                        )
                    )
                }
                CheckInEvent.NavigateToSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                CheckInEvent.OpenLocationSettings -> {
                    isLocationSettingsAlertVisible = true
                }
                is CheckInEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CheckInContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
        if (uiState.isLoginPromptVisible && uiState.loginPromptType == CheckInUiState.LoginPromptType.CHECK_IN) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(CheckInAction.DismissLoginPrompt) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                LoginRequiredBottomSheet(
                    onSignIn = { viewModel.onAction(CheckInAction.ClickSignIn) }
                )
            }
        }
        if (uiState.isLoginPromptVisible && uiState.loginPromptType == CheckInUiState.LoginPromptType.DETAIL) {
            AlertDialog(
                onDismissRequest = { viewModel.onAction(CheckInAction.DismissLoginPrompt) },
                title = { Text(stringResource(Res.string.auth_login_required_title)) },
                text = { Text(stringResource(Res.string.auth_login_required_message)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onAction(CheckInAction.ClickSignIn) }) {
                        Text(stringResource(Res.string.signin_submit))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onAction(CheckInAction.DismissLoginPrompt) }) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                }
            )
        }
        if (uiState.isNewVisitSheetVisible) {
            CheckInNewVisitDialog(
                onDismissRequest = { viewModel.onAction(CheckInAction.DismissNewVisitSheet) }
            ) {
                NewVisitCheckInBottomSheet(
                    cafes = uiState.mapCafes,
                    initialCafeId = uiState.preselectCafeId,
                    errorMessage = uiState.errorMessage,
                    onSubmit = { cafeId, visitedAt, memo ->
                        viewModel.onAction(
                            CheckInAction.SubmitNewVisit(
                                cafeId = cafeId,
                                visitedAt = visitedAt,
                                memo = memo
                            )
                        )
                    },
                    onQrCheckIn = { viewModel.onAction(CheckInAction.ClickQrCheckIn) },
                    onDismiss = { viewModel.onAction(CheckInAction.DismissNewVisitSheet) }
                )
            }
        }
        if (uiState.isQrCheckInSheetVisible) {
            CheckInNewVisitDialog(
                onDismissRequest = { viewModel.onAction(CheckInAction.DismissQrCheckInSheet) }
            ) {
                QrCheckInBottomSheet(
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.onAction(CheckInAction.DismissQrCheckInSheet) },
                    onScanSuccess = { rawValue ->
                        viewModel.onAction(CheckInAction.SubmitQrCheckIn(rawValue))
                    },
                    onScanFailed = { message ->
                        viewModel.onAction(CheckInAction.QrScanFailed(message))
                    }
                )
            }
        }
        uiState.reviewPrompt?.let { prompt ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(CheckInAction.DismissReviewPrompt) },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReviewPromptBottomSheet(
                    cafeName = prompt.cafeName,
                    onWriteReview = { viewModel.onAction(CheckInAction.ClickWriteReviewPrompt) },
                    onDismiss = { viewModel.onAction(CheckInAction.DismissReviewPrompt) }
                )
            }
        }
        if (isLocationSettingsAlertVisible) {
            AlertDialog(
                onDismissRequest = { isLocationSettingsAlertVisible = false },
                title = { Text(stringResource(Res.string.checkin_location_permission_title)) },
                text = { Text(stringResource(Res.string.checkin_location_permission_desc)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isLocationSettingsAlertVisible = false
                            openLocationSettings()
                        }
                    ) {
                        Text(stringResource(Res.string.checkin_location_permission_open_settings))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { isLocationSettingsAlertVisible = false }
                    ) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun CheckInNewVisitDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(520.dp)
                    .keyboardBottomInsets(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ReviewPromptBottomSheet(
    cafeName: String,
    onWriteReview: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(Res.string.checkin_review_prompt_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(Res.string.checkin_review_prompt_desc, cafeName),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onWriteReview,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConCafeColors.primaryContainer,
                contentColor = ConCafeColors.textPrimary
            )
        ) {
            Text(stringResource(Res.string.checkin_review_prompt_primary), fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.checkin_review_prompt_later))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CheckInContentScreen(
    uiState: CheckInUiState,
    onAction: (CheckInAction) -> Unit
) {
    when {
        uiState.isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                ShimmerCardListSkeleton(itemCount = 1, imageHeight = 220.dp)
                ShimmerListSkeleton(itemCount = 4)
            }
        }
        uiState.currentUser == null -> {
            CheckInGuestScreen(
                uiState = uiState,
                onAction = onAction
            )
        }
        else -> {
            CheckInUserScreen(
                uiState = uiState,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun CheckInGuestScreen(
    uiState: CheckInUiState,
    onAction: (CheckInAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CafeMapSection(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                currentLocationLabel = uiState.currentLocationLabel,
                mapCafes = uiState.mapCafes,
                userCityKey = uiState.userCityKey,
                selectedRegion = uiState.selectedMapRegion,
                onCafeClick = { onAction(CheckInAction.ClickCafe(it)) },
                onCafeCheckIn = { onAction(CheckInAction.ClickCheckInForCafe(it)) },
                onCheckInClick = { onAction(CheckInAction.ClickCheckIn) },
                onRegionSelected = { onAction(CheckInAction.UpdateMapRegion(it)) },
                onExpandClick = { onAction(CheckInAction.ClickMapFullView) }
            )
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LoginPromotionSection(
                    onSignIn = { onAction(CheckInAction.ClickSignIn) },
                    onSignUp = { onAction(CheckInAction.ClickSignUp) }
                )
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CheckInGuestSectionTitle(
                    title = stringResource(Res.string.checkin_section_popular_cafe_title)
                )
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (uiState.popularCafes.isNotEmpty()) {
                    PopularCafeSection(
                        cafes = uiState.popularCafes,
                        onCafeClick = { onAction(CheckInAction.ClickCafe(it)) }
                    )
                } else {
                    CheckInSectionPlaceholderCard(
                        title = stringResource(Res.string.checkin_popular_cafe_empty_title),
                        description = stringResource(Res.string.checkin_popular_cafe_empty_desc)
                    )
                }
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CheckInGuestSectionTitle(
                    title = stringResource(Res.string.checkin_section_popular_cast_title)
                )
            }
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                if (uiState.popularCasts.isNotEmpty()) {
                    PopularCastSection(
                        casts = uiState.popularCasts,
                        onCastClick = { onAction(CheckInAction.ClickCast(it)) }
                    )
                } else {
                    CheckInSectionPlaceholderCard(
                        title = stringResource(Res.string.checkin_popular_cast_empty_title),
                        description = stringResource(Res.string.checkin_popular_cast_empty_desc)
                    )
                }
            }
            if (uiState.errorMessage != null) {
                Text(
                    text = stringResource(Res.string.checkin_partial_load_error),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CheckInUserScreen(
    uiState: CheckInUiState,
    onAction: (CheckInAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CafeMapSection(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                currentLocationLabel = uiState.currentLocationLabel,
                mapCafes = uiState.mapCafes,
                userCityKey = uiState.userCityKey,
                selectedRegion = uiState.selectedMapRegion,
                onCafeClick = { onAction(CheckInAction.ClickCafe(it)) },
                onCafeCheckIn = { onAction(CheckInAction.ClickCheckInForCafe(it)) },
                onCheckInClick = { onAction(CheckInAction.ClickCheckIn) },
                onRegionSelected = { onAction(CheckInAction.UpdateMapRegion(it)) },
                onExpandClick = { onAction(CheckInAction.ClickMapFullView) }
            )
        }
        item {
            CheckInButton(
                onClick = { onAction(CheckInAction.ClickCheckIn) }
            )
        }
        item {
            CheckInSectionTitle(
                title = stringResource(Res.string.checkin_section_today_visit_title),
                trailing = TimeUtils.currentMonthDayLabelKorean()
            )
        }
        item {
            TodayVisitsRow(
                visits = uiState.todayVisits
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            CheckInSectionTitle(
                title = stringResource(Res.string.checkin_section_timeline_title),
                trailing = null
            )
        }
        if (uiState.recentVisits.isEmpty()) {
            item {
                EmptyVisitState(
                    title = stringResource(Res.string.checkin_timeline_empty_title),
                    description = stringResource(Res.string.checkin_timeline_empty_desc)
                )
            }
        } else {
            itemsIndexed(
                items = uiState.recentVisits,
                key = { _, visit -> visit.id }
            ) { _, visit ->
                TimelineItem(
                    visit = visit,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                if (uiState.isLoadingMoreRecentVisits) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (uiState.canLoadMoreRecentVisits) {
                    TextButton(
                        onClick = { onAction(CheckInAction.LoadMoreRecentVisits) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(stringResource(Res.string.checkin_load_more_visits))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CafeMapSection(
    modifier: Modifier = Modifier,
    currentLocationLabel: String,
    mapCafes: List<CheckInCafeSummary>,
    userCityKey: String?,
    selectedRegion: ExploreUiState.RegionFilter,
    onCafeClick: (String) -> Unit,
    onCafeCheckIn: (String) -> Unit,
    onCheckInClick: () -> Unit,
    onRegionSelected: (ExploreUiState.RegionFilter) -> Unit,
    onExpandClick: () -> Unit = {},
    showExpandButton: Boolean = true,
    showCheckInButton: Boolean = true,
    mapModifier: Modifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
) {
    var isRegionDropdownExpanded by remember { mutableStateOf(false) }
    val usesInlineRegionFilter = useInlineCheckInMapRegionFilter()
    val selectedRegionLabel = stringResource(
        Res.string.checkin_main_cafe_label,
        if (selectedRegion == ExploreUiState.RegionFilter.ALL) {
            stringResource(Res.string.checkin_nearby_label)
        } else {
            selectedRegion.label
        }
    )
    val mapCameraTarget = resolveCheckInMapCameraTarget(selectedRegion)
    val filteredMapCafes = when {
        selectedRegion != ExploreUiState.RegionFilter.ALL -> {
            mapCafes.filter { cafe ->
                cafe.matchesRegion(selectedRegion)
            }
        }
        else -> {
            mapCafes.filter { cafe ->
                cafe.matchesNearbyCity(userCityKey)
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (isSystemInDarkTheme()) {
                            listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                        } else {
                            listOf(ConCafeColors.background, ConCafeColors.background, ConCafeColors.background)
                        }
                    )
                )
                .padding(12.dp)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.checkin_map_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (usesInlineRegionFilter) {
                            Text(
                                text = selectedRegionLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            CheckInRegionDropdown(
                                selectedRegionLabel = selectedRegionLabel,
                                isExpanded = isRegionDropdownExpanded,
                                onExpandedChange = { isRegionDropdownExpanded = it },
                                onRegionSelected = { region ->
                                    onRegionSelected(region)
                                    isRegionDropdownExpanded = false
                                }
                            )
                        }
                    }
                    if (usesInlineRegionFilter) {
                        CheckInRegionChips(
                            selectedRegion = selectedRegion,
                            onRegionSelected = onRegionSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (showCheckInButton) {
                    OutlinedButton(
                        onClick = onCheckInClick,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(stringResource(Res.string.checkin_button))
                    }
                }
                if (showExpandButton && usesInlineRegionFilter) {
                    FilledTonalIconButton(
                        onClick = onExpandClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null
                        )
                    }
                }
            }
            Box(modifier = mapModifier) {
                CheckInCafeMap(
                    cafes = filteredMapCafes,
                    onCafeClick = onCafeClick,
                    onCafeCheckIn = onCafeCheckIn,
                    showCheckInButton = showCheckInButton,
                    cameraTarget = mapCameraTarget,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
                if (showExpandButton && !usesInlineRegionFilter) {
                    FilledTonalIconButton(
                        onClick = onExpandClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInRegionDropdown(
    selectedRegionLabel: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRegionSelected: (ExploreUiState.RegionFilter) -> Unit
) {
    Box {
        Row(
            modifier = Modifier.clickable { onExpandedChange(true) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = ConCafeColors.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = selectedRegionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            ExploreUiState.RegionFilter.entries.forEach { region ->
                DropdownMenuItem(
                    text = { Text(region.checkInMapFilterLabel()) },
                    onClick = { onRegionSelected(region) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CheckInRegionChips(
    selectedRegion: ExploreUiState.RegionFilter,
    onRegionSelected: (ExploreUiState.RegionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = modifier
    ) {
        items(ExploreUiState.RegionFilter.entries) { region ->
            FilterChip(
                selected = selectedRegion == region,
                onClick = { onRegionSelected(region) },
                label = { Text(region.checkInMapFilterLabel()) },
                leadingIcon = if (selectedRegion == region) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun ExploreUiState.RegionFilter.checkInMapFilterLabel(): String {
    return if (this == ExploreUiState.RegionFilter.ALL) {
        stringResource(Res.string.checkin_nearby_label)
    } else {
        label
    }
}

private fun resolveCheckInMapCameraTarget(region: ExploreUiState.RegionFilter): CheckInMapCameraTarget? {
    return when (region) {
        ExploreUiState.RegionFilter.ALL -> null
        ExploreUiState.RegionFilter.SEOUL -> CheckInMapCameraTarget(
            latitude = 37.5665,
            longitude = 126.9780,
            zoom = 12.5f
        )
        ExploreUiState.RegionFilter.BUSAN -> CheckInMapCameraTarget(
            latitude = 35.1796,
            longitude = 129.0756,
            zoom = 12.0f
        )
        ExploreUiState.RegionFilter.DAEGU -> CheckInMapCameraTarget(
            latitude = 35.8714,
            longitude = 128.6014,
            zoom = 12.0f
        )
        ExploreUiState.RegionFilter.TOKYO -> CheckInMapCameraTarget(
            latitude = 35.6762,
            longitude = 139.6503,
            zoom = 12.0f
        )
        ExploreUiState.RegionFilter.OSAKA -> CheckInMapCameraTarget(
            latitude = 34.6937,
            longitude = 135.5023,
            zoom = 12.0f
        )
        ExploreUiState.RegionFilter.ETC -> CheckInMapCameraTarget(
            latitude = 35.4437,
            longitude = 139.6380,
            zoom = 12.0f
        )
    }
}

private fun CheckInCafeSummary.matchesRegion(region: ExploreUiState.RegionFilter): Boolean {
    val normalizedLocation = locationLabel.lowercase()
    return normalizedLocation.contains(region.key) ||
        normalizedLocation.contains(region.label.lowercase()) ||
        geoPoint.matchesRegion(region)
}

private fun CheckInCafeSummary.matchesNearbyCity(cityKey: String?): Boolean {
    if (cityKey.isNullOrBlank()) return true
    val nearbyRegion = ExploreUiState.RegionFilter.entries
        .firstOrNull {
            val normalizedCityKey = cityKey.trim().lowercase()
            it.key == normalizedCityKey || (normalizedCityKey == "yokohama" && it == ExploreUiState.RegionFilter.ETC)
        }
        ?: return true
    return matchesRegion(nearbyRegion)
}

private fun GeoPoint.matchesRegion(region: ExploreUiState.RegionFilter): Boolean {
    return when (region) {
        ExploreUiState.RegionFilter.ALL -> true
        ExploreUiState.RegionFilter.SEOUL -> latitude in 37.4..37.7 && longitude in 126.7..127.2
        ExploreUiState.RegionFilter.BUSAN -> latitude in 35.0..35.4 && longitude in 128.8..129.3
        ExploreUiState.RegionFilter.DAEGU -> latitude in 35.7..36.0 && longitude in 128.4..128.8
        ExploreUiState.RegionFilter.ETC -> latitude in 35.35..35.60 && longitude in 139.50..139.75
        ExploreUiState.RegionFilter.TOKYO -> latitude in 35.5..35.9 && longitude in 139.3..139.9
        ExploreUiState.RegionFilter.OSAKA -> latitude in 34.5..34.9 && longitude in 135.3..135.7
    }
}

@Composable
private fun PopularCafeSection(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cafes.forEach { cafe ->
            PopularCafeCard(
                cafe = cafe,
                onClick = { onCafeClick(cafe.id) }
            )
        }
    }
}

@Composable
private fun PopularCastSection(
    casts: List<CheckInCastSummary>,
    onCastClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        casts.forEach { cast ->
            PopularCastCard(
                cast = cast,
                onClick = { onCastClick(cast.id) }
            )
        }
    }
}

@Composable
private fun PopularCafeCard(
    cafe: CheckInCafeSummary,
    onClick: () -> Unit
) {
    CafeSummaryCard(
        name = cafe.name,
        rating = RatingUtils.formatOneDecimal(cafe.rating),
        location = localizedRegionCity(cafe.locationLabel),
        thumbnailImage = cafe.thumbnailImage,
        showLocationIcon = false,
        modifier = Modifier
            .width(220.dp),
        trailingLabel = stringResource(Res.string.checkin_count_label, cafe.checkInCount),
        onClick = onClick
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularCastCard(
    cast: CheckInCastSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(ConCafeColors.primaryContainer, ConCafeColors.surfaceTint)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cast.name.take(1),
                        color = ConCafeColors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    if (!cast.profileImage.isNullOrBlank()) {
                        CompatImageDisplay(
                            imageUrl = cast.profileImage,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape),
                            applyRoundedClip = false
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = cast.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cast.cafeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isSystemInDarkTheme()) {
                    ConCafeColors.primary.copy(alpha = 0.22f)
                } else {
                    ConCafeColors.surfaceTint
                }
            ) {
                Text(
                    text = stringResource(Res.string.checkin_today_visit_count, cast.todayVisit),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isSystemInDarkTheme()) ConCafeColors.secondaryContainer else ConCafeColors.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LoginPromotionSection(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(ConCafeColors.primary, ConCafeColors.secondaryContainer)
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(Res.string.checkin_login_promo_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.checkin_login_promo_feature_visit), color = Color.White)
                Text(stringResource(Res.string.checkin_login_promo_feature_fan_level), color = Color.White)
                Text(stringResource(Res.string.checkin_login_promo_feature_badge), color = Color.White)
            }
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                    contentColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else ConCafeColors.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(Res.string.signin_submit), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LoginRequiredBottomSheet(
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(Res.string.checkin_login_required_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(Res.string.checkin_login_required_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConCafeColors.primaryContainer,
                contentColor = ConCafeColors.textPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(Res.string.signin_submit), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NewVisitCheckInBottomSheet(
    cafes: List<CheckInCafeSummary>,
    initialCafeId: String? = null,
    errorMessage: String?,
    onSubmit: (String, String, String?) -> Unit,
    onQrCheckIn: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cafeOptions = cafes.map { it.name to it.id }
    var selectedCafeId by remember {
        mutableStateOf(
            if (initialCafeId != null && cafes.any { it.id == initialCafeId }) initialCafeId
            else cafeOptions.firstOrNull()?.second.orEmpty()
        )
    }
    var isCafeDropdownExpanded by remember { mutableStateOf(false) }
    var cafeDropdownWidth by remember { mutableStateOf(0) }
    val selectedCafeName = cafes
        .firstOrNull { it.id == selectedCafeId }
        ?.name
        ?: cafeOptions.firstOrNull()?.first.orEmpty()
    val dropdownInteractionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    var memo by remember { mutableStateOf("") }

    LaunchedEffect(cafes) {
        val fallbackCafeId = if (initialCafeId != null && cafes.any { it.id == initialCafeId }) {
            initialCafeId
        } else {
            cafes.firstOrNull()?.id.orEmpty()
        }

        if (selectedCafeId.isBlank()) {
            selectedCafeId = fallbackCafeId
        } else if (cafes.none { it.id == selectedCafeId }) {
            selectedCafeId = fallbackCafeId
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.checkin_new_visit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.common_close),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = stringResource(Res.string.checkin_new_visit_desc),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        if (cafeOptions.isEmpty()) {
            Text(
                text = stringResource(Res.string.checkin_new_visit_no_cafe),
                color = colorScheme.onSurfaceVariant
            )
            ConCafeFormField(
                label = stringResource(Res.string.checkin_new_visit_cafe_label),
                value = "",
                onValueChange = {},
                placeholder = stringResource(Res.string.checkin_new_visit_cafe_unavailable_placeholder),
                readOnly = true,
                enabled = false
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                ConCafeFormField(
                    label = stringResource(Res.string.checkin_new_visit_cafe_label),
                    value = selectedCafeName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.onSizeChanged { cafeDropdownWidth = it.width },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(Res.string.checkin_new_visit_cafe_label),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = dropdownInteractionSource,
                            indication = null
                        ) { isCafeDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = isCafeDropdownExpanded,
                    onDismissRequest = { isCafeDropdownExpanded = false },
                    modifier = Modifier.then(
                        if (cafeDropdownWidth > 0) {
                            Modifier.width(with(density) { cafeDropdownWidth.toDp() })
                        } else {
                            Modifier
                        }
                    )
                ) {
                    cafes.forEach { cafe ->
                        DropdownMenuItem(
                            text = { Text(cafe.name) },
                            onClick = {
                                selectedCafeId = cafe.id
                                isCafeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
        ConCafeFormField(
            label = stringResource(Res.string.checkin_new_visit_memo_label),
            value = memo,
            onValueChange = { memo = it },
            placeholder = stringResource(Res.string.checkin_new_visit_memo_placeholder),
            modifier = Modifier.height(120.dp),
            minLines = 4,
            singleLine = false
        )
        if (!errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.errorContainer,
                border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onErrorContainer
                    )
                }
            }
        }
        Button(
            enabled = selectedCafeId.isNotBlank(),
            onClick = {
                val normalizedCafeId = selectedCafeId.trim()
                val normalizedMemo = memo.trim().ifEmpty { null }
                val normalizedVisitedAt = Clock.System.now().toString()

                onSubmit(normalizedCafeId, normalizedVisitedAt, normalizedMemo)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConCafeColors.primaryContainer,
                contentColor = ConCafeColors.textPrimary
            )
        ) {
            Text(stringResource(Res.string.checkin_new_visit_submit), fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onQrCheckIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConCafeColors.primaryContainer,
                contentColor = ConCafeColors.textPrimary
            )
        ) {
            Text("QR ${stringResource(Res.string.checkin_button)}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CheckInGuestSectionTitle(
    title: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CheckInSectionTitle(
    title: String,
    trailing: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TodayVisitsRow(
    visits: List<CheckInVisitEntry>
) {
    if (visits.isEmpty()) {
        EmptyVisitState(
            title = stringResource(Res.string.checkin_today_visit_empty_title),
            description = stringResource(Res.string.checkin_today_visit_empty_desc)
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (visits.size) {
                1 -> {
                    CheckInVisitCard(
                        name = visits[0].cafeName,
                        time = visits[0].visitedLabel,
                        image = visits[0].cafeImage,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    CheckInVisitCard(
                        name = visits[0].cafeName,
                        time = visits[0].visitedLabel,
                        image = visits[0].cafeImage,
                        modifier = Modifier.weight(1f)
                    )
                    MoreVisitCard(
                        remainingCount = visits.size - 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCheckInBottomSheet(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onScanSuccess: (String) -> Unit,
    onScanFailed: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.checkin_qr_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.common_close),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = stringResource(Res.string.checkin_qr_sheet_desc),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            CheckInQrScanner(
                onScanSuccess = onScanSuccess,
                onScanCanceled = onDismiss,
                onScanFailed = onScanFailed
            )
        }
        if (!errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.errorContainer,
                border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.35f))
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun CheckInVisitCard(name: String, time: String, image: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .height(180.dp)
            .clip(shape)
    ) {
        CompatImageDisplay(
            imageUrl = image,
            modifier = Modifier.matchParentSize(),
            applyRoundedClip = false
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Text(
                text = time,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MoreVisitCard(
    remainingCount: Int,
    modifier: Modifier = Modifier
) {

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(ConCafeColors.background, ConCafeColors.surfaceTint)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$remainingCount",
                    color = ConCafeColors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.checkin_more_visit_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CheckInButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor = when {
        isPressed -> ConCafeColors.secondaryContainer
        isHovered -> ConCafeColors.secondaryContainer
        else -> ConCafeColors.primaryContainer
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 64.dp)
            .padding(vertical = 12.dp)
            .height(70.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = ConCafeColors.textPrimary
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(Res.string.checkin_new_visit_cta), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun TimelineItem(
    visit: CheckInVisitEntry,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // 왼쪽 타임라인 선과 아이콘
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ConCafeColors.primaryContainer),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(100.dp)
                    .background(ConCafeColors.primaryContainer.copy(alpha = 0.3f))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        visit.cafeName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = visit.relativeVisitedLabel(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = visit.memo ?: if (visit.checkInMethod?.uppercase() == "QR") {
                        stringResource(Res.string.checkin_visit_qr_label)
                    } else {
                        stringResource(Res.string.checkin_visit_memo_empty)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun CheckInVisitEntry.relativeVisitedLabel(): String {
    return TimeUtils.relativeVisitedLabel(
        visitedAt = visitedAt,
        visitedLabel = visitedLabel,
        referenceDate = TimeUtils.currentIsoDate()
    )
}

@Composable
private fun EmptyVisitState(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckInSectionPlaceholderCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
