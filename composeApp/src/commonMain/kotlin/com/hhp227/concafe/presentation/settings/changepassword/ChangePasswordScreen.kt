package com.hhp227.concafe.presentation.settings.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.changepw_back_content_description
import concafe.composeapp.generated.resources.changepw_current_password_label
import concafe.composeapp.generated.resources.changepw_current_password_placeholder
import concafe.composeapp.generated.resources.changepw_guide_1
import concafe.composeapp.generated.resources.changepw_guide_2
import concafe.composeapp.generated.resources.changepw_guide_3
import concafe.composeapp.generated.resources.changepw_guide_title
import concafe.composeapp.generated.resources.changepw_hero_desc
import concafe.composeapp.generated.resources.changepw_hero_title
import concafe.composeapp.generated.resources.changepw_info_desc
import concafe.composeapp.generated.resources.changepw_info_title
import concafe.composeapp.generated.resources.changepw_new_password_confirm_label
import concafe.composeapp.generated.resources.changepw_new_password_confirm_placeholder
import concafe.composeapp.generated.resources.changepw_new_password_label
import concafe.composeapp.generated.resources.changepw_new_password_placeholder
import concafe.composeapp.generated.resources.changepw_submit
import concafe.composeapp.generated.resources.changepw_submitting
import concafe.composeapp.generated.resources.changepw_title
import org.jetbrains.compose.resources.stringResource
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
                title = { Text(stringResource(Res.string.changepw_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(ChangePasswordAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.changepw_back_content_description))
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
                        Text(stringResource(Res.string.changepw_hero_title), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(Res.string.changepw_hero_desc),
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
                        text = stringResource(Res.string.changepw_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.changepw_info_desc),
                        color = Color(0xFF7C7480),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ConCafeFormField(
                        label = stringResource(Res.string.changepw_current_password_label),
                        value = uiState.currentPassword,
                        onValueChange = { onAction(ChangePasswordAction.ChangeCurrentPassword(it)) },
                        placeholder = stringResource(Res.string.changepw_current_password_placeholder),
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFB3ACB7)
                            )
                        }
                    )
                    ConCafeFormField(
                        label = stringResource(Res.string.changepw_new_password_label),
                        value = uiState.newPassword,
                        onValueChange = { onAction(ChangePasswordAction.ChangeNewPassword(it)) },
                        placeholder = stringResource(Res.string.changepw_new_password_placeholder),
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFB3ACB7)
                            )
                        }
                    )
                    ConCafeFormField(
                        label = stringResource(Res.string.changepw_new_password_confirm_label),
                        value = uiState.confirmPassword,
                        onValueChange = { onAction(ChangePasswordAction.ChangeConfirmPassword(it)) },
                        placeholder = stringResource(Res.string.changepw_new_password_confirm_placeholder),
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
                        text = stringResource(Res.string.changepw_guide_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PasswordGuideRow(stringResource(Res.string.changepw_guide_1))
                    PasswordGuideRow(stringResource(Res.string.changepw_guide_2))
                    PasswordGuideRow(stringResource(Res.string.changepw_guide_3))
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
                    text = if (uiState.isSubmitting) stringResource(Res.string.changepw_submitting) else stringResource(Res.string.changepw_submit),
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
