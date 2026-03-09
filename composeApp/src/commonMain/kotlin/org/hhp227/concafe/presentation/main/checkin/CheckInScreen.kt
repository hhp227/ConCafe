package org.hhp227.concafe.presentation.main.checkin

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.hhp227.concafe.di.resolveGetCheckInGuestFeedUseCase
import org.hhp227.concafe.di.resolveGetCheckInUserFeedUseCase
import org.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import org.hhp227.concafe.domain.model.CheckInCafeSummary
import org.hhp227.concafe.domain.model.CheckInCastSummary
import org.hhp227.concafe.domain.model.CheckInVisitEntry
import org.hhp227.concafe.presentation.component.CafeSummaryCard
import org.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CheckInViewModel(
                    getCheckInGuestFeedUseCase = resolveGetCheckInGuestFeedUseCase(),
                    getCheckInUserFeedUseCase = resolveGetCheckInUserFeedUseCase(),
                    observeCurrentUserUseCase = resolveObserveCurrentUserUseCase()
                )
            }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CheckInEvent.NavigateToCafe -> onNavigate(NavigationAction.NavigateToCafe(event.id))
                is CheckInEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
                CheckInEvent.NavigateToSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD))
    ) {
        CheckInContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction
        )
        if (uiState.isLoginPromptVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(CheckInAction.DismissLoginPrompt) },
                containerColor = Color.White
            ) {
                LoginRequiredBottomSheet(
                    onSignIn = { viewModel.onAction(CheckInAction.ClickSignIn) },
                    onSignUp = { viewModel.onAction(CheckInAction.ClickSignUp) }
                )
            }
        }
    }
}

@Composable
private fun CheckInContentScreen(
    uiState: CheckInUiState,
    onAction: (CheckInAction) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
            .background(Color(0xFFFFFBFD))
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
                onCafeClick = { onAction(CheckInAction.ClickCafe(it)) },
                onCheckInClick = { onAction(CheckInAction.ClickCheckIn) }
            )
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LoginPromotionSection(
                    onSignIn = { onAction(CheckInAction.ClickSignIn) },
                    onSignUp = { onAction(CheckInAction.ClickSignUp) }
                )
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CheckInGuestSectionTitle("🔥 인기 메이드 카페")
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PopularCafeSection(
                    cafes = uiState.popularCafes,
                    onCafeClick = { onAction(CheckInAction.ClickCafe(it)) }
                )
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CheckInGuestSectionTitle("☕ 오늘 인기 캐스트")
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PopularCastSection(
                    casts = uiState.popularCasts,
                    onCastClick = { onAction(CheckInAction.ClickCast(it)) }
                )
            }
            if (uiState.errorMessage != null) {
                Text(
                    text = "체크인 탭 데이터를 일부 불러오지 못했습니다.",
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CafeMapSection(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            currentLocationLabel = uiState.currentLocationLabel,
            mapCafes = uiState.mapCafes,
            onCafeClick = { onAction(CheckInAction.ClickCafe(it)) },
            onCheckInClick = { onAction(CheckInAction.ClickCheckIn) }
        )
        CheckInButton(
            onClick = { onAction(CheckInAction.ClickCheckIn) }
        )
        CheckInSectionTitle("오늘의 방문", "3월 9일")
        TodayVisitsRow(
            visits = uiState.todayVisits
        )
        Spacer(modifier = Modifier.height(24.dp))
        CheckInSectionTitle("최근 타임라인", "🕘")
        TimelineList(
            visits = uiState.recentVisits
        )
    }
}

@Composable
private fun CafeMapSection(
    modifier: Modifier = Modifier,
    currentLocationLabel: String,
    mapCafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    onCheckInClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF0F6), Color(0xFFFFFAFC), Color(0xFFFFF3F8))
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "주변 메이드카페 지도",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFEF6797),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = currentLocationLabel,
                            color = Color(0xFF7B7480),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedButton(
                    onClick = onCheckInClick,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("체크인")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFDDEB), Color(0xFFFFF5F9), Color(0xFFFFE8F1))
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                mapCafes.take(6).forEachIndexed { index, cafe ->
                    val markerModifier = when (index) {
                        0 -> Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 36.dp)
                        1 -> Modifier.align(Alignment.TopEnd).padding(end = 34.dp, top = 58.dp)
                        2 -> Modifier.align(Alignment.CenterStart).padding(start = 54.dp)
                        3 -> Modifier.align(Alignment.Center).padding(bottom = 10.dp)
                        4 -> Modifier.align(Alignment.CenterEnd).padding(end = 40.dp, top = 24.dp)
                        else -> Modifier.align(Alignment.BottomStart).padding(start = 110.dp, bottom = 28.dp)
                    }

                    Surface(
                        modifier = markerModifier.clickable { onCafeClick(cafe.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFEF6797), CircleShape)
                            )
                            Text(
                                text = cafe.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
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
        rating = "${cafe.rating}",
        location = cafe.locationLabel,
        modifier = Modifier
            .width(220.dp),
        trailingLabel = "체크인 ${cafe.checkInCount}",
        onClick = onClick
    )
}

@Composable
private fun PopularCastCard(
    cast: CheckInCastSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                listOf(Color(0xFFFFD1E2), Color(0xFFFFEAF2))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cast.name.take(1),
                        color = Color(0xFFB74C72),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
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
                        color = Color(0xFF7A7380)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFFFEEF5)
            ) {
                Text(
                    text = "오늘 방문 ${cast.todayVisit}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color(0xFFEF6797),
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
                        listOf(Color(0xFFEF6797), Color(0xFFF7A1C3))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "로그인하고 메이드카페 방문을 기록해보세요!",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• 방문 기록 저장", color = Color.White)
                Text("• 카페 팬 레벨 상승", color = Color.White)
                Text("• 체크인 배지 획득", color = Color.White)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFEF6797)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("로그인", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSignUp,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("회원가입", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LoginRequiredBottomSheet(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "체크인하려면 로그인이 필요합니다.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "로그인 후 방문 기록 저장, 팬 레벨, 배지 획득 기능을 사용할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6E6872)
        )
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6797)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("로그인하고 체크인", color = Color.White, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("회원가입", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CheckInGuestSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2B2630)
    )
}

@Composable
private fun CheckInSectionTitle(
    title: String,
    trailing: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(trailing)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TodayVisitsRow(
    visits: List<CheckInVisitEntry>
) {
    val todayVisits = visits.map {
        CheckInVisitCardUi(
            id = it.id,
            name = it.cafeName,
            time = it.visitedLabel
        )
    }

    if (todayVisits.isEmpty()) {
        EmptyVisitState(
            title = "오늘 방문 기록이 아직 없어요",
            description = "지금 체크인하고 첫 방문 기록을 남겨보세요."
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (todayVisits.size) {
                1 -> {
                    VisitCard(
                        name = todayVisits[0].name,
                        time = todayVisits[0].time,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    VisitCard(
                        name = todayVisits[0].name,
                        time = todayVisits[0].time,
                        modifier = Modifier.weight(1f)
                    )
                    MoreVisitCard(
                        remainingCount = todayVisits.size - 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitCard(
    name: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(180.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(time, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF1F6), Color(0xFFFFE1EC))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$remainingCount",
                    color = Color(0xFFEF6797),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "더 방문했어요",
                    color = Color(0xFF7C7480),
                    fontSize = 13.sp
                )
            }
        }
    }
}

private data class CheckInVisitCardUi(
    val id: String,
    val name: String,
    val time: String
)

@Composable
private fun CheckInButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor = when {
        isPressed -> Color(0xFFE78CB3)
        isHovered -> Color(0xFFF2A8C6)
        else -> Color(0xFFF6BCD1)
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
            contentColor = Color(0xFF3A2E36)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("새 방문 체크인", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun TimelineList(
    visits: List<CheckInVisitEntry>
) {
    if (visits.isEmpty()) {
        EmptyVisitState(
            title = "최근 타임라인이 비어 있어요",
            description = "체크인한 방문 기록이 이 영역에 시간순으로 표시됩니다."
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            visits.forEach { visit ->
                TimelineItem(visit)
            }
        }
    }
}

@Composable
fun TimelineItem(visit: CheckInVisitEntry) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // 왼쪽 타임라인 선과 아이콘
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF6BCD1)),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = Color.DarkGray
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(100.dp)
                    .background(Color(0xFFF6BCD1).copy(alpha = 0.3f))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(visit.cafeName, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = visit.relativeVisitedLabel(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(visit.memo ?: "방문 메모 없음", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun CheckInVisitEntry.relativeVisitedLabel(): String {
    val visitedDate = visitedAt.take(10)
    val referenceEpochDay = REFERENCE_DATE.toEpochDayOrNull() ?: return visitedLabel
    val visitedEpochDay = visitedDate.toEpochDayOrNull() ?: return visitedLabel
    val daysAgo = referenceEpochDay - visitedEpochDay
    return when {
        daysAgo < 0 -> visitedLabel
        daysAgo.toInt() == 0 -> "오늘"
        daysAgo.toInt() == 1 -> "어제"
        else -> "${daysAgo}일 전"
    }
}

private fun String.toEpochDayOrNull(): Long? {
    if (length != 10 || this[4] != '-' || this[7] != '-') return null

    val year = substring(0, 4).toIntOrNull() ?: return null
    val month = substring(5, 7).toIntOrNull() ?: return null
    val day = substring(8, 10).toIntOrNull() ?: return null

    if (month !in 1..12 || day !in 1..31) return null

    val adjustedYear = year - if (month <= 2) 1 else 0
    val era = if (adjustedYear >= 0) adjustedYear / 400 else (adjustedYear - 399) / 400
    val yearOfEra = adjustedYear - era * 400
    val adjustedMonth = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146097L + dayOfEra - 719468L
}

private const val REFERENCE_DATE = "2026-03-09"

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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7C7480)
            )
        }
    }
}
