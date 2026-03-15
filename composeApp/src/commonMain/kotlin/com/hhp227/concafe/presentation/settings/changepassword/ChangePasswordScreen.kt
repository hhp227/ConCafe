package com.hhp227.concafe.presentation.settings.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = viewModel(),
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
                    PasswordGuideRow("실제 서버 변경 연동은 후속 단계에서 연결됩니다.")
                }
            }
        }
        item {
            Button(
                onClick = { onAction(ChangePasswordAction.ClickSubmit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD1DC),
                    contentColor = Color(0xFF2B2330)
                )
            ) {
                Text("비밀번호 변경", fontWeight = FontWeight.Bold)
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
