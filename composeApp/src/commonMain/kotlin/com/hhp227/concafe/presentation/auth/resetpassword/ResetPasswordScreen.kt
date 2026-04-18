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
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.reset_password_email_label
import concafe.composeapp.generated.resources.reset_password_email_placeholder
import concafe.composeapp.generated.resources.reset_password_guide_1
import concafe.composeapp.generated.resources.reset_password_guide_2
import concafe.composeapp.generated.resources.reset_password_guide_3
import concafe.composeapp.generated.resources.reset_password_guide_title
import concafe.composeapp.generated.resources.reset_password_hero_desc
import concafe.composeapp.generated.resources.reset_password_hero_title
import concafe.composeapp.generated.resources.reset_password_input_desc
import concafe.composeapp.generated.resources.reset_password_input_title
import concafe.composeapp.generated.resources.reset_password_sending
import concafe.composeapp.generated.resources.reset_password_submit
import concafe.composeapp.generated.resources.reset_password_title
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
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
                title = { Text(stringResource(Res.string.reset_password_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(ResetPasswordAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            .background(colorFromHex("FFFBFD"))
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
                                listOf(colorFromHex("EF6797"), colorFromHex("F7A0C1"))
                            ),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                        Text(stringResource(Res.string.reset_password_hero_title), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(Res.string.reset_password_hero_desc),
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
                        text = stringResource(Res.string.reset_password_input_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.reset_password_input_desc),
                        color = colorFromHex("7C7480"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ConCafeFormField(
                        label = stringResource(Res.string.reset_password_email_label),
                        value = uiState.email,
                        onValueChange = { onAction(ResetPasswordAction.ChangeEmail(it)) },
                        placeholder = stringResource(Res.string.reset_password_email_placeholder),
                        keyboardType = KeyboardType.Email,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = colorFromHex("B3ACB7")
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
                        text = stringResource(Res.string.reset_password_guide_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ResetPasswordGuideRow(stringResource(Res.string.reset_password_guide_1))
                    ResetPasswordGuideRow(stringResource(Res.string.reset_password_guide_2))
                    ResetPasswordGuideRow(stringResource(Res.string.reset_password_guide_3))
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
                    containerColor = colorFromHex("FFD1DC"),
                    contentColor = colorFromHex("2B2330")
                )
            ) {
                Text(
                    text = if (uiState.isSubmitting) {
                        stringResource(Res.string.reset_password_sending)
                    } else {
                        stringResource(Res.string.reset_password_submit)
                    },
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
        color = colorFromHex("F8F5F6"),
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
                tint = colorFromHex("EF6797"),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = colorFromHex("6F6673")
            )
        }
    }
}
