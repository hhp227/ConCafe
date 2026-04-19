package com.hhp227.concafe.presentation.auth.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.collectLatest
import com.hhp227.concafe.presentation.component.SignInDivider
import com.hhp227.concafe.presentation.component.SignInLogoSection
import com.hhp227.concafe.presentation.component.SignInSocialButton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.apple_icon
import concafe.composeapp.generated.resources.google_logo
import concafe.composeapp.generated.resources.kakao_icon
import concafe.composeapp.generated.resources.signin_back_content_description
import concafe.composeapp.generated.resources.signin_email_label
import concafe.composeapp.generated.resources.signin_forgot_password
import concafe.composeapp.generated.resources.signin_loading
import concafe.composeapp.generated.resources.signin_password_label
import concafe.composeapp.generated.resources.signin_sign_up
import concafe.composeapp.generated.resources.signin_submit
import concafe.composeapp.generated.resources.signup_social_apple
import concafe.composeapp.generated.resources.signup_social_google
import concafe.composeapp.generated.resources.signup_social_kakao
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<SignInViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collectLatest { event ->
            when (event) {
                SignInEvent.SignedIn -> onNavigate(NavigationAction.NavigateBack)
            }
        }
    }
    SignInContentScreen(
        uiState = uiState,
        onBack = { onNavigate(NavigationAction.NavigateBack) },
        onResetPassword = { onNavigate(NavigationAction.NavigateToResetPassword) },
        onSignUp = { onNavigate(NavigationAction.NavigateToSignUp) },
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignInContentScreen(
    uiState: SignInUiState,
    onBack: () -> Unit,
    onResetPassword: () -> Unit,
    onSignUp: () -> Unit,
    onAction: (SignInAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        if (isSystemInDarkTheme()) {
                            listOf(
                                colorFromHex("FFFBFD"),
                                colorFromHex("FFFBFD"),
                                colorFromHex("FFFBFD")
                            )
                        } else {
                            listOf(colorFromHex("FFF2F7"), colorFromHex("FFFBFD"), colorFromHex("FDEDF4"))
                        }
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 88.dp,
                    end = 20.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    SignInLogoSection()
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = { onAction(SignInAction.ChangeEmail(it)) },
                            label = { Text(stringResource(Res.string.signin_email_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                unfocusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                focusedBorderColor = colorFromHex("EF6797"),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isSystemInDarkTheme()) 0.65f else 0.35f),
                                focusedLabelColor = colorFromHex("EF6797"),
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { onAction(SignInAction.ChangePassword(it)) },
                            label = { Text(stringResource(Res.string.signin_password_label)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                unfocusedContainerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                focusedBorderColor = colorFromHex("EF6797"),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isSystemInDarkTheme()) 0.65f else 0.35f),
                                focusedLabelColor = colorFromHex("EF6797"),
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage,
                                color = colorFromHex("D1436F"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { onAction(SignInAction.ClickEmailSignIn) },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorFromHex("FFD1DC"),
                                contentColor = colorFromHex("2B2330")
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(if (uiState.isLoading) stringResource(Res.string.signin_loading) else stringResource(Res.string.signin_submit))
                        }
                    }
                }
                item {
                    SignInDivider()
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SignInSocialButton(
                            label = stringResource(Res.string.signup_social_kakao),
                            icon = painterResource(Res.drawable.kakao_icon),
                            containerColor = colorFromHex("FEE500"),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { onAction(SignInAction.ClickSocialSignIn(SignInProvider.KAKAO)) }
                        )
                        SignInSocialButton(
                            label = stringResource(Res.string.signup_social_google),
                            icon = painterResource(Res.drawable.google_logo),
                            containerColor = Color.White,
                            contentColor = colorFromHex("222222"),
                            outlined = true,
                            onClick = { onAction(SignInAction.ClickSocialSignIn(SignInProvider.GOOGLE)) }
                        )
                        SignInSocialButton(
                            label = stringResource(Res.string.signup_social_apple),
                            icon = painterResource(Res.drawable.apple_icon),
                            containerColor = colorFromHex("111111"),
                            contentColor = Color.White,
                            onClick = { onAction(SignInAction.ClickSocialSignIn(SignInProvider.APPLE)) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onResetPassword,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.signin_forgot_password),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = " | ",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = onSignUp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.signin_sign_up),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 5.dp, top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.signin_back_content_description)
                )
            }
        }
    }
}

enum class SignInProvider {
    KAKAO,
    GOOGLE,
    APPLE
}
