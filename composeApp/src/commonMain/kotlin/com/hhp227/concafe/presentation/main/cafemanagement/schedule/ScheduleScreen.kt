package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import concafe.composeapp.generated.resources.Res
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.ScheduleManagementDaySchedule
import com.hhp227.concafe.domain.model.ScheduleManagementWeekDay
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.schedule_action_close
import concafe.composeapp.generated.resources.schedule_apply_edit
import concafe.composeapp.generated.resources.schedule_badge_cast_member
import concafe.composeapp.generated.resources.schedule_break_notice
import concafe.composeapp.generated.resources.schedule_calendar
import concafe.composeapp.generated.resources.schedule_concept_butler
import concafe.composeapp.generated.resources.schedule_concept_idol
import concafe.composeapp.generated.resources.schedule_concept_maid
import concafe.composeapp.generated.resources.schedule_content_back
import concafe.composeapp.generated.resources.schedule_content_edit
import concafe.composeapp.generated.resources.schedule_content_more
import concafe.composeapp.generated.resources.schedule_duration_hours_minutes
import concafe.composeapp.generated.resources.schedule_duration_hours_only
import concafe.composeapp.generated.resources.schedule_edit_title
import concafe.composeapp.generated.resources.schedule_error_end_after_start
import concafe.composeapp.generated.resources.schedule_error_end_required
import concafe.composeapp.generated.resources.schedule_error_save_failed
import concafe.composeapp.generated.resources.schedule_error_start_required
import concafe.composeapp.generated.resources.schedule_error_week_save_failed
import concafe.composeapp.generated.resources.schedule_event_week_saved
import concafe.composeapp.generated.resources.schedule_info_calendar_next_step
import concafe.composeapp.generated.resources.schedule_info_edit_applied
import concafe.composeapp.generated.resources.schedule_info_load_failed
import concafe.composeapp.generated.resources.schedule_info_more_next_step
import concafe.composeapp.generated.resources.schedule_info_no_changes
import concafe.composeapp.generated.resources.schedule_info_saved_off
import concafe.composeapp.generated.resources.schedule_info_saved_vacation
import concafe.composeapp.generated.resources.schedule_info_saved_work
import concafe.composeapp.generated.resources.schedule_label_end_time
import concafe.composeapp.generated.resources.schedule_label_start_time
import concafe.composeapp.generated.resources.schedule_save
import concafe.composeapp.generated.resources.schedule_save_in_progress
import concafe.composeapp.generated.resources.schedule_status_off
import concafe.composeapp.generated.resources.schedule_status_vacation
import concafe.composeapp.generated.resources.schedule_status_work
import concafe.composeapp.generated.resources.schedule_title
import concafe.composeapp.generated.resources.schedule_total_prefix
import concafe.composeapp.generated.resources.schedule_total_work
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    castId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: ScheduleViewModel = viewModel(
        key = "schedule-${castId ?: "self"}",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<ScheduleViewModel> { parametersOf(castId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                ScheduleEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is ScheduleEvent.NavigateToCastManagement -> onNavigationAction(
                    NavigationAction.NavigateToCastManagement(event.cafeId, event.cafeName)
                )
                is ScheduleEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        when (event.message) {
                            "schedule_info_saved_work",
                            "schedule_info_saved_off",
                            "schedule_info_saved_vacation",
                            "schedule_info_load_failed",
                            "schedule_info_more_next_step",
                            "schedule_info_calendar_next_step",
                            "schedule_error_end_after_start",
                            "schedule_info_edit_applied",
                            "schedule_info_no_changes",
                            "schedule_error_start_required",
                            "schedule_error_end_required",
                            "schedule_error_save_failed",
                            "schedule_error_week_save_failed",
                            "schedule_event_week_saved" -> getString(
                                when (event.message) {
                                    "schedule_info_saved_work" -> Res.string.schedule_info_saved_work
                                    "schedule_info_saved_off" -> Res.string.schedule_info_saved_off
                                    "schedule_info_saved_vacation" -> Res.string.schedule_info_saved_vacation
                                    "schedule_info_load_failed" -> Res.string.schedule_info_load_failed
                                    "schedule_info_more_next_step" -> Res.string.schedule_info_more_next_step
                                    "schedule_info_calendar_next_step" -> Res.string.schedule_info_calendar_next_step
                                    "schedule_error_end_after_start" -> Res.string.schedule_error_end_after_start
                                    "schedule_info_edit_applied" -> Res.string.schedule_info_edit_applied
                                    "schedule_info_no_changes" -> Res.string.schedule_info_no_changes
                                    "schedule_error_start_required" -> Res.string.schedule_error_start_required
                                    "schedule_error_end_required" -> Res.string.schedule_error_end_required
                                    "schedule_error_save_failed" -> Res.string.schedule_error_save_failed
                                    "schedule_error_week_save_failed" -> Res.string.schedule_error_week_save_failed
                                    else -> Res.string.schedule_event_week_saved
                                }
                            )
                            else -> event.message
                        }
                    )
                }
            }
        }
    }
    if (uiState.isEditSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(ScheduleAction.DismissEditSheet) },
            sheetState = editSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ScheduleEditSheet(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
    ScheduleContentScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun statusLabel(status: CastScheduleStatus): String {
    return when (status) {
        CastScheduleStatus.WORK -> stringResource(Res.string.schedule_status_work)
        CastScheduleStatus.OFF -> stringResource(Res.string.schedule_status_off)
        CastScheduleStatus.VACATION -> stringResource(Res.string.schedule_status_vacation)
    }
}

@Composable
private fun ScheduleEditSheet(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .keyboardBottomInsets()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(stringResource(Res.string.schedule_edit_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(uiState.editingScheduleTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CastScheduleStatus.entries.forEach { status ->
                    val selected = uiState.editStatus == status
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAction(ScheduleAction.ChangeEditStatus(status)) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                statusLabel(status),
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeDropdownField(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.schedule_label_start_time),
                value = uiState.editStartTime,
                enabled = uiState.isEditingWorking,
                options = uiState.timeOptions,
                onSelect = { onAction(ScheduleAction.ChangeEditStartTime(it)) }
            )
            TimeDropdownField(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.schedule_label_end_time),
                value = uiState.editEndTime,
                enabled = uiState.isEditingWorking,
                options = uiState.timeOptions,
                onSelect = { onAction(ScheduleAction.ChangeEditEndTime(it)) }
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0x1AFFD1DC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFD1DC))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = colorFromHex("EF6797"), modifier = Modifier.size(16.dp))
                Text(
                    stringResource(Res.string.schedule_break_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.schedule_total_work), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                Text(stringResource(Res.string.schedule_total_prefix), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(resolveScheduleDurationLabel(uiState.totalWorkDurationLabel), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        Button(
            onClick = { onAction(ScheduleAction.SubmitEditDay) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colorFromHex("FFD1DC"), contentColor = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(stringResource(Res.string.schedule_apply_edit), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdownField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    enabled: Boolean,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = colorFromHex("EF6797"))
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    disabledContainerColor = colorFromHex("F2EDF0"),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleContentScreen(
    uiState: ScheduleUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ScheduleAction) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.schedule_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(ScheduleAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.schedule_content_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(ScheduleAction.ClickMore) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.schedule_content_more))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                Button(
                    onClick = { onAction(ScheduleAction.ClickSave) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .navigationBarsPadding(),
                    enabled = uiState.hasPendingChanges && !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = colorFromHex("FFD1DC")),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (uiState.isSaving) Res.string.schedule_save_in_progress else Res.string.schedule_save),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSystemInDarkTheme()) {
                        Modifier.background(colorFromHex("FFFBFD"))
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                    }
                )
                .padding(innerPadding)
        ) {
            if (!uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScheduleCastSummaryCard(uiState.castSummary)
                    WeekSelectorSection(uiState = uiState, onAction = onAction)
                    if (uiState.errorMessage != null) {
                        ScheduleInfoBanner(
                            message = uiState.errorMessage,
                            onDismiss = { onAction(ScheduleAction.DismissInfoMessage) }
                        )
                    }
                    if (uiState.infoMessage != null) {
                        ScheduleInfoBanner(
                            message = uiState.infoMessage,
                            onDismiss = { onAction(ScheduleAction.DismissInfoMessage) }
                        )
                    }
                    ScheduleDayList(
                        schedules = uiState.schedules,
                        onAction = onAction
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorFromHex("EF6797")
                )
            }
        }
    }
}

@Composable
private fun ScheduleCastSummaryCard(
    castSummary: ScheduleUiState.CastSummary
) {
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (castSummary.badge) {
                        "schedule_badge_cast_member" -> stringResource(Res.string.schedule_badge_cast_member)
                        else -> castSummary.badge
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("EF6797")
                )
                Text(
                    text = castSummary.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = resolveScheduleCastSubtitle(castSummary.subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colorFromHex("FFD7E5"), colorFromHex("F2ADC2"))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = castSummary.initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("7C3F67")
                )
            }
        }
    }
}

@Composable
private fun WeekSelectorSection(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.weekRangeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.clickable { onAction(ScheduleAction.ClickCalendar) },
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
                    text = stringResource(Res.string.schedule_calendar),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorFromHex("EF6797"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.weekDays.forEach { day ->
                WeekDayChip(
                    day = day,
                    isSelected = day.id == uiState.selectedDayId,
                    onClick = { onAction(ScheduleAction.SelectDay(day.id)) }
                )
            }
        }
    }
}

@Composable
private fun WeekDayChip(
    day: ScheduleManagementWeekDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colorFromHex("FFD1DC") else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
    ) {
        Column(
            modifier = Modifier
                .width(56.dp)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = day.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = day.number,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ScheduleInfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorFromHex("FFF6D7"),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorFromHex("F1D88D"))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = resolveScheduleMessageLabel(message),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colorFromHex("6B5320")
            )
            Text(
                text = stringResource(Res.string.schedule_action_close),
                modifier = Modifier.clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colorFromHex("6B5320")
            )
        }
    }
}

@Composable
private fun ScheduleDayList(
    schedules: List<ScheduleManagementDaySchedule>,
    onAction: (ScheduleAction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        schedules.forEach { day ->
            DayScheduleCard(
                schedule = day,
                onEditClick = { onAction(ScheduleAction.ClickEditDay(day.id)) }
            )
        }
    }
}

@Composable
private fun DayScheduleCard(
    schedule: ScheduleManagementDaySchedule,
    onEditClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (schedule.isWorking) colorFromHex("FFD1DC").copy(alpha = 0.12f) else Color.Transparent
                )
                .padding(start = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .background(if (schedule.isWorking) colorFromHex("FFD1DC") else colorFromHex("E9E0E5"))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (schedule.isWorking) Color(0x14FFD1DC) else colorFromHex("F2EDF0")
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (schedule.isWorking) Icons.Default.Schedule else Icons.Default.Hotel,
                        contentDescription = null,
                        tint = if (schedule.isWorking) colorFromHex("EF6797") else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = schedule.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (schedule.isWorking) Color(0x4DFFD1DC) else colorFromHex("F2EDF0")
                        ) {
                            Text(
                                text = resolveScheduleStatusLabel(schedule.statusLabel, schedule.status),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (schedule.isWorking) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = resolveScheduleTimeLabel(schedule.timeLabel, schedule.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier.clickable(onClick = onEditClick),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.schedule_content_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveScheduleDurationLabel(value: String): String {
    return when {
        value.startsWith("schedule_duration_hours_only:") -> {
            val hours = value.substringAfter(':').toIntOrNull() ?: 0
            stringResource(Res.string.schedule_duration_hours_only, hours)
        }
        value.startsWith("schedule_duration_hours_minutes:") -> {
            val tokens = value.split(':')
            val hours = tokens.getOrNull(1)?.toIntOrNull() ?: 0
            val minutes = tokens.getOrNull(2)?.toIntOrNull() ?: 0
            stringResource(Res.string.schedule_duration_hours_minutes, hours, minutes)
        }
        else -> value
    }
}

@Composable
private fun resolveScheduleMessageLabel(message: String): String {
    return when (message) {
        "schedule_info_saved_work" -> stringResource(Res.string.schedule_info_saved_work)
        "schedule_info_saved_off" -> stringResource(Res.string.schedule_info_saved_off)
        "schedule_info_saved_vacation" -> stringResource(Res.string.schedule_info_saved_vacation)
        "schedule_info_load_failed" -> stringResource(Res.string.schedule_info_load_failed)
        "schedule_info_more_next_step" -> stringResource(Res.string.schedule_info_more_next_step)
        "schedule_info_calendar_next_step" -> stringResource(Res.string.schedule_info_calendar_next_step)
        "schedule_error_end_after_start" -> stringResource(Res.string.schedule_error_end_after_start)
        "schedule_info_edit_applied" -> stringResource(Res.string.schedule_info_edit_applied)
        "schedule_info_no_changes" -> stringResource(Res.string.schedule_info_no_changes)
        "schedule_error_start_required" -> stringResource(Res.string.schedule_error_start_required)
        "schedule_error_end_required" -> stringResource(Res.string.schedule_error_end_required)
        "schedule_error_save_failed" -> stringResource(Res.string.schedule_error_save_failed)
        "schedule_error_week_save_failed" -> stringResource(Res.string.schedule_error_week_save_failed)
        "schedule_event_week_saved" -> stringResource(Res.string.schedule_event_week_saved)
        else -> message
    }
}

@Composable
private fun resolveScheduleStatusLabel(statusLabel: String, status: CastScheduleStatus): String {
    return when (statusLabel.lowercase()) {
        "schedule_status_work", "근무", "work" -> stringResource(Res.string.schedule_status_work)
        "schedule_status_off", "휴무", "off" -> stringResource(Res.string.schedule_status_off)
        "schedule_status_vacation", "휴가", "vacation" -> stringResource(Res.string.schedule_status_vacation)
        else -> {
            when (status) {
                CastScheduleStatus.WORK -> stringResource(Res.string.schedule_status_work)
                CastScheduleStatus.OFF -> stringResource(Res.string.schedule_status_off)
                CastScheduleStatus.VACATION -> stringResource(Res.string.schedule_status_vacation)
            }
        }
    }
}

@Composable
private fun resolveScheduleTimeLabel(timeLabel: String, status: CastScheduleStatus): String {
    return when (timeLabel.lowercase()) {
        "schedule_status_off", "휴무", "off" -> stringResource(Res.string.schedule_status_off)
        "schedule_status_vacation", "휴가", "vacation" -> stringResource(Res.string.schedule_status_vacation)
        else -> {
            if (status == CastScheduleStatus.OFF) {
                stringResource(Res.string.schedule_status_off)
            } else if (status == CastScheduleStatus.VACATION) {
                stringResource(Res.string.schedule_status_vacation)
            } else {
                timeLabel
            }
        }
    }
}

@Composable
private fun resolveScheduleCastSubtitle(subtitle: String): String {
    val separator = " / "
    if (!subtitle.contains(separator)) return subtitle
    val concept = subtitle.substringBefore(separator)
    val cafeName = subtitle.substringAfter(separator)
    val resolvedConcept = when (concept) {
        "schedule_concept_maid" -> stringResource(Res.string.schedule_concept_maid)
        "schedule_concept_butler" -> stringResource(Res.string.schedule_concept_butler)
        "schedule_concept_idol" -> stringResource(Res.string.schedule_concept_idol)
        else -> concept
    }
    return "$resolvedConcept$separator$cafeName"
}
