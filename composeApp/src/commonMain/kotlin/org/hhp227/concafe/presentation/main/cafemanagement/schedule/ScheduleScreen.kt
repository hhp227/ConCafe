package org.hhp227.concafe.presentation.main.cafemanagement.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import org.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun ScheduleScreen(
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                ScheduleEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is ScheduleEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    ScheduleContentScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleContentScreen(
    uiState: ScheduleUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ScheduleAction) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8F5F6),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "주간 출근표 관리",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(ScheduleAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(ScheduleAction.ClickMore) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Button(
                    onClick = { onAction(ScheduleAction.ClickSave) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .navigationBarsPadding(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD1DC)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF24161E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "주간 시간표 저장하기",
                        color = Color(0xFF24161E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFF8FB), Color(0xFFFFEFF5))
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScheduleCastSummaryCard(uiState.castSummary)
            WeekSelectorSection(uiState = uiState, onAction = onAction)
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
    }
}

@Composable
private fun ScheduleCastSummaryCard(
    castSummary: ScheduleUiState.CastSummary
) {
    Card(
        modifier = Modifier.padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
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
                    text = castSummary.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF6797)
                )
                Text(
                    text = castSummary.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF24161E)
                )
                Text(
                    text = castSummary.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7A707A)
                )
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD7E5), Color(0xFFF2ADC2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3F67)
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
                color = Color(0xFF24161E)
            )
            Row(
                modifier = Modifier.clickable { onAction(ScheduleAction.ClickCalendar) },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFFEF6797),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "달력보기",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFEF6797),
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
                    onClick = { onAction(ScheduleAction.SelectDay(day.id)) }
                )
            }
        }
    }
}

@Composable
private fun WeekDayChip(
    day: ScheduleUiState.WeekDay,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (day.isSelected) Color(0xFFFFD1DC) else Color.White.copy(alpha = 0.92f),
        shadowElevation = if (day.isSelected) 4.dp else 0.dp,
        border = if (day.isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFD1DC))
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
                color = if (day.isSelected) Color(0x9924161E) else Color(0xFF9C8C98)
            )
            Text(
                text = day.number,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF24161E)
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
        color = Color(0xFFFFF6D7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1D88D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320)
            )
            Text(
                text = "닫기",
                modifier = Modifier.clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B5320)
            )
        }
    }
}

@Composable
private fun ScheduleDayList(
    schedules: List<ScheduleUiState.DaySchedule>,
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
    schedule: ScheduleUiState.DaySchedule,
    onEditClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = if (schedule.isWorking) 0.96f else 0.88f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (schedule.isWorking) Color(0xFFFFD1DC).copy(alpha = 0.12f) else Color.Transparent
                )
                .padding(start = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .background(if (schedule.isWorking) Color(0xFFFFD1DC) else Color(0xFFE9E0E5))
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
                            if (schedule.isWorking) Color(0x14FFD1DC) else Color(0xFFF2EDF0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (schedule.isWorking) Icons.Default.Schedule else Icons.Default.Hotel,
                        contentDescription = null,
                        tint = if (schedule.isWorking) Color(0xFFEF6797) else Color(0xFFB0A3AC)
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
                            color = Color(0xFF24161E)
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (schedule.isWorking) Color(0x4DFFD1DC) else Color(0xFFF2EDF0)
                        ) {
                            Text(
                                text = schedule.statusLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (schedule.isWorking) Color(0xFF5B4A57) else Color(0xFF9C8C98)
                            )
                        }
                    }
                    Text(
                        text = schedule.timeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (schedule.isWorking) Color(0xFF7A707A) else Color(0xFFB0A3AC)
                    )
                }
                Surface(
                    modifier = Modifier.clickable(onClick = onEditClick),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8F5F6)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = Color(0xFF7A707A)
                        )
                    }
                }
            }
        }
    }
}
