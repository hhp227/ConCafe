package com.hhp227.concafe.presentation.main.cafemanagement.castmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastManagementScreen(
    cafeId: String,
    cafeName: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CastManagementViewModel = viewModel(
        key = "cast-management-$cafeId",
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<CastManagementViewModel> { parametersOf(cafeId, cafeName) }
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                CastManagementEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    CastManagementContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastManagementContentScreen(
    uiState: CastManagementUiState,
    onAction: (CastManagementAction) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.cast_management_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CastManagementAction.ClickBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cast_management_back)
                        )
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
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorFromHex("EF6797")
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ViewModeSelector(
                        viewMode = uiState.viewMode,
                        onSelect = { onAction(CastManagementAction.ChangeViewMode(it)) }
                    )
                    if (uiState.periodStart.isNotEmpty()) {
                        val periodLabel = formatPeriodLabel(uiState.viewMode, uiState.periodStart, uiState.periodEnd)

                        Text(
                            text = stringResource(Res.string.cast_management_period_schedule, periodLabel, uiState.cafeName),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (uiState.errorMessage != null) {
                        Text(
                            text = stringResource(Res.string.cast_management_error_load_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        when (uiState.viewMode) {
                            CastScheduleViewMode.WEEK -> WeekScheduleView(uiState.weekColumns)
                            CastScheduleViewMode.MONTH -> MonthScheduleView(uiState.monthOffset, uiState.monthCells)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatPeriodLabel(mode: CastScheduleViewMode, periodStart: String, periodEnd: String): String {
    if (periodStart.isEmpty()) return ""
    return when (mode) {
        CastScheduleViewMode.WEEK -> {
            val from = LocalDate.parse(periodStart)
            val to = LocalDate.parse(periodEnd)
            stringResource(Res.string.cast_management_period_week, from.monthNumber, from.dayOfMonth, to.monthNumber, to.dayOfMonth)
        }
        CastScheduleViewMode.MONTH -> {
            val from = LocalDate.parse(periodStart)
            stringResource(Res.string.cast_management_period_month, from.year, from.monthNumber)
        }
    }
}

@Composable
private fun ViewModeSelector(
    viewMode: CastScheduleViewMode,
    onSelect: (CastScheduleViewMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CastScheduleViewMode.entries.forEach { mode ->
                val selected = viewMode == mode
                Surface(
                    modifier = Modifier,
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) colorFromHex("FFD1DC") else Color.Transparent,
                    onClick = { onSelect(mode) }
                ) {
                    Text(
                        text = when (mode) {
                            CastScheduleViewMode.WEEK -> stringResource(Res.string.cast_management_view_week)
                            CastScheduleViewMode.MONTH -> stringResource(Res.string.cast_management_view_month)
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekScheduleView(columns: List<CastManagementUiState.WeekColumn>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        columns.forEach { col ->
            WeekDayCard(column = col)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekDayCard(column: CastManagementUiState.WeekColumn) {
    val isWorking = column.castNames.isNotEmpty()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = if (isWorking) colorFromHex("FFF0F4") else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = resolveDayLabel(column.dayLabelKey),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isWorking) colorFromHex("EF6797") else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = column.dateLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isWorking) {
                Text(
                    text = "-",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    column.castNames.forEach { name ->
                        CastNameChip(name, isWorking = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveDayLabel(key: String): String {
    val res: StringResource = when (key) {
        "day_mon" -> Res.string.day_mon
        "day_tue" -> Res.string.day_tue
        "day_wed" -> Res.string.day_wed
        "day_thu" -> Res.string.day_thu
        "day_fri" -> Res.string.day_fri
        "day_sat" -> Res.string.day_sat
        "day_sun" -> Res.string.day_sun
        else -> return key
    }
    return stringResource(res)
}

@Composable
private fun CastNameChip(name: String, isWorking: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isWorking) colorFromHex("ECFFF5") else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isWorking) colorFromHex("C8EFD9") else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWorking) colorFromHex("1F8B5F") else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthScheduleView(
    monthOffset: Int,
    cells: List<CastManagementUiState.MonthCell>
) {
    val dayKeys = listOf("day_mon", "day_tue", "day_wed", "day_thu", "day_fri", "day_sat", "day_sun")
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val totalCols = 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayKeys.forEach { key ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resolveDayLabel(key),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val allItems: List<CastManagementUiState.MonthCell?> = buildList {
            repeat(monthOffset) { add(null) }
            addAll(cells)
        }
        val rowCount = (allItems.size + totalCols - 1) / totalCols

        repeat(rowCount) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalCols) { colIndex ->
                    val itemIndex = rowIndex * totalCols + colIndex
                    val cell = allItems.getOrNull(itemIndex)

                    Box(modifier = Modifier.weight(1f)) {
                        if (cell == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            )
                        } else {
                            MonthDayCell(cell = cell)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(cell: CastManagementUiState.MonthCell) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = cell.dayNumber.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorFromHex("4A3645")
            )
            cell.castNames.forEach { name ->
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("5D4B55"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (cell.castNames.isEmpty()) {
                Text(
                    text = "-",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
