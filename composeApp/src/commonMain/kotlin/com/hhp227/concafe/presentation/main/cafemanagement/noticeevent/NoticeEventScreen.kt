package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.hhp227.concafe.di.resolveCreateCafeEventUseCase
import com.hhp227.concafe.di.resolveCreateCafeNoticeUseCase
import com.hhp227.concafe.di.resolveDeleteCafeEventUseCase
import com.hhp227.concafe.di.resolveDeleteCafeNoticeUseCase
import com.hhp227.concafe.di.resolveGetCafeEventPageUseCase
import com.hhp227.concafe.di.resolveGetCafeNoticePageUseCase
import com.hhp227.concafe.di.resolveObserveNoticeManagementEventUseCase
import com.hhp227.concafe.di.resolveUpdateCafeEventUseCase
import com.hhp227.concafe.di.resolveUpdateCafeNoticeUseCase
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.ConCafeTabBar
import com.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeEventScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: NoticeEventViewModel = viewModel(
        key = "notice-event-$cafeId",
        factory = viewModelFactory {
            initializer {
                NoticeEventViewModel(
                    cafeId = cafeId,
                    getCafeNoticePageUseCase = resolveGetCafeNoticePageUseCase(),
                    getCafeEventPageUseCase = resolveGetCafeEventPageUseCase(),
                    createCafeNoticeUseCase = resolveCreateCafeNoticeUseCase(),
                    createCafeEventUseCase = resolveCreateCafeEventUseCase(),
                    updateCafeNoticeUseCase = resolveUpdateCafeNoticeUseCase(),
                    updateCafeEventUseCase = resolveUpdateCafeEventUseCase(),
                    deleteCafeNoticeUseCase = resolveDeleteCafeNoticeUseCase(),
                    deleteCafeEventUseCase = resolveDeleteCafeEventUseCase(),
                    observeNoticeManagementEventUseCase = resolveObserveNoticeManagementEventUseCase()
                )
            }
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
            containerColor = Color(0xFFF8F5F6),
            sheetState = sheetState
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

    Scaffold(
        containerColor = Color(0xFFF8F5F6),
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
                            placeholder = if (uiState.selectedTab == NoticeEventTab.NOTICE) "공지사항 검색" else "이벤트 검색",
                            leadingContent = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        )
                    } else {
                        Text(
                            text = "공지 및 이벤트 관리",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(NoticeEventAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
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
                            contentDescription = if (isSearchMode) "검색 닫기" else "검색"
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
                    containerColor = Color(0xFFFFD1DC),
                    contentColor = Color(0xFF2B2330)
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("공지/이벤트 등록", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFFBFD))
                    )
                )
                .padding(innerPadding)
        ) {
            ConCafeTabBar(
                labels = NoticeEventTab.entries.map { it.title },
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
                            InfoBanner(message = message, onDismiss = { onAction(NoticeEventAction.DismissInfoMessage) })
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
                            EmptyStateCard(message = "등록된 공지사항이 없습니다.")
                        }
                    }
                } else if (uiState.selectedTab == NoticeEventTab.EVENT && uiState.events.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyStateCard(message = "등록된 이벤트가 없습니다.")
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
                            CircularProgressIndicator(color = Color(0xFFEF6797))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeEventFormSheetContent(
    uiState: NoticeEventUiState,
    onAction: (NoticeEventAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(uiState.formSheetTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onAction(NoticeEventAction.DismissFormSheet) }) {
                Icon(Filled.Close, contentDescription = "닫기", tint = Color(0xFF7A707A))
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                ConCafeFormField(
                    label = "제목",
                    value = uiState.formTitle,
                    onValueChange = { onAction(NoticeEventAction.ChangeFormTitle(it)) },
                    placeholder = uiState.formTitlePlaceholder
                )
            }
            item {
                ConCafeFormField(
                    label = "내용",
                    value = uiState.formContent,
                    onValueChange = { onAction(NoticeEventAction.ChangeFormContent(it)) },
                    placeholder = uiState.formContentPlaceholder,
                    minLines = 8,
                    singleLine = false
                )
            }
            if (uiState.showsImageSection) {
                item {
                    NoticeEventImageSection(uiState = uiState, onAction = onAction)
                }
            }
            if (uiState.showsPinnedSection) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("중요 공지 (Pinned)", fontWeight = FontWeight.Bold, color = Color(0xFF23161C))
                                Text("목록 상단에 고정됩니다.", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8F848F))
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
                    uiState.formScheduleLabel,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF665A63),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Button(
                    onClick = { onAction(NoticeEventAction.ClickReserveSchedule) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF9A8D95)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (uiState.formReservedAt.isBlank()) uiState.formSchedulePlaceholder else uiState.formReservedAt)
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                }
            }
        }
        Surface(
            color = Color.Transparent,
            modifier = Modifier.navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF8F5F6), Color(0xFFF8F5F6))
                        )
                    )
            ) {
                Button(
                    onClick = { onAction(NoticeEventAction.ClickSubmitForm) },
                    enabled = uiState.isFormSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF2B2330),
                        disabledContainerColor = Color(0xFFF0D9E0),
                        disabledContentColor = Color(0xFF7F7078)
                    )
                ) {
                    Text(uiState.formSubmitLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NoticeEventImageSection(
    uiState: NoticeEventUiState,
    onAction: (NoticeEventAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "대표 이미지",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF665A63),
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD8E6), Color(0xFFFFEFF5))
                    )
                )
                .clickable { onAction(NoticeEventAction.ClickFormImage) }
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF8B5164),
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = uiState.formImageTitle,
                    color = Color(0xFF5A4954),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (uiState.hasAttachedImage) {
                Button(
                    onClick = { onAction(NoticeEventAction.ClickRemoveFormImage) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF8B5164)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("제거", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = uiState.formImageDescription,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8088),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoticeCard(
    item: NoticeItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            container = Color(0xFFFFD1DC),
                            content = Color(0xFF2B2330)
                        )
                    }
                    val (container, content) = when (item.statusAccent) {
                        NoticeStatusAccent.PUBLISHED -> Color(0xFFE8F8EC) to Color(0xFF2E9E5B)
                        NoticeStatusAccent.DRAFT -> Color(0xFFF2F0F3) to Color(0xFF7A707A)
                        NoticeStatusAccent.ENDED -> Color(0xFFF3E8E8) to Color(0xFF8C5A5A)
                    }
                    StatusChip(text = item.statusLabel, container = container, content = content)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "편집", tint = Color(0xFF9A8D95))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "삭제", tint = Color(0xFF9A8D95))
                    }
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF23161C)
            )
            Text(
                text = item.date,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8F848F)
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
        CircularProgressIndicator(color = Color(0xFFEF6797))
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                color = Color(0xFF8F848F)
            )
        }
    }
}

@Composable
private fun EventCard(
    item: EventItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.alpha(if (item.isDimmed) 0.72f else 1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFE7EF), Color(0xFFF6D3E0))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (item.statusLabel == "진행 중") Color(0xFFFFD1DC) else Color(0xFF6E6570))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.statusLabel,
                        color = if (item.statusLabel == "진행 중") Color(0xFF2B2330) else Color.White,
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
                        color = Color(0xFF23161C),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color(0xFF9A8D95))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "삭제", tint = Color(0xFF9A8D95))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF8F848F), modifier = Modifier.size(14.dp))
                    Text(text = item.period, style = MaterialTheme.typography.labelMedium, color = Color(0xFF8F848F))
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
            color = Color(0xFFEF6797),
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
            .background(Color(0xFFFFF2D8))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = message, color = Color(0xFF6B5320), modifier = Modifier.weight(1f))
        Text(
            text = "닫기",
            color = Color(0xFF6B5320),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onDismiss).padding(start = 12.dp)
        )
    }
}
