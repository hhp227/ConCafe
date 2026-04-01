package com.hhp227.concafe.presentation.auth.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.formatKoreanPhoneNumber
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.presentation.component.SignInDivider
import com.hhp227.concafe.presentation.component.SignInLogoSection
import com.hhp227.concafe.presentation.component.SignInSocialButton
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.apple_icon
import concafe.composeapp.generated.resources.google_logo
import concafe.composeapp.generated.resources.kakao_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.GlobalContext

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<SignUpViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                SignUpEvent.NavigateBack -> onNavigate(NavigationAction.NavigateBack)
                SignUpEvent.SignedUp -> onNavigate(NavigationAction.NavigateToMain())
            }
        }
    }
    SignUpContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignUpContentScreen(
    uiState: SignUpUiState,
    onAction: (SignUpAction) -> Unit
) {
    val density = LocalDensity.current
    val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFF2F7), Color(0xFFFFFBFD), Color(0xFFFDEDF4))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 76.dp,
                    end = 20.dp,
                    bottom = 32.dp + imeBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (uiState.step == SignUpUiState.Step.SELECT_TYPE) {
                    item { SignInLogoSection() }
                    item { SignUpIntroSection() }
                    items(SignUpUiState.UserType.entries, key = { it.name }) { type ->
                        UserTypeCard(type = type, onClick = { onAction(SignUpAction.ClickUserType(type)) })
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "이미 계정이 있으신가요?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8794)
                            )
                            TextButton(onClick = { onAction(SignUpAction.ClickSignInInstead) }) {
                                Text("로그인")
                            }
                        }
                    }
                } else {
                    item {
                        SignUpFormHeader(type = requireNotNull(uiState.selectedUserType))
                    }
                    item {
                        SignUpFormSection(
                            uiState = uiState,
                            onAction = onAction
                        )
                    }
                    if (uiState.selectedUserType == SignUpUiState.UserType.VISITOR) {
                        item { SignInDivider() }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SignInSocialButton(
                                    label = "카카오로 시작하기",
                                    icon = painterResource(Res.drawable.kakao_icon),
                                    containerColor = Color(0xFFFEE500),
                                    contentColor = Color.Black,
                                    onClick = { onAction(SignUpAction.ClickSocialSignUp(SignUpProvider.KAKAO)) }
                                )
                                SignInSocialButton(
                                    label = "구글로 시작하기",
                                    icon = painterResource(Res.drawable.google_logo),
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF222222),
                                    outlined = true,
                                    onClick = { onAction(SignUpAction.ClickSocialSignUp(SignUpProvider.GOOGLE)) }
                                )
                                SignInSocialButton(
                                    label = "애플로 시작하기",
                                    icon = painterResource(Res.drawable.apple_icon),
                                    containerColor = Color(0xFF111111),
                                    contentColor = Color.White,
                                    onClick = { onAction(SignUpAction.ClickSocialSignUp(SignUpProvider.APPLE)) }
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "이미 계정이 있으신가요?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8794)
                            )
                            TextButton(onClick = { onAction(SignUpAction.ClickSignInInstead) }) {
                                Text("로그인")
                            }
                        }
                    }
                }
            }
            IconButton(
                onClick = { onAction(SignUpAction.ClickBack) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
        }
    }
}

@Composable
private fun SignUpIntroSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "회원 유형 선택",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "어떤 방법으로 가입하시겠어요?",
            color = Color(0xFF7C7480)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTypeCard(
    type: SignUpUiState.UserType,
    onClick: () -> Unit
) {
    val accentColor = when (type) {
        SignUpUiState.UserType.VISITOR -> Color(0xFF4F8EF7)
        SignUpUiState.UserType.CAST -> Color(0xFFF06292)
        SignUpUiState.UserType.CAFE_OWNER -> Color(0xFF8B5CF6)
    }
    val icon = when (type) {
        SignUpUiState.UserType.VISITOR -> Icons.Default.Person
        SignUpUiState.UserType.CAST -> Icons.Default.AutoAwesome
        SignUpUiState.UserType.CAFE_OWNER -> Icons.Default.Storefront
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE7DFE8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(type.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = type.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6E6671)
                )
                Text(
                    text = type.badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFDA4E84)
                )
            }
        }
    }
}

@Composable
private fun SignUpFormHeader(type: SignUpUiState.UserType) {
    val icon = when (type) {
        SignUpUiState.UserType.VISITOR -> Icons.Default.Person
        SignUpUiState.UserType.CAST -> Icons.Default.AutoAwesome
        SignUpUiState.UserType.CAFE_OWNER -> Icons.Default.Storefront
    }
    val colors = when (type) {
        SignUpUiState.UserType.VISITOR -> listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))
        SignUpUiState.UserType.CAST -> listOf(Color(0xFFF472B6), Color(0xFFEC4899))
        SignUpUiState.UserType.CAFE_OWNER -> listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6))
    }
    val description = when (type) {
        SignUpUiState.UserType.VISITOR -> "간편하게 시작하세요!"
        SignUpUiState.UserType.CAST -> "소속 카페를 등록하세요"
        SignUpUiState.UserType.CAFE_OWNER -> "휴대폰 인증 후 운영 카페를 선택하거나 나중에 연결할 수 있습니다"
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(type.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(description, color = Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun SignUpFormSection(
    uiState: SignUpUiState,
    onAction: (SignUpAction) -> Unit
) {
    val selectedType = requireNotNull(uiState.selectedUserType)
    val filteredCafes = uiState.cafes.filter {
        uiState.cafeSearchQuery.isBlank() ||
            it.name.contains(uiState.cafeSearchQuery, ignoreCase = true) ||
            it.region.city.contains(uiState.cafeSearchQuery, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SignUpTextField(
            value = uiState.email,
            label = "이메일",
            placeholder = "email@example.com",
            keyboardType = KeyboardType.Email,
            onValueChange = { onAction(SignUpAction.ChangeEmail(it)) }
        )
        if (selectedType == SignUpUiState.UserType.CAFE_OWNER) {
            SignUpTextField(
                value = uiState.name,
                label = "이름",
                placeholder = "실명을 입력하세요",
                onValueChange = { onAction(SignUpAction.ChangeName(it)) }
            )
            PhoneVerificationSection(uiState = uiState, onAction = onAction)
            CafeSelectionSection(
                label = "운영 카페 연결 (선택)",
                placeholder = "가입 전에 연결할 카페를 1개 선택할 수 있습니다",
                filteredCafes = filteredCafes,
                uiState = uiState,
                onAction = onAction
            )
            OwnerCafeGuideCard()
        } else {
            SignUpTextField(
                value = uiState.nickname,
                label = if (selectedType == SignUpUiState.UserType.CAST) "활동명 (닉네임)" else "닉네임",
                placeholder = if (selectedType == SignUpUiState.UserType.CAST) "활동할 이름을 입력하세요" else "사용할 닉네임을 입력하세요",
                onValueChange = { onAction(SignUpAction.ChangeNickname(it)) }
            )
            if (selectedType == SignUpUiState.UserType.CAST) {
                CafeSelectionSection(
                    label = "소속 카페",
                    placeholder = "소속 카페를 선택하세요",
                    filteredCafes = filteredCafes,
                    uiState = uiState,
                    onAction = onAction
                )
                Text(
                    text = "* 소속 카페의 승인이 필요합니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8794)
                )
            }
        }
        SignUpTextField(
            value = uiState.password,
            label = "비밀번호",
            placeholder = "8자 이상 입력하세요",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = { onAction(SignUpAction.ChangePassword(it)) }
        )
        SignUpTextField(
            value = uiState.confirmPassword,
            label = "비밀번호 확인",
            placeholder = "비밀번호를 다시 입력하세요",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = { onAction(SignUpAction.ChangeConfirmPassword(it)) }
        )
        uiState.errorMessage?.let {
            Text(text = it, color = Color(0xFFD1436F), style = MaterialTheme.typography.bodySmall)
        }
        uiState.infoMessage?.let {
            Text(text = it, color = Color(0xFF2E8B57), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { onAction(SignUpAction.ClickSubmit) },
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD1DC),
                contentColor = Color(0xFF2B2330)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (uiState.isLoading) "처리 중..." else selectedType.submitLabel)
        }
        if (selectedType == SignUpUiState.UserType.CAST) {
            Text(
                text = "가입 후 소속 카페의 승인이 완료되면 활동을 시작할 수 있습니다",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8794)
            )
        }
    }
}

@Composable
private fun OwnerCafeGuideCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF6F0FF),
        border = BorderStroke(1.dp, Color(0xFFE6D9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "운영 카페 연결 안내",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5F3AA2)
            )
            Text(
                text = "회원가입 단계에서는 카페 1개만 미리 선택할 수 있습니다. 선택하지 않아도 가입 가능하며, 가입 후 카페관리 탭에서 기존 카페 검색이나 새 카페 등록으로 추가 연결할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5A82)
            )
        }
    }
}

@Composable
private fun SignUpTextField(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PhoneVerificationSection(
    uiState: SignUpUiState,
    onAction: (SignUpAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                SignUpTextField(
                    value = uiState.phone,
                    label = "휴대폰 번호",
                    placeholder = "010-1234-5678",
                    keyboardType = KeyboardType.Phone,
                    onValueChange = { onAction(SignUpAction.ChangePhone(formatKoreanPhoneNumber(it))) }
                )
            }
            Button(
                onClick = { onAction(SignUpAction.ClickSendVerification) },
                enabled = !uiState.isPhoneVerified && uiState.phone.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6797)),
                modifier = Modifier.height(56.dp)
            ) {
                Text(if (uiState.isPhoneVerified) "인증완료" else "인증요청")
            }
        }
        if (uiState.hasRequestedVerification && !uiState.isPhoneVerified) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SignUpTextField(
                        value = uiState.verificationCode,
                        label = "인증번호",
                        placeholder = "인증번호 4자리",
                        keyboardType = KeyboardType.Number,
                        onValueChange = { onAction(SignUpAction.ChangeVerificationCode(it)) }
                    )
                }
                Button(
                    onClick = { onAction(SignUpAction.ClickVerifyCode) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7D2E1), contentColor = Color(0xFF6B3050)),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("확인")
                }
            }
        }
        if (uiState.isPhoneVerified) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF8EF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E8B57))
                Text("휴대폰 인증이 완료되었습니다", color = Color(0xFF2E8B57))
            }
        }
    }
}

@Composable
private fun CafeSelectionSection(
    label: String,
    placeholder: String,
    filteredCafes: List<Cafe>,
    uiState: SignUpUiState,
    onAction: (SignUpAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, Color(0xFFE4DDE5))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(SignUpAction.ClickToggleCafeSearch) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.selectedCafe?.name ?: placeholder,
                    color = if (uiState.selectedCafe == null) Color(0xFF8E8794) else Color(0xFF222222)
                )
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8E8794))
            }
        }
        if (uiState.selectedCafe != null) {
            TextButton(
                onClick = { onAction(SignUpAction.ClickClearCafe) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("선택한 카페 지우기")
            }
        }
        if (uiState.isCafeSearchVisible) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    SignUpTextField(
                        value = uiState.cafeSearchQuery,
                        label = "카페 검색",
                        placeholder = "카페 이름 검색...",
                        onValueChange = { onAction(SignUpAction.ChangeCafeSearchQuery(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filteredCafes.isEmpty()) {
                        Text(
                            text = "검색 결과가 없습니다",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            color = Color(0xFF8E8794)
                        )
                    } else {
                        filteredCafes.forEachIndexed { index, cafe ->
                            CafeSearchItem(cafe = cafe, onClick = { onAction(SignUpAction.ClickCafe(cafe)) })
                            if (index < filteredCafes.lastIndex) {
                                Divider(color = Color(0xFFF1EAF1))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CafeSearchItem(
    cafe: Cafe,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(cafe.name, fontWeight = FontWeight.SemiBold)
            Text(
                text = cafe.region.city,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8794)
            )
        }
        if (cafe.approved) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFEF6797)
            ) {
                Text(
                    text = "인증",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
