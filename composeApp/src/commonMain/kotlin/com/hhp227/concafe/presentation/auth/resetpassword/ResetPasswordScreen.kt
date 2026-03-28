package com.hhp227.concafe.presentation.auth.resetpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: ResetPasswordViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<ResetPasswordViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                ResetPasswordEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is ResetPasswordEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("비밀번호 재설정") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(ResetPasswordAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ResetPasswordContentScreen(
            uiState = uiState,
            innerPadding = innerPadding,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun ResetPasswordContentScreen(
    uiState: ResetPasswordUiState,
    innerPadding: PaddingValues,
    onAction: (ResetPasswordAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
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
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                        Text("이메일로 비밀번호를 재설정하세요", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "가입한 이메일 주소로 재설정 링크를 보내드립니다.",
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
                        text = "계정에 등록된 이메일을 입력하면 비밀번호 재설정 메일을 발송합니다.",
                        color = Color(0xFF7C7480),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ConCafeFormField(
                        label = "이메일",
                        value = uiState.email,
                        onValueChange = { onAction(ResetPasswordAction.ChangeEmail(it)) },
                        placeholder = "가입한 이메일을 입력하세요",
                        keyboardType = KeyboardType.Email,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Email,
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
                    ResetPasswordGuideRow("메일 수신까지 1~3분 정도 소요될 수 있습니다.")
                    ResetPasswordGuideRow("메일이 보이지 않으면 스팸함을 확인해 주세요.")
                    ResetPasswordGuideRow("링크를 통해 새 비밀번호를 설정할 수 있습니다.")
                }
            }
        }
        item {
            Button(
                onClick = { onAction(ResetPasswordAction.ClickSubmit) },
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
                    text = if (uiState.isSubmitting) "발송 중..." else "재설정 메일 발송",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ResetPasswordGuideRow(
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
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
