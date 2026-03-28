package com.hhp227.concafe.presentation.settings.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<AccountSettingsViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                AccountSettingsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                AccountSettingsEvent.NavigateToMain -> onNavigationAction(NavigationAction.NavigateToMain())
                is AccountSettingsEvent.NavigateToCastEdit -> {
                    onNavigationAction(
                        NavigationAction.NavigateToCastEdit(
                            cafeId = event.cafeId,
                            castId = event.castId
                        )
                    )
                }
                AccountSettingsEvent.NavigateToChangePassword -> {
                    onNavigationAction(NavigationAction.NavigateToChangePassword)
                }
                is AccountSettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (uiState.isDeleteDialogVisible) {
        DeleteAccountDialog(
            password = uiState.deletePassword,
            errorMessage = uiState.deletePasswordErrorMessage,
            onValueChange = { viewModel.onAction(AccountSettingsAction.ChangeDeletePassword(it)) },
            onDismiss = { viewModel.onAction(AccountSettingsAction.ClickDismissDeleteDialog) },
            onConfirm = { viewModel.onAction(AccountSettingsAction.ClickDeleteAccount) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("계정 관리") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(AccountSettingsAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AccountSettingsContentScreen(
            uiState = uiState,
            innerPadding = innerPadding,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun AccountSettingsContentScreen(
    uiState: AccountSettingsUiState,
    innerPadding: PaddingValues,
    onAction: (AccountSettingsAction) -> Unit
) {
    val myInfoFeed = uiState.myInfoFeed
    val currentUser = myInfoFeed?.user
    val currentCast = myInfoFeed?.castDetail?.cast
    val linkedCafeName = myInfoFeed?.castDetail?.cafe?.name
    val role = currentUser?.role

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding() + 20.dp,
            end = 16.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            AccountHeroCard(uiState = uiState)
        }
        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = Color(0xFFD1436F),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            AccountSectionCard(
                title = "기본 정보",
                icon = Icons.Default.ManageAccounts
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionEyebrow("내 계정에서 바로 수정 가능한 정보")
                    ConCafeFormField(
                        label = "닉네임",
                        value = uiState.nicknameInput,
                        onValueChange = { onAction(AccountSettingsAction.ChangeNickname(it)) },
                        placeholder = "닉네임을 입력하세요"
                    )
                    Surface(
                        color = Color(0xFFF8F5F6),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccountMetaRow("권한", role.toDisplayText())
                            AccountMetaRow("가입일", currentUser?.createdAt.orEmpty().ifBlank { "연동 예정" })
                            if (role == UserRole.CAFE_OWNER) {
                                AccountMetaRow("운영 카페 수", "${myInfoFeed.ownedCafes.size}곳")
                            }
                        }
                    }
                    if (role == UserRole.CAFE_OWNER) {
                        Text(
                            text = "운영 권한 정보는 카페 관리 화면에서 이어서 확인할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7C7480)
                        )
                    }
                }
            }
        }
        item {
            AccountSectionCard(
                title = "저장",
                icon = Icons.Default.ManageAccounts
            ) {
                SectionEyebrow("닉네임 변경 사항을 반영합니다")
                Button(
                    onClick = { onAction(AccountSettingsAction.ClickSaveUserInfo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF2B2330)
                    )
                ) {
                    Text("사용자 정보 저장", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (role == UserRole.CAST) {
            item {
                AccountSectionCard(
                    title = "캐스트 연결 상태",
                    icon = Icons.Default.Storefront
                ) {
                    SectionEyebrow("현재 연결된 프로필 요약")
                    Text(
                        text = currentCast?.desc.orEmpty().ifBlank {
                            "캐스트 설명이 아직 없습니다. 전용 수정 화면에서 프로필과 공개 정보를 편집할 수 있습니다."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6F6673)
                    )
                }
            }
        }
        if (role == UserRole.ADMIN || role == UserRole.CAFE_OWNER) {
            item {
                AccountSectionCard(
                    title = "권한 연결 상태",
                    icon = Icons.Default.Storefront
                ) {
                    SectionEyebrow("현재 계정에 연결된 운영 권한")
                    Text(
                        text = if (role == UserRole.ADMIN) {
                            "관리자 계정은 운영 승인과 검토 작업을 수행합니다."
                        } else {
                            "운영 카페 ${myInfoFeed.ownedCafes.size}곳이 현재 계정과 연결되어 있습니다."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6F6673)
                    )
                }
            }
        }
        item {
            AccountSectionCard(
                title = "보안 및 연결",
                icon = Icons.Default.Lock
            ) {
                SectionEyebrow("전용 화면으로 이동해 안전하게 처리합니다")
                LinkedDestinationRow(
                    title = "비밀번호 변경",
                    description = "현재 비밀번호 확인 후 새 비밀번호를 설정합니다.",
                    icon = Icons.Default.Lock,
                    onClick = { onAction(AccountSettingsAction.ClickOpenChangePassword) }
                )
                if (role == UserRole.CAST) {
                    LinkedDestinationRow(
                        title = "캐스트 정보 수정",
                        description = buildString {
                            append(currentCast?.name?.ifBlank { "연결된 캐스트 프로필" } ?: "연결된 캐스트 프로필")
                            if (!currentCast?.conceptRole.isNullOrBlank()) {
                                append(" · ")
                                append(currentCast?.conceptRole)
                            }
                        },
                        supporting = linkedCafeName ?: "캐스트 프로필 전체 편집 화면으로 이동합니다.",
                        icon = Icons.Default.Badge,
                        onClick = { onAction(AccountSettingsAction.ClickOpenCastEdit) }
                    )
                }
            }
        }
        item {
            TextButton(
                onClick = { onAction(AccountSettingsAction.ClickShowDeleteDialog) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.isDeleteRequested) "회원탈퇴 요청 완료" else "회원탈퇴",
                    color = if (uiState.isDeleteRequested) Color(0xFFB84473) else Color(0xFF8E8794)
                )
            }
        }
    }
}

@Composable
private fun SectionEyebrow(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF8E8794)
    )
}

@Composable
private fun AccountHeroCard(
    uiState: AccountSettingsUiState
) {
    val currentUser = uiState.myInfoFeed?.user
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFEF6797), Color(0xFFF7A0C1))
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(20.dp)
        ) {
            Text(
                text = currentUser?.nickname?.ifBlank { "ConCafe User" } ?: "ConCafe User",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = currentUser?.email?.ifBlank { "로그인 정보 없음" } ?: "로그인 정보 없음",
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = currentUser?.role.toRoleSummary(),
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun AccountSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFFEF6797))
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun LinkedDestinationCard(
    title: String,
    description: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFFFF1F7), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFEF6797))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (description.isNotBlank()) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF302732))
                }
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C7480))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB3ACB7))
        }
    }
}

@Composable
private fun LinkedDestinationRow(
    title: String,
    description: String,
    supporting: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    LinkedDestinationCard(
        title = title,
        description = description,
        supporting = supporting ?: "이 화면에서 직접 편집하지 않고 전용 화면으로 이동합니다.",
        icon = icon,
        onClick = onClick
    )
}

@Composable
private fun AccountMetaRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF7C7480), style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DeleteAccountDialog(
    password: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFD1436F)
            )
        },
        title = { Text("회원탈퇴 확인") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("계정 보안을 위해 현재 비밀번호를 입력해 주세요.")
                ConCafeFormField(
                    label = "현재 비밀번호",
                    value = password,
                    onValueChange = onValueChange,
                    placeholder = "비밀번호 입력",
                    isPassword = true
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1436F)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("회원탈퇴")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun UserRole?.toDisplayText(): String {
    return when (this) {
        UserRole.ADMIN -> "관리자"
        UserRole.CAFE_OWNER -> "카페 운영자"
        UserRole.CAST -> "캐스트"
        UserRole.VISITOR -> "일반 유저"
        null -> "게스트"
    }
}

private fun UserRole?.toRoleSummary(): String {
    return when (this) {
        UserRole.CAST -> "캐스트 계정으로 팬과의 접점을 관리하고 있어요."
        UserRole.CAFE_OWNER -> "운영 카페와 함께 계정 권한을 관리하고 있어요."
        UserRole.ADMIN -> "운영 관리용 관리자 계정입니다."
        UserRole.VISITOR -> "팬 활동과 리뷰 기록을 관리하는 일반 계정입니다."
        null -> "로그인이 필요한 화면입니다."
    }
}
