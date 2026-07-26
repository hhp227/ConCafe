package com.hhp227.concafe.presentation.main.cafemanagement.banneredit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.fixedBottomBarInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.banner_action_ok
import concafe.composeapp.generated.resources.banner_content_back
import concafe.composeapp.generated.resources.banneredit_action_close
import concafe.composeapp.generated.resources.banneredit_action_save
import concafe.composeapp.generated.resources.banneredit_alert_image_message
import concafe.composeapp.generated.resources.banneredit_alert_image_title
import concafe.composeapp.generated.resources.banneredit_display_days
import concafe.composeapp.generated.resources.banneredit_error_network_failed
import concafe.composeapp.generated.resources.banneredit_error_permission_denied
import concafe.composeapp.generated.resources.banneredit_error_save_unknown
import concafe.composeapp.generated.resources.banneredit_error_target_not_found
import concafe.composeapp.generated.resources.banneredit_error_unauthorized
import concafe.composeapp.generated.resources.banneredit_image_button
import concafe.composeapp.generated.resources.banneredit_image_guide
import concafe.composeapp.generated.resources.banneredit_image_placeholder_pick
import concafe.composeapp.generated.resources.banneredit_image_section_title
import concafe.composeapp.generated.resources.banneredit_info_edit_banner_load_failed
import concafe.composeapp.generated.resources.banneredit_info_edit_banner_not_found
import concafe.composeapp.generated.resources.banneredit_info_event_list_load_failed
import concafe.composeapp.generated.resources.banneredit_info_image_upload_failed
import concafe.composeapp.generated.resources.banneredit_info_notice_list_load_failed
import concafe.composeapp.generated.resources.banneredit_info_owned_cafe_load_failed
import concafe.composeapp.generated.resources.banneredit_info_saved
import concafe.composeapp.generated.resources.banneredit_info_select_cafe_first
import concafe.composeapp.generated.resources.banneredit_info_slot_full
import concafe.composeapp.generated.resources.banneredit_label_applied_cafe
import concafe.composeapp.generated.resources.banneredit_label_external_link
import concafe.composeapp.generated.resources.banneredit_label_owned_cafe
import concafe.composeapp.generated.resources.banneredit_label_search
import concafe.composeapp.generated.resources.banneredit_label_subtitle
import concafe.composeapp.generated.resources.banneredit_label_title
import concafe.composeapp.generated.resources.banneredit_period_max_day
import concafe.composeapp.generated.resources.banneredit_period_min_day
import concafe.composeapp.generated.resources.banneredit_placeholder_no_applied_cafe
import concafe.composeapp.generated.resources.banneredit_placeholder_select_owned_cafe
import concafe.composeapp.generated.resources.banneredit_placeholder_subtitle
import concafe.composeapp.generated.resources.banneredit_placeholder_title
import concafe.composeapp.generated.resources.banneredit_screen_title_create
import concafe.composeapp.generated.resources.banneredit_screen_title_edit
import concafe.composeapp.generated.resources.banneredit_section_basic
import concafe.composeapp.generated.resources.banneredit_section_period
import concafe.composeapp.generated.resources.banneredit_section_target
import concafe.composeapp.generated.resources.banneredit_selector_empty
import concafe.composeapp.generated.resources.banneredit_selector_search_cafe
import concafe.composeapp.generated.resources.banneredit_selector_search_event
import concafe.composeapp.generated.resources.banneredit_selector_search_notice
import concafe.composeapp.generated.resources.banneredit_selector_title_cafe
import concafe.composeapp.generated.resources.banneredit_selector_title_event
import concafe.composeapp.generated.resources.banneredit_selector_title_notice
import concafe.composeapp.generated.resources.banneredit_submit_create
import concafe.composeapp.generated.resources.banneredit_submit_edit
import concafe.composeapp.generated.resources.banneredit_target_cafe_detail
import concafe.composeapp.generated.resources.banneredit_target_event_detail
import concafe.composeapp.generated.resources.banneredit_target_event_select_label
import concafe.composeapp.generated.resources.banneredit_target_event_select_placeholder
import concafe.composeapp.generated.resources.banneredit_target_external_link
import concafe.composeapp.generated.resources.banneredit_target_notice
import concafe.composeapp.generated.resources.banneredit_target_notice_select_label
import concafe.composeapp.generated.resources.banneredit_target_notice_select_placeholder
import concafe.composeapp.generated.resources.banneredit_target_placeholder_external
import concafe.composeapp.generated.resources.banneredit_validation_external_url_required
import concafe.composeapp.generated.resources.banneredit_validation_image_required
import concafe.composeapp.generated.resources.banneredit_validation_subtitle_required
import concafe.composeapp.generated.resources.banneredit_validation_target_required
import concafe.composeapp.generated.resources.banneredit_validation_title_required
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import com.hhp227.concafe.presentation.component.ConCafeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerEditScreen(
    initialCafeId: String? = null,
    initialBannerId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: BannerEditViewModel = viewModel(
        key = "banner-edit-${initialCafeId.orEmpty()}-${initialBannerId.orEmpty()}",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<BannerEditViewModel> { parametersOf(initialCafeId, initialBannerId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                BannerEditEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                BannerEditEvent.ShowSaveSuccessMessage -> {
                    snackbarHostState.showSnackbar(getString(Res.string.banneredit_info_saved))
                }
            }
        }
    }
    BannerEditContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
    uiState.selectorType?.let {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(BannerEditAction.DismissSelector) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            BannerSelectorSheet(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
    if (uiState.isImageRequiredAlertVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(BannerEditAction.DismissImageRequiredAlert) },
            title = { Text(stringResource(Res.string.banneredit_alert_image_title)) },
            text = { Text(stringResource(Res.string.banneredit_alert_image_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(BannerEditAction.DismissImageRequiredAlert) }) {
                    Text(stringResource(Res.string.banner_action_ok))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerEditContentScreen(
    uiState: BannerEditUiState,
    onAction: (BannerEditAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) {
                            stringResource(Res.string.banneredit_screen_title_edit)
                        } else {
                            stringResource(Res.string.banneredit_screen_title_create)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(BannerEditAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.banner_content_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(BannerEditAction.ClickSave) },
                        enabled = uiState.isSaveEnabled
                    ) {
                        Text(stringResource(Res.string.banneredit_action_save), fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = if (isSystemInDarkTheme()) ConCafeColors.background else Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, ConCafeColors.primaryContainer.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fixedBottomBarInsets()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onAction(BannerEditAction.ClickSave) },
                        enabled = uiState.isSaveEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConCafeColors.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = ConCafeColors.primaryContainer,
                            disabledContentColor = ConCafeColors.textMuted
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                        Text(
                            text = if (uiState.isEditMode) {
                                stringResource(Res.string.banneredit_submit_edit)
                            } else {
                                stringResource(Res.string.banneredit_submit_create)
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSystemInDarkTheme()) {
                        Modifier.background(ConCafeColors.background)
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                            )
                        )
                    }
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CompatImagePicker(
                        onImageSelected = { imageUrl ->
                            onAction(BannerEditAction.SelectImage(imageUrl))
                        }
                    ) { launchImagePicker ->
                        BannerImageCard(
                            uiState = uiState,
                            onClick = {
                                onAction(BannerEditAction.ClickImagePicker)
                                launchImagePicker()
                            }
                        )
                    }
                }
                item {
                    BannerSectionCard(title = stringResource(Res.string.banneredit_section_basic)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ConCafeFormField(
                                label = stringResource(Res.string.banneredit_label_title),
                                value = uiState.title,
                                onValueChange = { onAction(BannerEditAction.ChangeTitle(it)) },
                                placeholder = stringResource(Res.string.banneredit_placeholder_title)
                            )
                            ConCafeFormField(
                                label = stringResource(Res.string.banneredit_label_subtitle),
                                value = uiState.subtitle,
                                onValueChange = { onAction(BannerEditAction.ChangeSubtitle(it)) },
                                placeholder = stringResource(Res.string.banneredit_placeholder_subtitle)
                            )
                        }
                    }
                }
                item {
                    BannerSectionCard(title = stringResource(Res.string.banneredit_section_target)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TargetTypeGrid(
                                selectedTarget = uiState.selectedTarget,
                                onSelect = { onAction(BannerEditAction.SelectTarget(it)) }
                            )
                            when (uiState.selectedTarget) {
                                BannerTargetType.EXTERNAL_LINK -> {
                                    ConCafeFormField(
                                        label = stringResource(Res.string.banneredit_label_external_link),
                                        value = uiState.targetValue,
                                        onValueChange = { onAction(BannerEditAction.ChangeTargetValue(it)) },
                                        placeholder = when (uiState.targetFieldPlaceholderKey) {
                                            "banneredit_target_placeholder_external" -> stringResource(Res.string.banneredit_target_placeholder_external)
                                            else -> uiState.targetFieldPlaceholderKey
                                        }
                                    )
                                }
                                BannerTargetType.CAFE_DETAIL -> {
                                    if (uiState.isAdmin) {
                                        SelectionFieldCard(
                                            label = stringResource(Res.string.banneredit_label_owned_cafe),
                                            selectedTitle = uiState.selectedCafeOption?.name,
                                            selectedSubtitle = uiState.selectedCafeOption?.city,
                                            placeholder = stringResource(Res.string.banneredit_placeholder_select_owned_cafe),
                                            onClick = { onAction(BannerEditAction.ClickCafeSelector) }
                                        )
                                    } else {
                                        FixedSelectionCard(
                                            label = stringResource(Res.string.banneredit_label_applied_cafe),
                                            selectedTitle = uiState.selectedCafeOption?.name,
                                            selectedSubtitle = uiState.selectedCafeOption?.city,
                                            placeholder = stringResource(Res.string.banneredit_placeholder_no_applied_cafe)
                                        )
                                    }
                                }
                                BannerTargetType.NOTICE,
                                BannerTargetType.EVENT_DETAIL -> {
                                    if (uiState.isAdmin) {
                                        SelectionFieldCard(
                                            label = stringResource(Res.string.banneredit_label_owned_cafe),
                                            selectedTitle = uiState.selectedCafeOption?.name,
                                            selectedSubtitle = uiState.selectedCafeOption?.city,
                                            placeholder = stringResource(Res.string.banneredit_placeholder_select_owned_cafe),
                                            onClick = { onAction(BannerEditAction.ClickCafeSelector) }
                                        )
                                    } else {
                                        FixedSelectionCard(
                                            label = stringResource(Res.string.banneredit_label_applied_cafe),
                                            selectedTitle = uiState.selectedCafeOption?.name,
                                            selectedSubtitle = uiState.selectedCafeOption?.city,
                                            placeholder = stringResource(Res.string.banneredit_placeholder_no_applied_cafe)
                                        )
                                    }
                                    SelectionFieldCard(
                                        label = when (uiState.targetSelectionLabelKey) {
                                            "banneredit_target_notice_select_label" -> stringResource(Res.string.banneredit_target_notice_select_label)
                                            "banneredit_target_event_select_label" -> stringResource(Res.string.banneredit_target_event_select_label)
                                            else -> uiState.targetSelectionLabelKey
                                        },
                                        selectedTitle = uiState.selectedContentTitle,
                                        selectedSubtitle = uiState.selectedContentSubtitle,
                                        placeholder = when (uiState.targetSelectionPlaceholderKey) {
                                            "banneredit_target_notice_select_placeholder" -> stringResource(Res.string.banneredit_target_notice_select_placeholder)
                                            "banneredit_target_event_select_placeholder" -> stringResource(Res.string.banneredit_target_event_select_placeholder)
                                            else -> uiState.targetSelectionPlaceholderKey
                                        },
                                        onClick = { onAction(BannerEditAction.ClickTargetSelector) }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    BannerSectionCard(title = stringResource(Res.string.banneredit_section_period)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeText(label = stringResource(Res.string.banneredit_display_days, uiState.displayDaysLabelValue))
                            Slider(
                                value = uiState.displayDays.toFloat(),
                                onValueChange = { onAction(BannerEditAction.ChangeDisplayDays(it.toInt())) },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(Res.string.banneredit_period_min_day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(Res.string.banneredit_period_max_day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = when (message) {
                                "banneredit_info_slot_full" -> stringResource(Res.string.banneredit_info_slot_full)
                                "banneredit_info_saved" -> stringResource(Res.string.banneredit_info_saved)
                                "banneredit_info_owned_cafe_load_failed" -> stringResource(Res.string.banneredit_info_owned_cafe_load_failed)
                                "banneredit_info_edit_banner_not_found" -> stringResource(Res.string.banneredit_info_edit_banner_not_found)
                                "banneredit_info_edit_banner_load_failed" -> stringResource(Res.string.banneredit_info_edit_banner_load_failed)
                                "banneredit_info_select_cafe_first" -> stringResource(Res.string.banneredit_info_select_cafe_first)
                                "banneredit_info_notice_list_load_failed" -> stringResource(Res.string.banneredit_info_notice_list_load_failed)
                                "banneredit_info_event_list_load_failed" -> stringResource(Res.string.banneredit_info_event_list_load_failed)
                                "banneredit_info_image_upload_failed" -> stringResource(Res.string.banneredit_info_image_upload_failed)
                                "banneredit_validation_image_required" -> stringResource(Res.string.banneredit_validation_image_required)
                                "banneredit_validation_title_required" -> stringResource(Res.string.banneredit_validation_title_required)
                                "banneredit_validation_subtitle_required" -> stringResource(Res.string.banneredit_validation_subtitle_required)
                                "banneredit_validation_external_url_required" -> stringResource(Res.string.banneredit_validation_external_url_required)
                                "banneredit_validation_target_required" -> stringResource(Res.string.banneredit_validation_target_required)
                                "banneredit_error_unauthorized" -> stringResource(Res.string.banneredit_error_unauthorized)
                                "banneredit_error_permission_denied" -> stringResource(Res.string.banneredit_error_permission_denied)
                                "banneredit_error_target_not_found" -> stringResource(Res.string.banneredit_error_target_not_found)
                                "banneredit_error_network_failed" -> stringResource(Res.string.banneredit_error_network_failed)
                                "banneredit_error_save_unknown" -> stringResource(Res.string.banneredit_error_save_unknown)
                                else -> message
                            },
                            onDismiss = { onAction(BannerEditAction.DismissInfoMessage) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerSelectorSheet(
    uiState: BannerEditUiState,
    onAction: (BannerEditAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (uiState.selectorTitleKey) {
                "banneredit_selector_title_cafe" -> stringResource(Res.string.banneredit_selector_title_cafe)
                "banneredit_selector_title_notice" -> stringResource(Res.string.banneredit_selector_title_notice)
                "banneredit_selector_title_event" -> stringResource(Res.string.banneredit_selector_title_event)
                else -> uiState.selectorTitleKey
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        ConCafeFormField(
            label = stringResource(Res.string.banneredit_label_search),
            value = uiState.selectorQuery,
            onValueChange = { onAction(BannerEditAction.ChangeSelectorQuery(it)) },
            modifier = Modifier.padding(horizontal = 24.dp),
            placeholder = when (uiState.selectorSearchPlaceholderKey) {
                "banneredit_selector_search_cafe" -> stringResource(Res.string.banneredit_selector_search_cafe)
                "banneredit_selector_search_notice" -> stringResource(Res.string.banneredit_selector_search_notice)
                "banneredit_selector_search_event" -> stringResource(Res.string.banneredit_selector_search_event)
                else -> uiState.selectorSearchPlaceholderKey
            }
        )
        if (uiState.isSelectorLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ConCafeColors.primary)
            }
        } else if (uiState.activeSelectorItemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.banneredit_selector_empty),
                    color = ConCafeColors.textMuted
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (uiState.selectorType) {
                    BannerSelectorType.CAFE -> {
                        items(uiState.filteredCafeSelectorOptions, key = { it.id }) { item ->
                            SelectorOptionCard(
                                title = item.name,
                                subtitle = item.city,
                                onClick = { onAction(BannerEditAction.SelectSelectorItem(item.id)) }
                            )
                        }
                    }
                    BannerSelectorType.NOTICE -> {
                        items(uiState.noticeSelectorOptions, key = { it.id }) { item ->
                            SelectorOptionCard(
                                title = item.title,
                                subtitle = item.displayDate,
                                onClick = { onAction(BannerEditAction.SelectSelectorItem(item.id)) }
                            )
                        }
                    }
                    BannerSelectorType.EVENT -> {
                        items(uiState.eventSelectorOptions, key = { it.id }) { item ->
                            SelectorOptionCard(
                                title = item.title,
                                subtitle = item.periodText,
                                onClick = { onAction(BannerEditAction.SelectSelectorItem(item.id)) }
                            )
                        }
                    }
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun BannerImageCard(
    uiState: BannerEditUiState,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(
                        Brush.linearGradient(listOf(ConCafeColors.primaryContainer, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))),
                        RoundedCornerShape(20.dp)
                    )
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.selectedImageLabel.isNullOrBlank()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = ConCafeColors.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(Res.string.banneredit_image_placeholder_pick),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ConCafeColors.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    CompatImageDisplay(
                        imageUrl = uiState.selectedImageLabel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.banneredit_image_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.banneredit_image_guide),
                    style = MaterialTheme.typography.bodySmall,
                    color = ConCafeColors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConCafeColors.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    stringResource(Res.string.banneredit_image_button),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BannerSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(width = 4.dp, height = 18.dp)
                        .background(ConCafeColors.primaryContainer, RoundedCornerShape(999.dp))
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun TargetTypeGrid(
    selectedTarget: BannerTargetType,
    onSelect: (BannerTargetType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BannerTargetType.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { target ->
                    val isSelected = selectedTarget == target

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(target) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) ConCafeColors.primaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ConCafeColors.primaryContainer else ConCafeColors.primaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (target.label) {
                                    "banneredit_target_cafe_detail" -> stringResource(Res.string.banneredit_target_cafe_detail)
                                    "banneredit_target_event_detail" -> stringResource(Res.string.banneredit_target_event_detail)
                                    "banneredit_target_notice" -> stringResource(Res.string.banneredit_target_notice)
                                    "banneredit_target_external_link" -> stringResource(Res.string.banneredit_target_external_link)
                                    else -> target.label
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionFieldCard(
    label: String,
    selectedTitle: String?,
    selectedSubtitle: String?,
    placeholder: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, ConCafeColors.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedTitle ?: placeholder,
                        color = if (selectedTitle == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selectedTitle == null) FontWeight.Normal else FontWeight.SemiBold
                    )
                    selectedSubtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ConCafeColors.textMuted
                        )
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ConCafeColors.textMuted)
            }
        }
    }
}

@Composable
private fun FixedSelectionCard(
    label: String,
    selectedTitle: String?,
    selectedSubtitle: String?,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, ConCafeColors.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedTitle ?: placeholder,
                    color = if (selectedTitle == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selectedTitle == null) FontWeight.Normal else FontWeight.SemiBold
                )
                selectedSubtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ConCafeColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ConCafeColors.textMuted)
        }
    }
}

@Composable
private fun BadgeText(label: String) {
    Surface(
        color = ConCafeColors.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = ConCafeColors.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ConCafeColors.primaryContainer.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = ConCafeColors.primary, modifier = Modifier.padding(top = 2.dp))
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.banneredit_action_close), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BannerEditContentPreview() {
    BannerEditContentScreen(
        uiState = BannerEditUiState(
            ownedCafeOptions = listOf(
                CafeManagementData.OwnedCafeSummary(
                    id = "cafe-1",
                    name = "Luna Maid Cafe",
                    city = "Seoul",
                    isApproved = true,
                    todayVisitors = 0,
                    todayCheckIns = 0,
                    todayReviews = 0,
                    rating = 0.0,
                    castCount = 0,
                    noticeCount = 0,
                    externalLinkCount = 0,
                    thumbnailImage = null
                )
            ),
            selectedCafeId = "cafe-1"
        ),
        onAction = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}

