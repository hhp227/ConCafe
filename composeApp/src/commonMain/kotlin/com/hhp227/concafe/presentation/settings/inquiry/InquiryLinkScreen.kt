package com.hhp227.concafe.presentation.settings.inquiry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryLinkScreen(
    viewModel: InquiryLinkViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<InquiryLinkViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                InquiryLinkEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is InquiryLinkEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    InquiryLinkContentScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InquiryLinkContentScreen(
    uiState: InquiryLinkUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (InquiryLinkAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("문의하기") },
                navigationIcon = {
                    IconButton(onClick = { onAction(InquiryLinkAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0x33FFD1DC)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onAction(InquiryLinkAction.ClickSubmit) },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Text(
                            text = if (uiState.isSubmitting) "접수 중..." else "문의 접수",
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFFBFD))
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    InquiryTypeCard(
                        selectedType = uiState.inquiryType,
                        onSelect = { onAction(InquiryLinkAction.ChangeInquiryType(it)) }
                    )
                }
                item {
                    InquirySectionCard(title = "문의 입력") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "입력한 문의는 서버에 저장되어 운영팀이 확인합니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6F6673)
                            )
                            ConCafeFormField(
                                label = "제목",
                                value = uiState.title,
                                onValueChange = { onAction(InquiryLinkAction.ChangeTitle(it)) },
                                placeholder = "문의 제목을 입력하세요"
                            )
                            ConCafeFormField(
                                label = "문의 내용",
                                value = uiState.message,
                                onValueChange = { onAction(InquiryLinkAction.ChangeMessage(it)) },
                                placeholder = "상세 내용을 입력해 주세요",
                                minLines = 7,
                                singleLine = false
                            )
                            if (uiState.errorMessage != null) {
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1436F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InquiryTypeCard(
    selectedType: InquiryType,
    onSelect: (InquiryType) -> Unit
) {
    InquirySectionCard(title = "문의 유형") {
        Text(
            text = "문의 성격에 맞는 항목을 선택해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6F6673)
        )
        InquiryTypeDropdown(
            selectedType = selectedType,
            onSelect = onSelect
        )
    }
}

@Composable
private fun InquiryTypeDropdown(
    selectedType: InquiryType,
    onSelect: (InquiryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8F5F6),
            border = BorderStroke(1.dp, Color(0x4DFFD1DC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = selectedType.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF2B2330)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF7C7480),
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            InquiryType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.title) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InquirySectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
