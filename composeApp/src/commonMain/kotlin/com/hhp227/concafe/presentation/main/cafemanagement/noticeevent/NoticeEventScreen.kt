package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.NoticeStatusAccent
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.ConCafeTabBar
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.common_confirm
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.menugoods_delete_content_description
import concafe.composeapp.generated.resources.menugoods_edit_content_description
import concafe.composeapp.generated.resources.noticeevent_empty_event
import concafe.composeapp.generated.resources.noticeevent_empty_notice
import concafe.composeapp.generated.resources.noticeevent_form_content_placeholder_event
import concafe.composeapp.generated.resources.noticeevent_form_content_placeholder_notice
import concafe.composeapp.generated.resources.noticeevent_form_image_description
import concafe.composeapp.generated.resources.noticeevent_form_image_label
import concafe.composeapp.generated.resources.noticeevent_form_image_title_attached
import concafe.composeapp.generated.resources.noticeevent_form_image_title_empty
import concafe.composeapp.generated.resources.noticeevent_form_label_content
import concafe.composeapp.generated.resources.noticeevent_form_label_title
import concafe.composeapp.generated.resources.noticeevent_form_schedule_label_event
import concafe.composeapp.generated.resources.noticeevent_form_schedule_label_notice
import concafe.composeapp.generated.resources.noticeevent_form_schedule_placeholder_event
import concafe.composeapp.generated.resources.noticeevent_form_schedule_placeholder_notice
import concafe.composeapp.generated.resources.noticeevent_form_sheet_title_event_create
import concafe.composeapp.generated.resources.noticeevent_form_sheet_title_event_edit
import concafe.composeapp.generated.resources.noticeevent_form_sheet_title_notice_create
import concafe.composeapp.generated.resources.noticeevent_form_sheet_title_notice_edit
import concafe.composeapp.generated.resources.noticeevent_form_submit_event_create
import concafe.composeapp.generated.resources.noticeevent_form_submit_event_edit
import concafe.composeapp.generated.resources.noticeevent_form_submit_notice_create
import concafe.composeapp.generated.resources.noticeevent_form_submit_notice_edit
import concafe.composeapp.generated.resources.noticeevent_form_title_placeholder_event
import concafe.composeapp.generated.resources.noticeevent_form_title_placeholder_notice
import concafe.composeapp.generated.resources.noticeevent_info_event_create_failed
import concafe.composeapp.generated.resources.noticeevent_info_event_created
import concafe.composeapp.generated.resources.noticeevent_info_event_delete_failed
import concafe.composeapp.generated.resources.noticeevent_info_event_delete_success
import concafe.composeapp.generated.resources.noticeevent_info_event_edit_target_not_found
import concafe.composeapp.generated.resources.noticeevent_info_event_load_failed
import concafe.composeapp.generated.resources.noticeevent_info_event_update_failed
import concafe.composeapp.generated.resources.noticeevent_info_event_updated
import concafe.composeapp.generated.resources.noticeevent_info_image_one_only
import concafe.composeapp.generated.resources.noticeevent_info_image_pick_required
import concafe.composeapp.generated.resources.noticeevent_info_image_upload_failed
import concafe.composeapp.generated.resources.noticeevent_info_more_events_next_step
import concafe.composeapp.generated.resources.noticeevent_info_notice_create_failed
import concafe.composeapp.generated.resources.noticeevent_info_notice_created
import concafe.composeapp.generated.resources.noticeevent_info_notice_delete_failed
import concafe.composeapp.generated.resources.noticeevent_info_notice_delete_success
import concafe.composeapp.generated.resources.noticeevent_info_notice_edit_target_not_found
import concafe.composeapp.generated.resources.noticeevent_info_notice_load_failed
import concafe.composeapp.generated.resources.noticeevent_info_notice_update_failed
import concafe.composeapp.generated.resources.noticeevent_info_notice_updated
import concafe.composeapp.generated.resources.noticeevent_info_reserve_schedule_next_step
import concafe.composeapp.generated.resources.noticeevent_pinned_desc
import concafe.composeapp.generated.resources.noticeevent_pinned_title
import concafe.composeapp.generated.resources.noticeevent_register_cta
import concafe.composeapp.generated.resources.noticeevent_remove
import concafe.composeapp.generated.resources.noticeevent_search_close_content_description
import concafe.composeapp.generated.resources.noticeevent_search_content_description
import concafe.composeapp.generated.resources.noticeevent_search_placeholder_event
import concafe.composeapp.generated.resources.noticeevent_search_placeholder_notice
import concafe.composeapp.generated.resources.noticeevent_tab_event
import concafe.composeapp.generated.resources.noticeevent_tab_notice
import concafe.composeapp.generated.resources.noticeevent_title
import concafe.composeapp.generated.resources.noticeevent_validation_cafe_required
import concafe.composeapp.generated.resources.noticeevent_validation_content_required
import concafe.composeapp.generated.resources.noticeevent_validation_event_image_required
import concafe.composeapp.generated.resources.noticeevent_validation_event_required
import concafe.composeapp.generated.resources.noticeevent_validation_notice_required
import concafe.composeapp.generated.resources.noticeevent_validation_title_required
import concafe.composeapp.generated.resources.signin_back_content_description
import concafe.composeapp.generated.resources.schedule_label_end_time
import concafe.composeapp.generated.resources.schedule_label_start_time
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeEventScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: NoticeEventViewModel = viewModel(
        key = "notice-event-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<NoticeEventViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                NoticeEventEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    NoticeEventContent(
        uiState = uiState,
        onAction = viewModel::onAction
    )
    if (uiState.isFormSheetVisible) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(NoticeEventAction.DismissFormSheet) },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = sheetState,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NoticeEventFormSheetContent(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeEventContent(
    uiState: NoticeEventUiState,
    onAction: (NoticeEventAction) -> Unit
) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    val isDarkMode = isSystemInDarkTheme()

    Scaffold(
        containerColor = if (isDarkMode) colorFromHex("FFFBFD") else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchMode) {
                        ConCafeFormField(
                            label = "",
                            value = uiState.query,
                            onValueChange = { onAction(NoticeEventAction.ChangeQuery(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            singleLine = true,
                            placeholder = stringResource(if (uiState.selectedTab == NoticeEventTab.NOTICE) {
                                Res.string.noticeevent_search_placeholder_notice
                            } else {
                                Res.string.noticeevent_search_placeholder_event
                            }),
                            leadingContent = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.noticeevent_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(NoticeEventAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.signin_back_content_description))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isSearchMode) {
                                isSearchMode = false
                                if (uiState.query.isNotEmpty()) {
                                    onAction(NoticeEventAction.ChangeQuery(""))
                                }
                            } else {
                                isSearchMode = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(if (isSearchMode) {
                                Res.string.noticeevent_search_close_content_description
                            } else {
                                Res.string.noticeevent_search_content_description
                            })
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Button(
                onClick = { onAction(NoticeEventAction.ClickRegister) },
                modifier = Modifier.navigationBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorFromHex("FFD1DC"),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(Res.string.noticeevent_register_cta), fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isDarkMode) {
                        Modifier.background(colorFromHex("FFFBFD"))
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                            )
                        )
                    }
                )
                .padding(innerPadding)
        ) {
            ConCafeTabBar(
                labels = NoticeEventTab.entries.map {
                    when (it) {
                        NoticeEventTab.NOTICE -> stringResource(Res.string.noticeevent_tab_notice)
                        NoticeEventTab.EVENT -> stringResource(Res.string.noticeevent_tab_event)
                    }
                },
                selectedIndex = NoticeEventTab.entries.indexOf(uiState.selectedTab),
                modifier = Modifier.fillMaxWidth(),
                onTabSelected = { index ->
                    onAction(NoticeEventAction.SelectTab(NoticeEventTab.entries[index]))
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                uiState.infoMessage?.let { message ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            InfoBanner(
                                message = when (message) {
                                    "noticeevent_validation_cafe_required" -> stringResource(Res.string.noticeevent_validation_cafe_required)
                                    "noticeevent_validation_notice_required" -> stringResource(Res.string.noticeevent_validation_notice_required)
                                    "noticeevent_validation_event_required" -> stringResource(Res.string.noticeevent_validation_event_required)
                                    "noticeevent_validation_title_required" -> stringResource(Res.string.noticeevent_validation_title_required)
                                    "noticeevent_validation_content_required" -> stringResource(Res.string.noticeevent_validation_content_required)
                                    "noticeevent_validation_event_image_required" -> stringResource(Res.string.noticeevent_validation_event_image_required)
                                    "noticeevent_info_notice_edit_target_not_found" -> stringResource(Res.string.noticeevent_info_notice_edit_target_not_found)
                                    "noticeevent_info_event_edit_target_not_found" -> stringResource(Res.string.noticeevent_info_event_edit_target_not_found)
                                    "noticeevent_info_notice_load_failed" -> stringResource(Res.string.noticeevent_info_notice_load_failed)
                                    "noticeevent_info_event_load_failed" -> stringResource(Res.string.noticeevent_info_event_load_failed)
                                    "noticeevent_info_notice_created" -> stringResource(Res.string.noticeevent_info_notice_created)
                                    "noticeevent_info_notice_updated" -> stringResource(Res.string.noticeevent_info_notice_updated)
                                    "noticeevent_info_event_created" -> stringResource(Res.string.noticeevent_info_event_created)
                                    "noticeevent_info_event_updated" -> stringResource(Res.string.noticeevent_info_event_updated)
                                    "noticeevent_info_notice_create_failed" -> stringResource(Res.string.noticeevent_info_notice_create_failed)
                                    "noticeevent_info_notice_update_failed" -> stringResource(Res.string.noticeevent_info_notice_update_failed)
                                    "noticeevent_info_event_create_failed" -> stringResource(Res.string.noticeevent_info_event_create_failed)
                                    "noticeevent_info_event_update_failed" -> stringResource(Res.string.noticeevent_info_event_update_failed)
                                    "noticeevent_info_notice_delete_success" -> stringResource(Res.string.noticeevent_info_notice_delete_success)
                                    "noticeevent_info_notice_delete_failed" -> stringResource(Res.string.noticeevent_info_notice_delete_failed)
                                    "noticeevent_info_event_delete_success" -> stringResource(Res.string.noticeevent_info_event_delete_success)
                                    "noticeevent_info_event_delete_failed" -> stringResource(Res.string.noticeevent_info_event_delete_failed)
                                    "noticeevent_info_image_upload_failed" -> stringResource(Res.string.noticeevent_info_image_upload_failed)
                                    "noticeevent_info_more_events_next_step" -> stringResource(Res.string.noticeevent_info_more_events_next_step)
                                    "noticeevent_info_image_one_only" -> stringResource(Res.string.noticeevent_info_image_one_only)
                                    "noticeevent_info_image_pick_required" -> stringResource(Res.string.noticeevent_info_image_pick_required)
                                    "noticeevent_info_reserve_schedule_next_step" -> stringResource(Res.string.noticeevent_info_reserve_schedule_next_step)
                                    else -> message
                                },
                                onDismiss = { onAction(NoticeEventAction.DismissInfoMessage) }
                            )
                        }
                    }
                }

                if (uiState.isCurrentTabLoading && uiState.isCurrentTabEmpty) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            LoadingStateCard()
                        }
                    }
                } else if (uiState.selectedTab == NoticeEventTab.NOTICE && uiState.notices.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyStateCard(message = stringResource(Res.string.noticeevent_empty_notice))
                        }
                    }
                } else if (uiState.selectedTab == NoticeEventTab.EVENT && uiState.events.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyStateCard(message = stringResource(Res.string.noticeevent_empty_event))
                        }
                    }
                } else if (uiState.selectedTab == NoticeEventTab.NOTICE) {
                    itemsIndexed(uiState.notices, key = { _, item -> item.id }) { index, notice ->
                        if (index == uiState.notices.lastIndex) {
                            LaunchedEffect(notice.id) {
                                onAction(NoticeEventAction.LoadMoreNotices)
                            }
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            NoticeCard(
                                item = notice,
                                onEdit = { onAction(NoticeEventAction.ClickEditNotice(notice.id)) },
                                onDelete = { onAction(NoticeEventAction.ClickDeleteNotice(notice.id)) }
                            )
                        }
                    }
                } else {
                    itemsIndexed(uiState.events, key = { _, item -> item.id }) { index, event ->
                        if (index == uiState.events.lastIndex) {
                            LaunchedEffect(event.id) {
                                onAction(NoticeEventAction.LoadMoreEvents)
                            }
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventCard(
                                item = event,
                                onEdit = { onAction(NoticeEventAction.ClickEditEvent(event.id)) },
                                onDelete = { onAction(NoticeEventAction.ClickDeleteEvent(event.id)) }
                            )
                        }
                    }
                }
                if (uiState.isCurrentTabLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorFromHex("EF6797"))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeEventFormSheetContent(
    uiState: NoticeEventUiState,
    onAction: (NoticeEventAction) -> Unit
) {
    var isEventPeriodEditorVisible by rememberSaveable { mutableStateOf(false) }
    var isStartDatePickerVisible by rememberSaveable { mutableStateOf(false) }
    var isEndDatePickerVisible by rememberSaveable { mutableStateOf(false) }
    var selectedStartDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedEndDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.selectedTab, uiState.formReservedAt, uiState.formEditingId) {
        if (uiState.selectedTab == NoticeEventTab.EVENT) {
            val parsed = parseEventPeriodText(uiState.formReservedAt)
            selectedStartDateMillis = parsed?.first
            selectedEndDateMillis = parsed?.second
        } else {
            isEventPeriodEditorVisible = false
            isStartDatePickerVisible = false
            isEndDatePickerVisible = false
            selectedStartDateMillis = null
            selectedEndDateMillis = null
        }
    }

    if (isStartDatePickerVisible) {
        val startState = rememberDatePickerState(
            initialSelectedDateMillis = selectedStartDateMillis ?: Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { isStartDatePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = startState.selectedDateMillis ?: return@TextButton
                        selectedStartDateMillis = picked
                        if (selectedEndDateMillis != null && selectedEndDateMillis!! < picked) {
                            selectedEndDateMillis = picked
                        }
                        isStartDatePickerVisible = false
                    }
                ) {
                    Text(stringResource(Res.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isStartDatePickerVisible = false }) {
                    Text(stringResource(Res.string.common_close))
                }
            }
        ) {
            DatePicker(state = startState)
        }
    }

    if (isEndDatePickerVisible) {
        val endState = rememberDatePickerState(
            initialSelectedDateMillis = selectedEndDateMillis ?: selectedStartDateMillis ?: Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { isEndDatePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = endState.selectedDateMillis ?: return@TextButton
                        val start = selectedStartDateMillis
                        selectedEndDateMillis = if (start != null && picked < start) start else picked
                        isEndDatePickerVisible = false
                    }
                ) {
                    Text(stringResource(Res.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isEndDatePickerVisible = false }) {
                    Text(stringResource(Res.string.common_close))
                }
            }
        ) {
            DatePicker(state = endState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(
                    when {
                        uiState.selectedTab == NoticeEventTab.NOTICE && uiState.formEditingId != null -> Res.string.noticeevent_form_sheet_title_notice_edit
                        uiState.selectedTab == NoticeEventTab.NOTICE -> Res.string.noticeevent_form_sheet_title_notice_create
                        uiState.formEditingId != null -> Res.string.noticeevent_form_sheet_title_event_edit
                        else -> Res.string.noticeevent_form_sheet_title_event_create
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onAction(NoticeEventAction.DismissFormSheet) }) {
                Icon(Filled.Close, contentDescription = stringResource(Res.string.common_close), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                ConCafeFormField(
                    label = stringResource(Res.string.noticeevent_form_label_title),
                    value = uiState.formTitle,
                    onValueChange = { onAction(NoticeEventAction.ChangeFormTitle(it)) },
                    placeholder = stringResource(
                        if (uiState.selectedTab == NoticeEventTab.NOTICE) {
                            Res.string.noticeevent_form_title_placeholder_notice
                        } else {
                            Res.string.noticeevent_form_title_placeholder_event
                        }
                    )
                )
            }
            item {
                ConCafeFormField(
                    label = stringResource(Res.string.noticeevent_form_label_content),
                    value = uiState.formContent,
                    onValueChange = { onAction(NoticeEventAction.ChangeFormContent(it)) },
                    placeholder = stringResource(
                        if (uiState.selectedTab == NoticeEventTab.NOTICE) {
                            Res.string.noticeevent_form_content_placeholder_notice
                        } else {
                            Res.string.noticeevent_form_content_placeholder_event
                        }
                    ),
                    minLines = 8,
                    singleLine = false
                )
            }
            if (uiState.showsImageSection) {
                item {
                    NoticeEventImageSection(
                        uiState = uiState,
                        onImageSelected = { imageUrl ->
                            onAction(NoticeEventAction.ChangeFormImageUrl(imageUrl))
                        },
                        onRemoveImage = {
                            onAction(NoticeEventAction.ClickRemoveFormImage)
                        }
                    )
                }
            }
            if (uiState.showsPinnedSection) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(Res.string.noticeevent_pinned_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(Res.string.noticeevent_pinned_desc), style = MaterialTheme.typography.labelMedium, color = colorFromHex("8F848F"))
                            }
                            Switch(
                                checked = uiState.formPinned,
                                onCheckedChange = { onAction(NoticeEventAction.ChangeFormPinned(it)) }
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(
                        if (uiState.selectedTab == NoticeEventTab.NOTICE) {
                            Res.string.noticeevent_form_schedule_label_notice
                        } else {
                            Res.string.noticeevent_form_schedule_label_event
                        }
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Button(
                    onClick = {
                        if (uiState.selectedTab == NoticeEventTab.EVENT) {
                            isEventPeriodEditorVisible = !isEventPeriodEditorVisible
                        } else {
                            onAction(NoticeEventAction.ClickReserveSchedule)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            uiState.formReservedAt.ifBlank {
                                stringResource(
                                    if (uiState.selectedTab == NoticeEventTab.NOTICE) {
                                        Res.string.noticeevent_form_schedule_placeholder_notice
                                    } else {
                                        Res.string.noticeevent_form_schedule_placeholder_event
                                    }
                                )
                            }
                        )
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                }
                if (uiState.selectedTab == NoticeEventTab.EVENT && isEventPeriodEditorVisible) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isStartDatePickerVisible = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(Res.string.schedule_label_start_time))
                                    Text(
                                        text = selectedStartDateMillis?.let(::formatDateMillis) ?: "-",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { isEndDatePickerVisible = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(Res.string.schedule_label_end_time))
                                    Text(
                                        text = selectedEndDateMillis?.let(::formatDateMillis) ?: "-",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedStartDateMillis = null
                                        selectedEndDateMillis = null
                                        onAction(NoticeEventAction.ChangeFormReservedAt(""))
                                        isEventPeriodEditorVisible = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(Res.string.noticeevent_remove))
                                }
                                Button(
                                    onClick = {
                                        val start = selectedStartDateMillis ?: return@Button
                                        val end = selectedEndDateMillis ?: return@Button
                                        val normalized = if (start <= end) start to end else end to start
                                        onAction(
                                            NoticeEventAction.ChangeFormReservedAt(
                                                formatEventPeriodText(normalized.first, normalized.second)
                                            )
                                        )
                                        isEventPeriodEditorVisible = false
                                    },
                                    enabled = selectedStartDateMillis != null && selectedEndDateMillis != null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorFromHex("FFD1DC"),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(stringResource(Res.string.common_confirm))
                                }
                            }
                        }
                    }
                }
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                        )
                    )
            ) {
                Button(
                    onClick = { onAction(NoticeEventAction.ClickSubmitForm) },
                    enabled = uiState.isFormSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .navigationBarsPadding()
                        .height(60.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorFromHex("FFD1DC"),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = colorFromHex("F0D9E0"),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        stringResource(
                            when {
                                uiState.selectedTab == NoticeEventTab.NOTICE && uiState.formEditingId != null -> Res.string.noticeevent_form_submit_notice_edit
                                uiState.selectedTab == NoticeEventTab.NOTICE -> Res.string.noticeevent_form_submit_notice_create
                                uiState.formEditingId != null -> Res.string.noticeevent_form_submit_event_edit
                                else -> Res.string.noticeevent_form_submit_event_create
                            }
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeEventImageSection(
    uiState: NoticeEventUiState,
    onImageSelected: (String) -> Unit,
    onRemoveImage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(Res.string.noticeevent_form_image_label),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        CompatImagePicker(
            onImageSelected = onImageSelected
        ) { launchImagePicker ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colorFromHex("FFD8E6"), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                        )
                    )
                    .clickable { launchImagePicker() }
            ) {
                if (uiState.hasAttachedImage) {
                    CompatImageDisplay(
                        imageUrl = uiState.formImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = colorFromHex("8B5164"),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(
                                if (uiState.hasAttachedImage) {
                                    Res.string.noticeevent_form_image_title_attached
                                } else {
                                    Res.string.noticeevent_form_image_title_empty
                                }
                            ),
                            color = colorFromHex("5A4954"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (uiState.hasAttachedImage) {
                    Button(
                        onClick = {
                            onRemoveImage()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = colorFromHex("8B5164")
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(Res.string.noticeevent_remove), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(
            text = stringResource(Res.string.noticeevent_form_image_description),
            style = MaterialTheme.typography.bodySmall,
            color = colorFromHex("8A8088"),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoticeCard(
    item: CafeNoticeManagementItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isPinned) {
                        StatusChip(
                            text = "PINNED",
                            container = colorFromHex("FFD1DC"),
                            content = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val (container, content) = when (item.statusAccent) {
                        NoticeStatusAccent.PUBLISHED -> colorFromHex("E8F8EC") to colorFromHex("2E9E5B")
                        NoticeStatusAccent.DRAFT -> colorFromHex("F2F0F3") to MaterialTheme.colorScheme.onSurfaceVariant
                        NoticeStatusAccent.ENDED -> colorFromHex("F3E8E8") to colorFromHex("8C5A5A")
                    }
                    StatusChip(text = item.statusLabel, container = container, content = content)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.menugoods_edit_content_description), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(Res.string.menugoods_delete_content_description), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.displayDate,
                style = MaterialTheme.typography.labelMedium,
                color = colorFromHex("8F848F")
            )
        }
    }
}

@Composable
private fun LoadingStateCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colorFromHex("EF6797"))
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorFromHex("8F848F")
            )
        }
    }
}

@Composable
private fun EventCard(
    item: CafeEventManagementItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.alpha(if (item.isDimmed) 0.72f else 1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val eventImageUrl = item.imageUrl.trim().takeIf { it.isNotEmpty() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colorFromHex("FFE7EF"), colorFromHex("F6D3E0"))
                            )
                        )
                )
                if (eventImageUrl != null) {
                    CompatImageDisplay(
                        imageUrl = eventImageUrl,
                        modifier = Modifier.fillMaxSize(),
                        applyRoundedClip = false
                    )
                    if (item.isDimmed) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!item.isDimmed) colorFromHex("FFD1DC") else colorFromHex("6E6570"))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.statusLabel,
                        color = if (!item.isDimmed) MaterialTheme.colorScheme.onSurface else Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.menugoods_edit_content_description), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(Res.string.menugoods_delete_content_description), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colorFromHex("8F848F"), modifier = Modifier.size(14.dp))
                    Text(text = item.periodText, style = MaterialTheme.typography.labelMedium, color = colorFromHex("8F848F"))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = actionLabel,
            color = colorFromHex("EF6797"),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@Composable
private fun StatusChip(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = content, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorFromHex("FFF2D8"))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message, color = colorFromHex("6B5320"), modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.common_close),
            color = colorFromHex("6B5320"),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onDismiss).padding(start = 12.dp)
        )
    }
}

private fun parseEventPeriodText(periodText: String): Pair<Long, Long>? {
    val regex = Regex("""(\d{4})\.(\d{2})\.(\d{2})\s*(?:~|-)\s*(\d{4})\.(\d{2})\.(\d{2})""")
    val match = regex.find(periodText.trim()) ?: return null
    val values = match.groupValues
    val start = LocalDate(values[1].toInt(), values[2].toInt(), values[3].toInt())
    val end = LocalDate(values[4].toInt(), values[5].toInt(), values[6].toInt())
    return localDateToEpochMillis(start) to localDateToEpochMillis(end)
}

private fun formatDateMillis(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "%04d.%02d.%02d".format(date.year, date.monthNumber, date.dayOfMonth)
}

private fun formatEventPeriodText(startMillis: Long, endMillis: Long): String {
    return "${formatDateMillis(startMillis)} - ${formatDateMillis(endMillis)}"
}

private fun localDateToEpochMillis(date: LocalDate): Long {
    val instant = date.atStartOfDayIn(TimeZone.currentSystemDefault())
    return instant.toEpochMilliseconds()
}

