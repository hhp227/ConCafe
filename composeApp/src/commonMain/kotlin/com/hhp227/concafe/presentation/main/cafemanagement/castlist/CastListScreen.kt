package com.hhp227.concafe.presentation.main.cafemanagement.castlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.ShimmerListSkeleton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.castlist_accessibility_delete
import concafe.composeapp.generated.resources.castlist_accessibility_schedule
import concafe.composeapp.generated.resources.castlist_action_add
import concafe.composeapp.generated.resources.castlist_action_load_more
import concafe.composeapp.generated.resources.castlist_content_back
import concafe.composeapp.generated.resources.castlist_empty
import concafe.composeapp.generated.resources.castlist_empty_search
import concafe.composeapp.generated.resources.castlist_info_cast_deleted
import concafe.composeapp.generated.resources.castlist_info_load_failed
import concafe.composeapp.generated.resources.castlist_off_shift
import concafe.composeapp.generated.resources.castlist_on_shift
import concafe.composeapp.generated.resources.castlist_screen_title
import concafe.composeapp.generated.resources.castlist_search_label
import concafe.composeapp.generated.resources.castlist_search_placeholder
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.common_confirm
import concafe.composeapp.generated.resources.dashboard_delete_cast_message
import concafe.composeapp.generated.resources.dashboard_delete_cast_title
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import com.hhp227.concafe.presentation.component.ConCafeColors

@Composable
fun CastListScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CastListViewModel = viewModel(
        key = "cast-list-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CastListViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CastListEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is CastListEvent.NavigateToCastEdit -> {
                    onNavigationAction(NavigationAction.NavigateToCastEdit(event.cafeId, event.castId))
                }
                is CastListEvent.NavigateToSchedule -> {
                    onNavigationAction(NavigationAction.NavigateToSchedule(event.castId))
                }
            }
        }
    }
    if (uiState.isDeleteCastDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(CastListAction.DismissDeleteCastDialog) },
            title = { Text(stringResource(Res.string.dashboard_delete_cast_title)) },
            text = { Text(stringResource(Res.string.dashboard_delete_cast_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(CastListAction.ConfirmDeleteCast) }) {
                    Text(stringResource(Res.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(CastListAction.DismissDeleteCastDialog) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    CastListContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastListContentScreen(
    uiState: CastListUiState,
    onAction: (CastListAction) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.castlist_screen_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CastListAction.ClickBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.castlist_content_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onAction(CastListAction.ClickAddCast) }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = ConCafeColors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(Res.string.castlist_action_add),
                            modifier = Modifier.padding(start = 4.dp),
                            color = ConCafeColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ConCafeColors.background)
                .padding(innerPadding)
        ) {
            ConCafeFormField(
                label = stringResource(Res.string.castlist_search_label),
                value = uiState.searchQuery,
                onValueChange = { onAction(CastListAction.ChangeSearchQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = stringResource(Res.string.castlist_search_placeholder),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ConCafeColors.textMuted
                    )
                }
            )
            uiState.infoMessage?.let { message ->
                CastListInfoBanner(
                    message = when (message) {
                        "castlist_info_load_failed" -> stringResource(Res.string.castlist_info_load_failed)
                        "castlist_info_cast_deleted" -> stringResource(Res.string.castlist_info_cast_deleted)
                        else -> message
                    },
                    onDismiss = { onAction(CastListAction.DismissInfoMessage) }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    ShimmerListSkeleton(itemCount = 8, avatarSize = 56.dp)
                } else if (uiState.filteredCasts.isEmpty()) {
                    Text(
                        text = if (uiState.isSearching) {
                            stringResource(Res.string.castlist_empty_search)
                        } else {
                            stringResource(Res.string.castlist_empty)
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        color = ConCafeColors.textMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredCasts, key = { it.id }) { cast ->
                            CastListItemCard(
                                cast = cast,
                                onClick = { onAction(CastListAction.ClickCast(cast.id)) },
                                onScheduleClick = { onAction(CastListAction.ClickCastSchedule(cast.id)) },
                                onDeleteClick = { onAction(CastListAction.ClickDeleteCast(cast.id)) }
                            )
                        }
                        if (uiState.hasMoreCasts && !uiState.isSearching) {
                            item {
                                LoadMoreCastRow(
                                    isLoading = uiState.isLoadingMore,
                                    onClick = { onAction(CastListAction.ClickLoadMoreCasts) }
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
private fun CastListItemCard(
    cast: CafeCastPreview,
    onClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ConCafeColors.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(ConCafeColors.primaryContainer, ConCafeColors.background)
                            ),
                            shape = CircleShape
                        )
                )
                if (!cast.profileImage.isNullOrBlank()) {
                    CompatImageDisplay(
                        imageUrl = cast.profileImage,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(
                            if (cast.isOnShift) ConCafeColors.success else ConCafeColors.outlineStrong,
                            CircleShape
                        )
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = cast.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CastShiftBadge(isOnShift = cast.isOnShift)
            }
            IconButton(
                onClick = onScheduleClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = ConCafeColors.surfaceTint,
                    contentColor = ConCafeColors.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(Res.string.castlist_accessibility_schedule),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = ConCafeColors.surfaceTint,
                    contentColor = ConCafeColors.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.castlist_accessibility_delete),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CastShiftBadge(isOnShift: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isOnShift) ConCafeColors.successContainer else ConCafeColors.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isOnShift) ConCafeColors.successContainer else ConCafeColors.outline
        )
    ) {
        Text(
            text = if (isOnShift) {
                stringResource(Res.string.castlist_on_shift)
            } else {
                stringResource(Res.string.castlist_off_shift)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isOnShift) ConCafeColors.success else ConCafeColors.textMuted
        )
    }
}

@Composable
private fun LoadMoreCastRow(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ConCafeColors.surfaceTint,
        border = BorderStroke(1.dp, ConCafeColors.outline),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = ConCafeColors.primary
                )
            } else {
                Text(
                    text = stringResource(Res.string.castlist_action_load_more),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ConCafeColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun CastListInfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .background(ConCafeColors.primaryContainer.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = ConCafeColors.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.common_confirm), fontWeight = FontWeight.Bold)
        }
    }
}
