package com.hhp227.concafe.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.NotificationListItem
import com.hhp227.concafe.domain.model.NotificationSection
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.common_notification
import concafe.composeapp.generated.resources.notification_login_required_desc
import concafe.composeapp.generated.resources.notification_login_required_title
import concafe.composeapp.generated.resources.notification_summary_desc
import concafe.composeapp.generated.resources.notification_summary_title
import concafe.composeapp.generated.resources.signin_back_content_description
import concafe.composeapp.generated.resources.signin_submit
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<NotificationViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                NotificationEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is NotificationEvent.NavigateToCafe -> onNavigationAction(NavigationAction.NavigateToCafe(event.id))
                is NotificationEvent.NavigateToCast -> onNavigationAction(NavigationAction.NavigateToCast(event.id))
                NotificationEvent.NavigateToSignIn -> onNavigationAction(NavigationAction.NavigateToSignIn)
            }
        }
    }
    NotificationContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationContentScreen(
    uiState: NotificationUiState,
    onAction: (NotificationAction) -> Unit
) {
    Scaffold(
        containerColor = colorFromHex("FFFBFD"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.common_notification)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(NotificationAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.signin_back_content_description)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            !uiState.isLoggedIn -> {
                NotificationSignInRequiredScreen(
                    modifier = Modifier.padding(innerPadding),
                    onAction = onAction
                )
            }
            else -> {
                NotificationSectionsScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun NotificationSignInRequiredScreen(
    modifier: Modifier = Modifier,
    onAction: (NotificationAction) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorFromHex("FFFBFD"))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = colorFromHex("EF6797"),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(Res.string.notification_login_required_title),
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.notification_login_required_desc),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { onAction(NotificationAction.ClickSignIn) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.signin_submit))
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionsScreen(
    modifier: Modifier = Modifier,
    uiState: NotificationUiState,
    onAction: (NotificationAction) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colorFromHex("FFFBFD")),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(colorFromHex("EF6797"), colorFromHex("F7A0C1"))
                            )
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.notification_summary_title, uiState.unreadCount), color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(Res.string.notification_summary_desc), color = Color.White.copy(alpha = 0.92f))
                    }
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        items(uiState.sections, key = { it.id }) { section ->
            NotificationSectionCard(
                section = section,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun NotificationSectionCard(
    section: NotificationSection,
    onAction: (NotificationAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.items.forEach { item ->
                NotificationItemCard(
                    item = item,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: NotificationListItem,
    onAction: (NotificationAction) -> Unit
) {
    val visual = notificationVisual(item.type)
    val containerColor = if (item.isRead) MaterialTheme.colorScheme.surface else colorFromHex("FFF3F8")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onAction(
                    NotificationAction.ClickNotification(
                        id = item.id,
                        type = item.type,
                        targetId = item.targetId
                    )
                )
            },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(visual.backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                            .background(colorFromHex("EF6797"), CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = item.relativeTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private data class NotificationVisual(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconColor: Color
)

private fun notificationVisual(type: String): NotificationVisual {
    return when (type) {
        "CAST_SHIFT" -> NotificationVisual(Icons.Filled.Place, colorFromHex("E4F7EC"), colorFromHex("2E9E5B"))
        "BIRTHDAY" -> NotificationVisual(Icons.Filled.Cake, colorFromHex("FFE6F1"), colorFromHex("EB5F97"))
        "CAFE_NOTICE" -> NotificationVisual(Icons.Filled.Campaign, colorFromHex("E8F0FF"), colorFromHex("4A79E8"))
        "CAFE_EVENT" -> NotificationVisual(Icons.Filled.Celebration, colorFromHex("FFF4E2"), colorFromHex("E29B35"))
        "FOLLOW_UPDATE" -> NotificationVisual(Icons.Filled.PersonAddAlt1, colorFromHex("F1E8FF"), colorFromHex("8A52E2"))
        else -> NotificationVisual(Icons.Filled.Notifications, colorFromHex("F2F2F2"), colorFromHex("666666"))
    }
}
