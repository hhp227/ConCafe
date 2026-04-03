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
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.account_settings_admin_status_desc
import concafe.composeapp.generated.resources.account_settings_back_content_description
import concafe.composeapp.generated.resources.account_settings_cast_desc_empty
import concafe.composeapp.generated.resources.account_settings_default_user_name
import concafe.composeapp.generated.resources.account_settings_delete
import concafe.composeapp.generated.resources.account_settings_delete_dialog_desc
import concafe.composeapp.generated.resources.account_settings_delete_dialog_title
import concafe.composeapp.generated.resources.account_settings_delete_password_label
import concafe.composeapp.generated.resources.account_settings_delete_password_placeholder
import concafe.composeapp.generated.resources.account_settings_delete_requested
import concafe.composeapp.generated.resources.account_settings_label_nickname
import concafe.composeapp.generated.resources.account_settings_link_cast_edit_supporting
import concafe.composeapp.generated.resources.account_settings_link_cast_edit_title
import concafe.composeapp.generated.resources.account_settings_link_cast_profile_default
import concafe.composeapp.generated.resources.account_settings_link_change_password_desc
import concafe.composeapp.generated.resources.account_settings_link_change_password_title
import concafe.composeapp.generated.resources.account_settings_link_default_supporting
import concafe.composeapp.generated.resources.account_settings_meta_joined_at
import concafe.composeapp.generated.resources.account_settings_meta_joined_pending
import concafe.composeapp.generated.resources.account_settings_meta_owned_cafe_count_label
import concafe.composeapp.generated.resources.account_settings_meta_owned_cafe_count_value
import concafe.composeapp.generated.resources.account_settings_meta_role
import concafe.composeapp.generated.resources.account_settings_no_login_info
import concafe.composeapp.generated.resources.account_settings_owner_hint
import concafe.composeapp.generated.resources.account_settings_owner_status_desc
import concafe.composeapp.generated.resources.account_settings_placeholder_nickname
import concafe.composeapp.generated.resources.account_settings_role_admin
import concafe.composeapp.generated.resources.account_settings_role_cast
import concafe.composeapp.generated.resources.account_settings_role_guest
import concafe.composeapp.generated.resources.account_settings_role_owner
import concafe.composeapp.generated.resources.account_settings_role_summary_admin
import concafe.composeapp.generated.resources.account_settings_role_summary_cast
import concafe.composeapp.generated.resources.account_settings_role_summary_guest
import concafe.composeapp.generated.resources.account_settings_role_summary_owner
import concafe.composeapp.generated.resources.account_settings_role_summary_visitor
import concafe.composeapp.generated.resources.account_settings_role_visitor
import concafe.composeapp.generated.resources.account_settings_save_user
import concafe.composeapp.generated.resources.account_settings_section_basic
import concafe.composeapp.generated.resources.account_settings_section_basic_eyebrow
import concafe.composeapp.generated.resources.account_settings_section_cast_status
import concafe.composeapp.generated.resources.account_settings_section_cast_status_eyebrow
import concafe.composeapp.generated.resources.account_settings_section_owner_status
import concafe.composeapp.generated.resources.account_settings_section_owner_status_eyebrow
import concafe.composeapp.generated.resources.account_settings_section_save
import concafe.composeapp.generated.resources.account_settings_section_save_eyebrow
import concafe.composeapp.generated.resources.account_settings_section_security
import concafe.composeapp.generated.resources.account_settings_section_security_eyebrow
import concafe.composeapp.generated.resources.account_settings_title
import concafe.composeapp.generated.resources.common_cancel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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
                title = { Text(stringResource(Res.string.account_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(AccountSettingsAction.ClickBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.account_settings_back_content_description)
                        )
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
                title = stringResource(Res.string.account_settings_section_basic),
                icon = Icons.Default.ManageAccounts
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionEyebrow(stringResource(Res.string.account_settings_section_basic_eyebrow))
                    ConCafeFormField(
                        label = stringResource(Res.string.account_settings_label_nickname),
                        value = uiState.nicknameInput,
                        onValueChange = { onAction(AccountSettingsAction.ChangeNickname(it)) },
                        placeholder = stringResource(Res.string.account_settings_placeholder_nickname)
                    )
                    Surface(
                        color = Color(0xFFF8F5F6),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccountMetaRow(
                                stringResource(Res.string.account_settings_meta_role),
                                stringResource(role.toDisplayText())
                            )
                            AccountMetaRow(
                                stringResource(Res.string.account_settings_meta_joined_at),
                                currentUser?.createdAt.orEmpty().ifBlank {
                                    stringResource(Res.string.account_settings_meta_joined_pending)
                                }
                            )
                            if (role == UserRole.CAFE_OWNER) {
                                AccountMetaRow(
                                    stringResource(Res.string.account_settings_meta_owned_cafe_count_label),
                                    stringResource(
                                        Res.string.account_settings_meta_owned_cafe_count_value,
                                        myInfoFeed.ownedCafes.size
                                    )
                                )
                            }
                        }
                    }
                    if (role == UserRole.CAFE_OWNER) {
                        Text(
                            text = stringResource(Res.string.account_settings_owner_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7C7480)
                        )
                    }
                }
            }
        }
        item {
            AccountSectionCard(
                title = stringResource(Res.string.account_settings_section_save),
                icon = Icons.Default.ManageAccounts
            ) {
                SectionEyebrow(stringResource(Res.string.account_settings_section_save_eyebrow))
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
                    Text(
                        stringResource(Res.string.account_settings_save_user),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (role == UserRole.CAST) {
            item {
                AccountSectionCard(
                    title = stringResource(Res.string.account_settings_section_cast_status),
                    icon = Icons.Default.Storefront
                ) {
                    SectionEyebrow(stringResource(Res.string.account_settings_section_cast_status_eyebrow))
                    Text(
                        text = currentCast?.desc.orEmpty().ifBlank {
                            stringResource(Res.string.account_settings_cast_desc_empty)
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
                    title = stringResource(Res.string.account_settings_section_owner_status),
                    icon = Icons.Default.Storefront
                ) {
                    SectionEyebrow(stringResource(Res.string.account_settings_section_owner_status_eyebrow))
                    Text(
                        text = if (role == UserRole.ADMIN) {
                            stringResource(Res.string.account_settings_admin_status_desc)
                        } else {
                            stringResource(
                                Res.string.account_settings_owner_status_desc,
                                myInfoFeed.ownedCafes.size
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6F6673)
                    )
                }
            }
        }
        item {
            AccountSectionCard(
                title = stringResource(Res.string.account_settings_section_security),
                icon = Icons.Default.Lock
            ) {
                SectionEyebrow(stringResource(Res.string.account_settings_section_security_eyebrow))
                LinkedDestinationRow(
                    title = stringResource(Res.string.account_settings_link_change_password_title),
                    description = stringResource(Res.string.account_settings_link_change_password_desc),
                    icon = Icons.Default.Lock,
                    onClick = { onAction(AccountSettingsAction.ClickOpenChangePassword) }
                )
                if (role == UserRole.CAST) {
                    LinkedDestinationRow(
                        title = stringResource(Res.string.account_settings_link_cast_edit_title),
                        description = buildString {
                            append(
                                currentCast?.name?.ifBlank {
                                    stringResource(Res.string.account_settings_link_cast_profile_default)
                                } ?: stringResource(Res.string.account_settings_link_cast_profile_default)
                            )
                            if (!currentCast?.conceptRole.isNullOrBlank()) {
                                append(" · ")
                                append(currentCast?.conceptRole)
                            }
                        },
                        supporting = linkedCafeName
                            ?: stringResource(Res.string.account_settings_link_cast_edit_supporting),
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
                    text = if (uiState.isDeleteRequested) {
                        stringResource(Res.string.account_settings_delete_requested)
                    } else {
                        stringResource(Res.string.account_settings_delete)
                    },
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
                text = currentUser?.nickname?.ifBlank {
                    stringResource(Res.string.account_settings_default_user_name)
                } ?: stringResource(Res.string.account_settings_default_user_name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = currentUser?.email?.ifBlank {
                    stringResource(Res.string.account_settings_no_login_info)
                } ?: stringResource(Res.string.account_settings_no_login_info),
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(currentUser?.role.toRoleSummary()),
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
        supporting = supporting ?: stringResource(Res.string.account_settings_link_default_supporting),
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
        title = { Text(stringResource(Res.string.account_settings_delete_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.account_settings_delete_dialog_desc))
                ConCafeFormField(
                    label = stringResource(Res.string.account_settings_delete_password_label),
                    value = password,
                    onValueChange = onValueChange,
                    placeholder = stringResource(Res.string.account_settings_delete_password_placeholder),
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
                Text(stringResource(Res.string.account_settings_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

private fun UserRole?.toDisplayText(): StringResource {
    return when (this) {
        UserRole.ADMIN -> Res.string.account_settings_role_admin
        UserRole.CAFE_OWNER -> Res.string.account_settings_role_owner
        UserRole.CAST -> Res.string.account_settings_role_cast
        UserRole.VISITOR -> Res.string.account_settings_role_visitor
        null -> Res.string.account_settings_role_guest
    }
}

private fun UserRole?.toRoleSummary(): StringResource {
    return when (this) {
        UserRole.CAST -> Res.string.account_settings_role_summary_cast
        UserRole.CAFE_OWNER -> Res.string.account_settings_role_summary_owner
        UserRole.ADMIN -> Res.string.account_settings_role_summary_admin
        UserRole.VISITOR -> Res.string.account_settings_role_summary_visitor
        null -> Res.string.account_settings_role_summary_guest
    }
}