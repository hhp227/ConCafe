package com.hhp227.concafe.presentation.banner

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerEditScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: BannerEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                BannerEditEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                BannerEditEvent.ShowSaveSuccessMessage -> {
                    snackbarHostState.showSnackbar("배너 초안이 저장되었습니다.")
                }
            }
        }
    }
    BannerEditContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
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
                        text = uiState.screenTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                            ConCafeFormField(
                                label = "대상 값",
                                value = uiState.targetValue,
                                onValueChange = { onAction(BannerEditAction.ChangeTargetValue(it)) },
                                placeholder = uiState.targetFieldPlaceholder
                            )
                        }
                    }
                }
                item {
                    BannerSectionCard(title = "노출 기간 설정") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriorityBadge(label = uiState.displayDaysLabel)
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
    trailing: @Composable (() -> Unit)? = null,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                trailing?.invoke()
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
private fun PriorityBadge(label: String) {
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

@Preview()
@Composable
private fun BannerEditContentPreview() {
    BannerEditContentScreen(
        uiState = BannerEditUiState(),
        onAction = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}
