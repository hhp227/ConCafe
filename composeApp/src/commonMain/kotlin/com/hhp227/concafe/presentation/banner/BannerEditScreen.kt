package com.hhp227.concafe.presentation.banner

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.di.resolveGetCafeEventPageUseCase
import com.hhp227.concafe.di.resolveGetCafeManagementUseCase
import com.hhp227.concafe.di.resolveGetCafeNoticePageUseCase
import com.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import com.hhp227.concafe.di.resolveCreateHomeBannerUseCase
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerEditScreen(
    initialCafeId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: BannerEditViewModel = viewModel(
        key = "banner-edit-${initialCafeId.orEmpty()}",
        factory = viewModelFactory {
            initializer {
                BannerEditViewModel(
                    initialCafeId = initialCafeId,
                    createHomeBannerUseCase = resolveCreateHomeBannerUseCase(),
                    getCafeManagementUseCase = resolveGetCafeManagementUseCase(),
                    getCafeNoticePageUseCase = resolveGetCafeNoticePageUseCase(),
                    getCafeEventPageUseCase = resolveGetCafeEventPageUseCase(),
                    observeCurrentUserUseCase = resolveObserveCurrentUserUseCase()
                )
            }
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
                    snackbarHostState.showSnackbar("배너가 등록되었습니다.")
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
            containerColor = Color(0xFFF8F5F6)
        ) {
            BannerSelectorSheet(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
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
                title = { Text(uiState.screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onAction(BannerEditAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(BannerEditAction.ClickSave) },
                        enabled = uiState.isSaveEnabled
                    ) {
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color(0x1AFFD1DC))
            ) {
                Button(
                    onClick = { onAction(BannerEditAction.ClickSave) },
                    enabled = uiState.isSaveEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF2B2330),
                        disabledContainerColor = Color(0xFFF4D7DF),
                        disabledContentColor = Color(0xFF8B7D83)
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF2B2330),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                    Text(
                        text = uiState.submitButtonText,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFFBFD))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BannerImageCard(
                        uiState = uiState,
                        onClick = { onAction(BannerEditAction.ClickImagePicker) }
                    )
                }
                item {
                    BannerSectionCard(title = "배너 기본 정보") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ConCafeFormField(
                                label = "배너 제목",
                                value = uiState.title,
                                onValueChange = { onAction(BannerEditAction.ChangeTitle(it)) },
                                placeholder = "배너 제목을 입력해주세요"
                            )
                            ConCafeFormField(
                                label = "서브 문구",
                                value = uiState.subtitle,
                                onValueChange = { onAction(BannerEditAction.ChangeSubtitle(it)) },
                                placeholder = "서브 문구를 입력해주세요"
                            )
                        }
                    }
                }
                item {
                    BannerSectionCard(title = "연결 대상 설정 (Target)") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TargetTypeGrid(
                                selectedTarget = uiState.selectedTarget,
                                onSelect = { onAction(BannerEditAction.SelectTarget(it)) }
                            )
                            when (uiState.selectedTarget) {
                                BannerTargetType.EXTERNAL_LINK -> {
                                    ConCafeFormField(
                                        label = "외부 링크",
                                        value = uiState.targetValue,
                                        onValueChange = { onAction(BannerEditAction.ChangeTargetValue(it)) },
                                        placeholder = uiState.targetFieldPlaceholder
                                    )
                                }
                                BannerTargetType.CAFE_DETAIL -> {
                                    if (uiState.isAdmin) {
                                        SelectionFieldCard(
                                            label = "운영 카페",
                                            selectedItem = uiState.selectedCafeOption,
                                            placeholder = "운영 카페를 선택해주세요",
                                            onClick = { onAction(BannerEditAction.ClickCafeSelector) }
                                        )
                                    } else {
                                        FixedSelectionCard(
                                            label = "적용 카페",
                                            selectedItem = uiState.selectedCafeOption,
                                            placeholder = "연결할 운영 카페가 없습니다."
                                        )
                                    }
                                }
                                BannerTargetType.NOTICE,
                                BannerTargetType.EVENT_DETAIL -> {
                                    if (uiState.isAdmin) {
                                        SelectionFieldCard(
                                            label = "운영 카페",
                                            selectedItem = uiState.selectedCafeOption,
                                            placeholder = "운영 카페를 선택해주세요",
                                            onClick = { onAction(BannerEditAction.ClickCafeSelector) }
                                        )
                                    } else {
                                        FixedSelectionCard(
                                            label = "적용 카페",
                                            selectedItem = uiState.selectedCafeOption,
                                            placeholder = "연결할 운영 카페가 없습니다."
                                        )
                                    }
                                    SelectionFieldCard(
                                        label = uiState.targetSelectionLabel,
                                        selectedItem = uiState.selectedContentOption,
                                        placeholder = uiState.targetSelectionPlaceholder,
                                        onClick = { onAction(BannerEditAction.ClickTargetSelector) }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    BannerSectionCard(title = "노출 기간 설정") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadgeText(label = uiState.displayDaysLabel)
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
                                Text("1일", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9A8D95))
                                Text("10일", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9A8D95))
                            }
                        }
                    }
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = message,
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
            text = uiState.selectorTitle,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        ConCafeFormField(
            label = "검색",
            value = uiState.selectorQuery,
            onValueChange = { onAction(BannerEditAction.ChangeSelectorQuery(it)) },
            modifier = Modifier.padding(horizontal = 24.dp),
            placeholder = uiState.selectorSearchPlaceholder
        )
        if (uiState.isSelectorLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFEF6797))
            }
        } else if (uiState.selectorOptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "선택 가능한 항목이 없습니다.",
                    color = Color(0xFF8F848F)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.selectorOptions, key = { it.id }) { item ->
                    SelectorOptionCard(
                        item = item,
                        onClick = { onAction(BannerEditAction.SelectSelectorItem(item.id)) }
                    )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFD))
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
                    .size(72.dp)
                    .background(Color(0x14EF6797), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color(0xFFEF6797),
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.imageSectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.imageGuideText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8F848F),
                    textAlign = TextAlign.Center
                )
                uiState.selectedImageLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFEF6797),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD1DC),
                    contentColor = Color(0xFF2B2330)
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(uiState.imageButtonText, fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        .background(Color(0xFFFFD1DC), RoundedCornerShape(999.dp))
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
                        color = if (isSelected) Color(0x14FFD1DC) else Color(0xFFF8F5F6),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFFD1DC) else Color(0x33FFD1DC)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF23161C) else Color(0xFF7A707A)
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
    selectedItem: BannerSelectableItem?,
    placeholder: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF665A63), fontWeight = FontWeight.Medium)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5F6)),
            border = BorderStroke(1.dp, Color(0x33FFD1DC))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedItem?.title ?: placeholder,
                        color = if (selectedItem == null) Color(0xFFAA98A4) else Color(0xFF23161C),
                        fontWeight = if (selectedItem == null) FontWeight.Normal else FontWeight.SemiBold
                    )
                    selectedItem?.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8F848F)
                        )
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF8F848F))
            }
        }
    }
}

@Composable
private fun FixedSelectionCard(
    label: String,
    selectedItem: BannerSelectableItem?,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF665A63), fontWeight = FontWeight.Medium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5F6)),
            border = BorderStroke(1.dp, Color(0x33FFD1DC))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedItem?.title ?: placeholder,
                    color = if (selectedItem == null) Color(0xFFAA98A4) else Color(0xFF23161C),
                    fontWeight = if (selectedItem == null) FontWeight.Normal else FontWeight.SemiBold
                )
                selectedItem?.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8F848F)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorOptionCard(
    item: BannerSelectableItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(item.title, fontWeight = FontWeight.Bold, color = Color(0xFF23161C))
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8F848F))
        }
    }
}

@Composable
private fun BadgeText(label: String) {
    Surface(
        color = Color(0x14EF6797),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xFFEF6797),
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
            .background(Color(0x14FFD1DC), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF6797), modifier = Modifier.padding(top = 2.dp))
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7A707A)
        )
        TextButton(onClick = onDismiss) {
            Text("닫기", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
private fun BannerEditContentPreview() {
    BannerEditContentScreen(
        uiState = BannerEditUiState(
            ownedCafeOptions = listOf(BannerSelectableItem("cafe-1", "루나 메이드 카페", "서울 홍대")),
            selectedCafeOption = BannerSelectableItem("cafe-1", "루나 메이드 카페", "서울 홍대")
        ),
        onAction = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}
