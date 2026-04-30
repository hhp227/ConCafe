package com.hhp227.concafe.presentation.cafe.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ImageDisplaySize
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_error_retry_prompt
import concafe.composeapp.generated.resources.cafeevent_go_to_cafe
import concafe.composeapp.generated.resources.cafeevent_like_count
import concafe.composeapp.generated.resources.cafeevent_live_performance_badge
import concafe.composeapp.generated.resources.cafeevent_section_participating_cast
import concafe.composeapp.generated.resources.noticeevent_info_event_load_failed
import concafe.composeapp.generated.resources.noticeevent_validation_event_required
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

private val HeroTitleTriggerOffset = 20.dp
private val HeroHeight = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeEventScreen(
    cafeId: String,
    eventId: String,
    showCafeButton: Boolean = false,
    viewModel: CafeEventViewModel = viewModel(
        key = "$cafeId:$eventId",
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<CafeEventViewModel> {
                    parametersOf(cafeId, eventId)
                }
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeEventEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is CafeEventEvent.NavigateToCast -> onNavigationAction(NavigationAction.NavigateToCast(event.castId))
                CafeEventEvent.NavigateToSignIn -> onNavigationAction(NavigationAction.NavigateToSignIn)
                CafeEventEvent.NavigateToCafe -> onNavigationAction(NavigationAction.ReplaceWithCafe(cafeId))
            }
        }
    }
    CafeEventContentScreen(
        uiState = uiState,
        cafeId = cafeId,
        showCafeButton = showCafeButton,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CafeEventContentScreen(
    uiState: CafeEventUiState,
    cafeId: String,
    showCafeButton: Boolean,
    onAction: (CafeEventAction) -> Unit
) {
    val listState = rememberLazyListState()
    val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset
    } else {
        Int.MAX_VALUE
    }
    val topBarVisible = uiState.event != null && (
        listState.firstVisibleItemIndex > 0 ||
            (listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }?.let { heroItem ->
                val heroBottom = heroItem.offset + heroItem.size
                heroBottom <= with(LocalDensity.current) { HeroTitleTriggerOffset.roundToPx() }
            } == true)
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (topBarVisible) uiState.event.title else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CafeEventAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = if (topBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (topBarVisible) MaterialTheme.colorScheme.surface else Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = if (topBarVisible) MaterialTheme.colorScheme.onSurface else Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorFromHex("EF6797")
                )
                uiState.event != null -> CafeEventDetailContent(
                    uiState = uiState,
                    listState = listState,
                    scrollOffset = scrollOffset,
                    showCafeButton = showCafeButton,
                    onAction = onAction,
                    innerPadding = innerPadding
                )
                else -> CafeEventErrorContent(
                    message = when (uiState.errorMessage) {
                        "Event not found." -> stringResource(Res.string.noticeevent_validation_event_required)
                        else -> stringResource(Res.string.noticeevent_info_event_load_failed)
                    },
                    onRetry = { onAction(CafeEventAction.Retry) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun CafeEventDetailContent(
    uiState: CafeEventUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollOffset: Int,
    showCafeButton: Boolean,
    onAction: (CafeEventAction) -> Unit,
    innerPadding: PaddingValues
) {
    val event = requireNotNull(uiState.event)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 32.dp)
    ) {
        item {
            CafeEventHeroImage(
                event = event,
                scrollOffset = scrollOffset
            )
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = colorFromHex("FDE7EF")
                    ) {
                        Text(
                            text = event.statusLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = colorFromHex("9E2E5C"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (event.hasLivePerformance) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = colorFromHex("FFF0E0")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = colorFromHex("C25800")
                                )
                                Text(
                                    text = stringResource(Res.string.cafeevent_live_performance_badge),
                                    color = colorFromHex("C25800"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.periodText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onAction(CafeEventAction.ToggleLike) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (uiState.isLikedByMe) colorFromHex("EF6797") else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(Res.string.cafeevent_like_count, uiState.likeCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isLikedByMe) colorFromHex("EF6797") else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showCafeButton) {
                        Button(
                            onClick = { onAction(CafeEventAction.GoToCafe) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorFromHex("EF6797"),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(Res.string.cafeevent_go_to_cafe),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        if (uiState.participantCasts.isNotEmpty()) {
            item {
                CafeEventCastSection(
                    casts = uiState.participantCasts,
                    onCastClick = { castId -> onAction(CafeEventAction.ClickCast(castId)) }
                )
            }
        }
        item {
            Text(
                text = event.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = if (uiState.participantCasts.isNotEmpty()) 4.dp else 18.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun CafeEventHeroImage(
    event: CafeEventManagementItem,
    scrollOffset: Int
) {
    val parallaxOffset = if (scrollOffset == Int.MAX_VALUE) {
        120f
    } else {
        scrollOffset * 0.35f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (event.imageUrl.isNotBlank()) {
            BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                CompatImageDisplay(
                    imageUrl = event.imageUrl,
                    modifier = Modifier
                        .size(maxWidth, maxHeight)
                        .graphicsLayer { translationY = parallaxOffset },
                    displaySize = ImageDisplaySize.FULL,
                    applyRoundedClip = false
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(colorFromHex("FDE7EF"), colorFromHex("FCCFDF"))))
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.4f to Color.Transparent,
                            1.0f to Color(0x55000000)
                        )
                    )
                )
        )
    }
}

@Composable
private fun CafeEventCastSection(
    casts: List<Cast>,
    onCastClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(Res.string.cafeevent_section_participating_cast),
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(casts, key = { it.id }) { cast ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onCastClick(cast.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(colorFromHex("FDE7EF"), colorFromHex("FCCFDF"))
                                )
                            )
                    ) {
                        if (!cast.profileImage.isNullOrBlank()) {
                            CompatImageDisplay(
                                imageUrl = cast.profileImage,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape),
                                applyRoundedClip = false
                            )
                        }
                    }
                    Text(
                        text = cast.name,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CafeEventErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.cafe_error_retry_prompt))
        }
    }
}
