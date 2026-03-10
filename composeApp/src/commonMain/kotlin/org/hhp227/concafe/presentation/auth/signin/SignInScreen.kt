package org.hhp227.concafe.presentation.auth.signin

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.hhp227.concafe.di.resolveSignInUseCase
import org.hhp227.concafe.presentation.component.SignInDivider
import org.hhp227.concafe.presentation.component.SignInLogoSection
import org.hhp227.concafe.presentation.component.SignInSocialButton
import org.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SignInViewModel(resolveSignInUseCase())
            }
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
        onSignUp = { onNavigate(NavigationAction.NavigateToSignUp) },
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignInContentScreen(
    uiState: SignInUiState,
    onBack: () -> Unit,
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
                        listOf(Color(0xFFFFF2F7), Color(0xFFFFFBFD), Color(0xFFFDEDF4))
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
                            label = { Text("이메일") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { onAction(SignInAction.ChangePassword(it)) },
                            label = { Text("비밀번호") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage,
                                color = Color(0xFFD1436F),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { onAction(SignInAction.ClickEmailSignIn) },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6797)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(if (uiState.isLoading) "로그인 중..." else "로그인")
                        }
                    }
                }
                item {
                    SignInDivider()
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SignInSocialButton(
                            label = "카카오로 시작하기",
                            emoji = "💬",
                            containerColor = Color(0xFFFEE500),
                            contentColor = Color.Black,
                            onClick = { onAction(SignInAction.ClickSocialSignIn(SignInProvider.KAKAO)) }
                        )
                        SignInSocialButton(
                            label = "구글로 시작하기",
                            emoji = "🔍",
                            containerColor = Color.White,
                            contentColor = Color(0xFF222222),
                            outlined = true,
                            onClick = { onAction(SignInAction.ClickSocialSignIn(SignInProvider.GOOGLE)) }
                        )
                        SignInSocialButton(
                            label = "애플로 시작하기",
                            emoji = "🍎",
                            containerColor = Color(0xFF111111),
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
                        Text(
                            text = "비밀번호 찾기",
                            color = Color(0xFF8E8794),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = " | ",
                            color = Color(0xFFB5AEB9),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = onSignUp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "회원가입",
                                color = Color(0xFF8E8794),
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
                    .padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기"
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
