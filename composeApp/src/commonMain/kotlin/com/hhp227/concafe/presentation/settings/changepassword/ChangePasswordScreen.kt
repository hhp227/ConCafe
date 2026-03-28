package com.hhp227.concafe.presentation.settings.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<ChangePasswordViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                ChangePasswordEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is ChangePasswordEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("비밀번호 변경") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(ChangePasswordAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ChangePasswordContentScreen(
            uiState = uiState,
            innerPadding = innerPadding,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun ChangePasswordContentScreen(
    uiState: ChangePasswordUiState,
    innerPadding: PaddingValues,
    onAction: (ChangePasswordAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding() + 20.dp,
            end = 16.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFEF6797), Color(0xFFF7A0C1))
                            ),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                        Text("비밀번호 보안을 다시 설정하세요", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "현재 비밀번호를 확인한 뒤 새 비밀번호를 등록합니다.",
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "입력 정보",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "비밀번호는 전용 화면에서만 변경되며, 변경 전 현재 비밀번호 확인이 필요합니다.",
                        color = Color(0xFF7C7480),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ConCafeFormField(
                        label = "현재 비밀번호",
                        value = uiState.currentPassword,
                        onValueChange = { onAction(ChangePasswordAction.ChangeCurrentPassword(it)) },
                        placeholder = "현재 비밀번호를 입력하세요",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFB3ACB7)
                            )
                        }
                    )
                    ConCafeFormField(
                        label = "새 비밀번호",
                        value = uiState.newPassword,
                        onValueChange = { onAction(ChangePasswordAction.ChangeNewPassword(it)) },
                        placeholder = "8자 이상 입력하세요",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFB3ACB7)
                            )
                        }
                    )
                    ConCafeFormField(
                        label = "새 비밀번호 확인",
                        value = uiState.confirmPassword,
                        onValueChange = { onAction(ChangePasswordAction.ChangeConfirmPassword(it)) },
                        placeholder = "새 비밀번호를 다시 입력하세요",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFB3ACB7)
                            )
                        }
                    )
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "안내",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PasswordGuideRow("새 비밀번호는 8자 이상이어야 합니다.")
                    PasswordGuideRow("새 비밀번호 확인 입력값까지 일치해야 합니다.")
                    PasswordGuideRow("변경 즉시 다음 로그인부터 새 비밀번호가 적용됩니다.")
                }
            }
        }
        item {
            Button(
                onClick = { onAction(ChangePasswordAction.ClickSubmit) },
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
                Text(
                    text = if (uiState.isSubmitting) "변경 중..." else "비밀번호 변경",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PasswordGuideRow(
    text: String
) {
    Surface(
        color = Color(0xFFF8F5F6),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFFEF6797),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6F6673)
            )
        }
    }
}
