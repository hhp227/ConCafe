package com.hhp227.concafe.presentation.settings.inquiry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.inquiry_back_content_description
import concafe.composeapp.generated.resources.inquiry_input_section_desc
import concafe.composeapp.generated.resources.inquiry_input_section_title
import concafe.composeapp.generated.resources.inquiry_message_label
import concafe.composeapp.generated.resources.inquiry_message_placeholder
import concafe.composeapp.generated.resources.inquiry_screen_title
import concafe.composeapp.generated.resources.inquiry_submit
import concafe.composeapp.generated.resources.inquiry_submitting
import concafe.composeapp.generated.resources.inquiry_title_label
import concafe.composeapp.generated.resources.inquiry_title_placeholder
import concafe.composeapp.generated.resources.inquiry_type_bug_report
import concafe.composeapp.generated.resources.inquiry_type_section_desc
import concafe.composeapp.generated.resources.inquiry_type_section_title
import concafe.composeapp.generated.resources.inquiry_type_service
import concafe.composeapp.generated.resources.inquiry_type_suggestion
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryLinkScreen(
    viewModel: InquiryLinkViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<InquiryLinkViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                InquiryLinkEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is InquiryLinkEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    InquiryLinkContentScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InquiryLinkContentScreen(
    uiState: InquiryLinkUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (InquiryLinkAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inquiry_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(InquiryLinkAction.ClickBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.inquiry_back_content_description)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0x33FFD1DC)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onAction(InquiryLinkAction.ClickSubmit) },
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Text(
                            text = if (uiState.isSubmitting) {
                                stringResource(Res.string.inquiry_submitting)
                            } else {
                                stringResource(Res.string.inquiry_submit)
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSystemInDarkTheme()) {
                        Modifier.background(colorFromHex("FFFBFD"))
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(colorFromHex("F8F5F6"), colorFromHex("FFFBFD"))
                            )
                        )
                    }
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    InquiryTypeCard(
                        selectedType = uiState.inquiryType,
                        onSelect = { onAction(InquiryLinkAction.ChangeInquiryType(it)) }
                    )
                }
                item {
                    InquirySectionCard(title = stringResource(Res.string.inquiry_input_section_title)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(Res.string.inquiry_input_section_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ConCafeFormField(
                                label = stringResource(Res.string.inquiry_title_label),
                                value = uiState.title,
                                onValueChange = { onAction(InquiryLinkAction.ChangeTitle(it)) },
                                placeholder = stringResource(Res.string.inquiry_title_placeholder)
                            )
                            ConCafeFormField(
                                label = stringResource(Res.string.inquiry_message_label),
                                value = uiState.message,
                                onValueChange = { onAction(InquiryLinkAction.ChangeMessage(it)) },
                                placeholder = stringResource(Res.string.inquiry_message_placeholder),
                                minLines = 7,
                                singleLine = false
                            )
                            if (uiState.errorMessage != null) {
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorFromHex("D1436F")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InquiryTypeCard(
    selectedType: InquiryType,
    onSelect: (InquiryType) -> Unit
) {
    InquirySectionCard(title = stringResource(Res.string.inquiry_type_section_title)) {
        Text(
            text = stringResource(Res.string.inquiry_type_section_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        InquiryTypeDropdown(
            selectedType = selectedType,
            onSelect = onSelect
        )
    }
}

@Composable
private fun InquiryTypeDropdown(
    selectedType: InquiryType,
    onSelect: (InquiryType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else colorFromHex("F8F5F6"),
            border = BorderStroke(1.dp, Color(0x4DFFD1DC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = localizedInquiryTypeTitle(selectedType),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colorFromHex("7C7480"),
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            InquiryType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(localizedInquiryTypeTitle(type)) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun localizedInquiryTypeTitle(
    type: InquiryType
): String {
    return when (type) {
        InquiryType.SERVICE -> stringResource(Res.string.inquiry_type_service)
        InquiryType.BUG_REPORT -> stringResource(Res.string.inquiry_type_bug_report)
        InquiryType.SUGGESTION -> stringResource(Res.string.inquiry_type_suggestion)
    }
}

@Composable
private fun InquirySectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
