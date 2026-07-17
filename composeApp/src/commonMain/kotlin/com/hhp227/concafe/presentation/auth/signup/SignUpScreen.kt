package com.hhp227.concafe.presentation.auth.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.formatKoreanPhoneNumber
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.presentation.component.SignInDivider
import com.hhp227.concafe.presentation.component.SignInLogoSection
import com.hhp227.concafe.presentation.component.SignInSocialButton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.apple_icon
import concafe.composeapp.generated.resources.signup_cast_after_signup_notice
import concafe.composeapp.generated.resources.signup_cast_cafe_approval_required
import concafe.composeapp.generated.resources.signup_cast_cafe_placeholder
import concafe.composeapp.generated.resources.signup_cast_cafe_title
import concafe.composeapp.generated.resources.signup_cast_nickname_label
import concafe.composeapp.generated.resources.signup_cast_nickname_placeholder
import concafe.composeapp.generated.resources.signup_cafe_verified_badge
import concafe.composeapp.generated.resources.signup_clear_selected_cafe
import concafe.composeapp.generated.resources.signup_confirm_password_label
import concafe.composeapp.generated.resources.signup_confirm_password_placeholder
import concafe.composeapp.generated.resources.signup_desc_cast
import concafe.composeapp.generated.resources.signup_desc_owner
import concafe.composeapp.generated.resources.signup_desc_visitor
import concafe.composeapp.generated.resources.signup_email_label
import concafe.composeapp.generated.resources.signup_email_placeholder
import concafe.composeapp.generated.resources.signup_footer_has_account
import concafe.composeapp.generated.resources.signup_footer_sign_in
import concafe.composeapp.generated.resources.google_logo
import concafe.composeapp.generated.resources.kakao_icon
import concafe.composeapp.generated.resources.signup_name_label
import concafe.composeapp.generated.resources.signup_name_placeholder
import concafe.composeapp.generated.resources.signup_nickname_label
import concafe.composeapp.generated.resources.signup_nickname_placeholder
import concafe.composeapp.generated.resources.signup_owner_cafe_guide_message
import concafe.composeapp.generated.resources.signup_owner_cafe_guide_title
import concafe.composeapp.generated.resources.signup_owner_cafe_link_placeholder
import concafe.composeapp.generated.resources.signup_owner_cafe_link_title
import concafe.composeapp.generated.resources.signup_password_label
import concafe.composeapp.generated.resources.signup_password_placeholder
import concafe.composeapp.generated.resources.signup_phone_label
import concafe.composeapp.generated.resources.signup_phone_placeholder
import concafe.composeapp.generated.resources.signup_phone_request
import concafe.composeapp.generated.resources.signup_phone_verified
import concafe.composeapp.generated.resources.signup_phone_verified_message
import concafe.composeapp.generated.resources.signup_processing
import concafe.composeapp.generated.resources.signup_search_cafe_label
import concafe.composeapp.generated.resources.signup_search_cafe_placeholder
import concafe.composeapp.generated.resources.signup_search_no_results
import concafe.composeapp.generated.resources.signup_select_type_subtitle
import concafe.composeapp.generated.resources.signup_select_type_title
import concafe.composeapp.generated.resources.signup_social_google
import concafe.composeapp.generated.resources.signup_social_kakao
import concafe.composeapp.generated.resources.signup_social_apple
import concafe.composeapp.generated.resources.signup_submit
import concafe.composeapp.generated.resources.signup_submit_cast
import concafe.composeapp.generated.resources.signup_user_type_cast_badge
import concafe.composeapp.generated.resources.signup_user_type_cast_subtitle
import concafe.composeapp.generated.resources.signup_user_type_cast_title
import concafe.composeapp.generated.resources.signup_user_type_owner_badge
import concafe.composeapp.generated.resources.signup_user_type_owner_subtitle
import concafe.composeapp.generated.resources.signup_user_type_owner_title
import concafe.composeapp.generated.resources.signup_user_type_visitor_badge
import concafe.composeapp.generated.resources.signup_user_type_visitor_subtitle
import concafe.composeapp.generated.resources.signup_user_type_visitor_title
import concafe.composeapp.generated.resources.signup_verification_code_label
import concafe.composeapp.generated.resources.signup_verification_code_placeholder
import concafe.composeapp.generated.resources.signup_verification_confirm
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

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
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onAction(SignUpAction.CleanupIncompleteAccount)
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
                        if (isSystemInDarkTheme()) {
                            listOf(
                                ConCafeColors.background,
                                ConCafeColors.background,
                                ConCafeColors.background
                            )
                        } else {
                            listOf(ConCafeColors.background, ConCafeColors.background, ConCafeColors.surfaceTint)
                        }
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
                                text = stringResource(Res.string.signup_footer_has_account),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { onAction(SignUpAction.ClickSignInInstead) }) {
                                Text(stringResource(Res.string.signup_footer_sign_in))
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
                    if (!uiState.hasAuthenticatedSocialAccount) {
                        item { SignInDivider() }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SignInSocialButton(
                                    label = stringResource(Res.string.signup_social_kakao),
                                    icon = painterResource(Res.drawable.kakao_icon),
                                    containerColor = colorFromHex("FEE500"),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    onClick = { onAction(SignUpAction.ClickSocialSignUp(SignUpProvider.KAKAO)) }
                                )
                                SignInSocialButton(
                                    label = stringResource(Res.string.signup_social_google),
                                    icon = painterResource(Res.drawable.google_logo),
                                    containerColor = Color.White,
                                    contentColor = colorFromHex("222222"),
                                    outlined = true,
                                    onClick = { onAction(SignUpAction.ClickSocialSignUp(SignUpProvider.GOOGLE)) }
                                )
                                SignInSocialButton(
                                    label = stringResource(Res.string.signup_social_apple),
                                    icon = painterResource(Res.drawable.apple_icon),
                                    containerColor = colorFromHex("111111"),
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
                                text = stringResource(Res.string.signup_footer_has_account),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { onAction(SignUpAction.ClickSignInInstead) }) {
                                Text(stringResource(Res.string.signup_footer_sign_in))
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
                    .padding(start = 5.dp, top = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SignUpIntroSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.signup_select_type_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.signup_select_type_subtitle),
            color = ConCafeColors.textSecondary
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
        SignUpUiState.UserType.VISITOR -> ConCafeColors.info
        SignUpUiState.UserType.CAST -> ConCafeColors.primary
        SignUpUiState.UserType.CAFE_OWNER -> ConCafeColors.primary
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
        border = BorderStroke(1.dp, ConCafeColors.outline)
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
                Text(userTypeTitle(type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = userTypeSubtitle(type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ConCafeColors.textSecondary
                )
                Text(
                    text = userTypeBadge(type),
                    style = MaterialTheme.typography.labelMedium,
                    color = ConCafeColors.primary
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
        SignUpUiState.UserType.VISITOR -> listOf(ConCafeColors.info, ConCafeColors.info)
        SignUpUiState.UserType.CAST -> listOf(ConCafeColors.primary, ConCafeColors.primary)
        SignUpUiState.UserType.CAFE_OWNER -> listOf(ConCafeColors.secondary, ConCafeColors.primary)
    }
    val description = when (type) {
        SignUpUiState.UserType.VISITOR -> stringResource(Res.string.signup_desc_visitor)
        SignUpUiState.UserType.CAST -> stringResource(Res.string.signup_desc_cast)
        SignUpUiState.UserType.CAFE_OWNER -> stringResource(Res.string.signup_desc_owner)
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
                Text(userTypeTitle(type), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            label = stringResource(Res.string.signup_email_label),
            placeholder = stringResource(Res.string.signup_email_placeholder),
            keyboardType = KeyboardType.Email,
            onValueChange = { onAction(SignUpAction.ChangeEmail(it)) }
        )
        if (selectedType == SignUpUiState.UserType.CAFE_OWNER) {
            SignUpTextField(
                value = uiState.name,
                label = stringResource(Res.string.signup_name_label),
                placeholder = stringResource(Res.string.signup_name_placeholder),
                onValueChange = { onAction(SignUpAction.ChangeName(it)) }
            )
            PhoneVerificationSection(uiState = uiState, onAction = onAction)
            CafeSelectionSection(
                label = stringResource(Res.string.signup_owner_cafe_link_title),
                placeholder = stringResource(Res.string.signup_owner_cafe_link_placeholder),
                filteredCafes = filteredCafes,
                uiState = uiState,
                onAction = onAction
            )
            OwnerCafeGuideCard()
        } else {
            SignUpTextField(
                value = uiState.nickname,
                label = if (selectedType == SignUpUiState.UserType.CAST) {
                    stringResource(Res.string.signup_cast_nickname_label)
                } else {
                    stringResource(Res.string.signup_nickname_label)
                },
                placeholder = if (selectedType == SignUpUiState.UserType.CAST) {
                    stringResource(Res.string.signup_cast_nickname_placeholder)
                } else {
                    stringResource(Res.string.signup_nickname_placeholder)
                },
                onValueChange = { onAction(SignUpAction.ChangeNickname(it)) }
            )
            if (selectedType == SignUpUiState.UserType.CAST) {
                CafeSelectionSection(
                    label = stringResource(Res.string.signup_cast_cafe_title),
                    placeholder = stringResource(Res.string.signup_cast_cafe_placeholder),
                    filteredCafes = filteredCafes,
                    uiState = uiState,
                    onAction = onAction
                )
                Text(
                    text = stringResource(Res.string.signup_cast_cafe_approval_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = ConCafeColors.textMuted
                )
            }
        }
        if (!uiState.isSocialFlow) {
            SignUpTextField(
                value = uiState.password,
                label = stringResource(Res.string.signup_password_label),
                placeholder = stringResource(Res.string.signup_password_placeholder),
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { onAction(SignUpAction.ChangePassword(it)) }
            )
            SignUpTextField(
                value = uiState.confirmPassword,
                label = stringResource(Res.string.signup_confirm_password_label),
                placeholder = stringResource(Res.string.signup_confirm_password_placeholder),
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { onAction(SignUpAction.ChangeConfirmPassword(it)) }
            )
        }
        uiState.errorMessage?.let {
            Text(text = it, color = ConCafeColors.primary, style = MaterialTheme.typography.bodySmall)
        }
        uiState.infoMessage?.let {
            Text(text = it, color = ConCafeColors.success, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { onAction(SignUpAction.ClickSubmit) },
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConCafeColors.primaryContainer,
                contentColor = ConCafeColors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (uiState.isLoading) stringResource(Res.string.signup_processing) else submitLabel(selectedType))
        }
        if (selectedType == SignUpUiState.UserType.CAST) {
            Text(
                text = stringResource(Res.string.signup_cast_after_signup_notice),
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.textMuted
            )
        }
    }
}

@Composable
private fun OwnerCafeGuideCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ConCafeColors.primaryContainer,
        border = BorderStroke(1.dp, ConCafeColors.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(Res.string.signup_owner_cafe_guide_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ConCafeColors.primary
            )
            Text(
                text = stringResource(Res.string.signup_owner_cafe_guide_message),
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.primary
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
    visualTransformation: VisualTransformation = VisualTransformation.None
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
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            unfocusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            focusedBorderColor = ConCafeColors.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isSystemInDarkTheme()) 0.65f else 0.35f),
            focusedLabelColor = ConCafeColors.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun PhoneVerificationSection(
    uiState: SignUpUiState,
    onAction: (SignUpAction) -> Unit
) {
    var phoneFieldValue by remember(uiState.phone) {
        mutableStateOf(TextFieldValue(uiState.phone, TextRange(uiState.phone.length)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = phoneFieldValue,
                    onValueChange = { new ->
                        val formatted = formatKoreanPhoneNumber(new.text)
                        phoneFieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                        onAction(SignUpAction.ChangePhone(formatted))
                    },
                    label = { Text(stringResource(Res.string.signup_phone_label)) },
                    placeholder = { Text(stringResource(Res.string.signup_phone_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        unfocusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        focusedBorderColor = ConCafeColors.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isSystemInDarkTheme()) 0.65f else 0.35f),
                        focusedLabelColor = ConCafeColors.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Button(
                onClick = { onAction(SignUpAction.ClickSendVerification) },
                enabled = !uiState.isPhoneVerified && uiState.phone.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConCafeColors.primary),
                modifier = Modifier.height(56.dp)
            ) {
                Text(if (uiState.isPhoneVerified) stringResource(Res.string.signup_phone_verified) else stringResource(Res.string.signup_phone_request))
            }
        }
        if (uiState.hasRequestedVerification && !uiState.isPhoneVerified) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SignUpTextField(
                        value = uiState.verificationCode,
                        label = stringResource(Res.string.signup_verification_code_label),
                        placeholder = stringResource(Res.string.signup_verification_code_placeholder),
                        keyboardType = KeyboardType.Number,
                        onValueChange = { onAction(SignUpAction.ChangeVerificationCode(it)) }
                    )
                }
                Button(
                    onClick = { onAction(SignUpAction.ClickVerifyCode) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConCafeColors.primaryContainer, contentColor = ConCafeColors.onPrimaryContainer),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(stringResource(Res.string.signup_verification_confirm))
                }
            }
        }
        if (uiState.isPhoneVerified) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConCafeColors.successContainer, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ConCafeColors.success)
                Text(stringResource(Res.string.signup_phone_verified_message), color = ConCafeColors.success)
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
            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isSystemInDarkTheme()) 0.65f else 0.35f))
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
                    color = if (uiState.selectedCafe == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (uiState.selectedCafe != null) {
            TextButton(
                onClick = { onAction(SignUpAction.ClickClearCafe) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(stringResource(Res.string.signup_clear_selected_cafe))
            }
        }
        if (uiState.isCafeSearchVisible) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White
                )
            ) {
                Column {
                    SignUpTextField(
                        value = uiState.cafeSearchQuery,
                        label = stringResource(Res.string.signup_search_cafe_label),
                        placeholder = stringResource(Res.string.signup_search_cafe_placeholder),
                        onValueChange = { onAction(SignUpAction.ChangeCafeSearchQuery(it)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filteredCafes.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.signup_search_no_results),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredCafes.forEachIndexed { index, cafe ->
                            CafeSearchItem(cafe = cafe, onClick = { onAction(SignUpAction.ClickCafe(cafe)) })
                            if (index < filteredCafes.lastIndex) {
                                Divider(color = ConCafeColors.surfaceTint)
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
                color = ConCafeColors.textMuted
            )
        }
        if (cafe.approved) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = ConCafeColors.primary
            ) {
                Text(
                    text = stringResource(Res.string.signup_cafe_verified_badge),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun userTypeTitle(type: SignUpUiState.UserType): String {
    return when (type) {
        SignUpUiState.UserType.VISITOR -> stringResource(Res.string.signup_user_type_visitor_title)
        SignUpUiState.UserType.CAST -> stringResource(Res.string.signup_user_type_cast_title)
        SignUpUiState.UserType.CAFE_OWNER -> stringResource(Res.string.signup_user_type_owner_title)
    }
}

@Composable
private fun userTypeSubtitle(type: SignUpUiState.UserType): String {
    return when (type) {
        SignUpUiState.UserType.VISITOR -> stringResource(Res.string.signup_user_type_visitor_subtitle)
        SignUpUiState.UserType.CAST -> stringResource(Res.string.signup_user_type_cast_subtitle)
        SignUpUiState.UserType.CAFE_OWNER -> stringResource(Res.string.signup_user_type_owner_subtitle)
    }
}

@Composable
private fun userTypeBadge(type: SignUpUiState.UserType): String {
    return when (type) {
        SignUpUiState.UserType.VISITOR -> stringResource(Res.string.signup_user_type_visitor_badge)
        SignUpUiState.UserType.CAST -> stringResource(Res.string.signup_user_type_cast_badge)
        SignUpUiState.UserType.CAFE_OWNER -> stringResource(Res.string.signup_user_type_owner_badge)
    }
}

@Composable
private fun submitLabel(type: SignUpUiState.UserType): String {
    return if (type == SignUpUiState.UserType.CAST) {
        stringResource(Res.string.signup_submit_cast)
    } else {
        stringResource(Res.string.signup_submit)
    }
}
