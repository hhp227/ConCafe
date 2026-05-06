package com.hhp227.concafe.presentation.main.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.RatingUtils
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.presentation.component.CafeSummaryCard
import com.hhp227.concafe.presentation.component.CapsuleDropdown
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ConCafeTabBar
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.auth_login_required_message
import concafe.composeapp.generated.resources.auth_login_required_title
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.explore_empty_cafe_title
import concafe.composeapp.generated.resources.explore_empty_cast_title
import concafe.composeapp.generated.resources.explore_empty_hint
import concafe.composeapp.generated.resources.explore_error_load_failed
import concafe.composeapp.generated.resources.explore_search_placeholder
import concafe.composeapp.generated.resources.explore_cast_followers
import concafe.composeapp.generated.resources.home_nearby_cafe_type_butler
import concafe.composeapp.generated.resources.home_nearby_cafe_type_cosplay
import concafe.composeapp.generated.resources.home_nearby_cafe_type_devil
import concafe.composeapp.generated.resources.home_nearby_cafe_type_doll
import concafe.composeapp.generated.resources.home_nearby_cafe_type_idol
import concafe.composeapp.generated.resources.home_nearby_cafe_type_maid
import concafe.composeapp.generated.resources.home_nearby_cafe_type_other
import concafe.composeapp.generated.resources.signin_submit
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<ExploreViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is ExploreEvent.NavigateToCafe -> onNavigate(NavigationAction.NavigateToCafe(event.id))
                is ExploreEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
                ExploreEvent.NavigateToSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }
    ExploreContentScreen(uiState, listState, viewModel::onAction)
    if (uiState.isLoginPromptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(ExploreAction.DismissLoginPrompt) },
            title = { Text(stringResource(Res.string.auth_login_required_title)) },
            text = { Text(stringResource(Res.string.auth_login_required_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ExploreAction.ClickLoginPromptSignIn) }) {
                    Text(stringResource(Res.string.signin_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ExploreAction.DismissLoginPrompt) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreContentScreen(
    uiState: ExploreUiState,
    listState: LazyListState,
    onAction: (ExploreAction) -> Unit
) {
    var searchFieldValue by remember { mutableStateOf(TextFieldValue(uiState.query)) }
    val cafeNameById = remember(uiState.cafes) { uiState.cafes.associate { it.id to it.name } }

    LaunchedEffect(uiState.query) {
        if (searchFieldValue.text != uiState.query && searchFieldValue.composition == null) {
            searchFieldValue = TextFieldValue(
                text = uiState.query,
                selection = TextRange(uiState.query.length)
            )
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFromHex("FFFBFD"))
    ) {
        val gridColumnCount = exploreGridColumnCount(maxWidth)
        val rows = remember(uiState.selectedTab, uiState.cafes, uiState.maids, gridColumnCount) {
            if (uiState.selectedTab == ExploreUiState.TabType.CAFE) {
                uiState.cafes.chunked(gridColumnCount).map { row ->
                    row.map { ExploreGridItem.CafeItem(it) }
                }
            } else {
                uiState.maids.chunked(gridColumnCount).map { row ->
                    row.map { ExploreGridItem.MaidItem(it, cafeNameById[it.cafeId] ?: it.cafeId) }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchFieldValue,
                        onValueChange = { next ->
                            searchFieldValue = next
                            if (next.text != uiState.query) {
                                onAction(ExploreAction.QueryChanged(next.text))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(stringResource(Res.string.explore_search_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CapsuleDropdown(
                            selected = uiState.selectedRegion.label,
                            options = ExploreUiState.RegionFilter.entries.map { it.label to it },
                            onSelected = { onAction(ExploreAction.RegionChanged(it)) }
                        )
                        CapsuleDropdown(
                            selected = uiState.selectedSort.label,
                            options = ExploreUiState.SortFilter.entries.map { it.label to it },
                            onSelected = { onAction(ExploreAction.SortChanged(it)) }
                        )
                    }
                }
            }
            stickyHeader {
                Surface(
                    color = colorFromHex("FFFBFD"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                ) {
                    val selectedTabIndex = if (uiState.selectedTab == ExploreUiState.TabType.CAFE) 0 else 1
                    val labels = ExploreUiState.TabType.entries.map { it.label }

                    ConCafeTabBar(
                        labels = labels,
                        selectedIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth(),
                        onTabSelected = { index ->
                            onAction(ExploreAction.TabChanged(ExploreUiState.TabType.entries[index]))
                        }
                    )
                }
            }
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.explore_error_load_failed),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                if (rows.isNotEmpty()) {
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    when (item) {
                                        is ExploreGridItem.CafeItem -> CafeCard(
                                            cafe = item.cafe,
                                            onClick = { onAction(ExploreAction.ClickCafe(item.cafe.id)) }
                                        )
                                        is ExploreGridItem.MaidItem -> MaidCard(
                                            maid = item.maid,
                                            cafeName = item.cafeName,
                                            onClick = { onAction(ExploreAction.ClickMaid(item.maid.id)) }
                                        )
                                    }
                                }
                            }
                            repeat(gridColumnCount - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        ExploreEmptyPlaceholder(
                            title = if (uiState.selectedTab == ExploreUiState.TabType.CAFE) stringResource(Res.string.explore_empty_cafe_title) else stringResource(Res.string.explore_empty_cast_title),
                            description = stringResource(Res.string.explore_empty_hint)
                        )
                    }
                }
                item {
                    ExplorePagingTrigger(uiState = uiState, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun ExploreEmptyPlaceholder(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExplorePagingTrigger(
    uiState: ExploreUiState,
    onAction: (ExploreAction) -> Unit
) {
    val canLoadMore = if (uiState.selectedTab == ExploreUiState.TabType.CAFE) {
        uiState.canLoadMoreCafes
    } else {
        uiState.canLoadMoreMaids
    }
    val isLoadingMore = if (uiState.selectedTab == ExploreUiState.TabType.CAFE) {
        uiState.isLoadingMoreCafes
    } else {
        uiState.isLoadingMoreMaids
    }

    if (!canLoadMore && !isLoadingMore) return
    if (canLoadMore && !isLoadingMore) {
        LaunchedEffect(
            uiState.selectedTab,
            uiState.cafes.size,
            uiState.maids.size,
            canLoadMore,
            isLoadingMore
        ) {
            if (uiState.selectedTab == ExploreUiState.TabType.CAFE) {
                onAction(ExploreAction.LoadMoreCafes)
            } else {
                onAction(ExploreAction.LoadMoreMaids)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator()
        } else {
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Composable
private fun CafeCard(cafe: Cafe, onClick: () -> Unit) {
    val ratingText = RatingUtils.formatOneDecimal(cafe.ratingAvg)

    CafeSummaryCard(
        name = cafe.name,
        rating = ratingText,
        conceptType = localizedCafeConceptType(cafe.conceptType),
        location = cafe.region.city,
        thumbnailImage = cafe.thumbnailImage,
        showLocationIcon = false,
        onClick = onClick
    )
}

@Composable
private fun localizedCafeConceptType(rawConceptType: String): String {
    val normalized = rawConceptType.trim()
    if (normalized.isEmpty()) {
        return ""
    }
    return when (normalized.uppercase()) {
        "MAID" -> stringResource(Res.string.home_nearby_cafe_type_maid)
        "BUTLER" -> stringResource(Res.string.home_nearby_cafe_type_butler)
        "IDOL" -> stringResource(Res.string.home_nearby_cafe_type_idol)
        "DEVIL" -> stringResource(Res.string.home_nearby_cafe_type_devil)
        "DOLL" -> stringResource(Res.string.home_nearby_cafe_type_doll)
        "COSPLAY" -> stringResource(Res.string.home_nearby_cafe_type_cosplay)
        "OTHER" -> stringResource(Res.string.home_nearby_cafe_type_other)
        else -> normalized
    }
}

@Composable
private fun MaidCard(maid: Cast, cafeName: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(colorFromHex("FFDFEA"), colorFromHex("FFBED5"))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!maid.profileImage.isNullOrBlank()) {
                    CompatImageDisplay(
                        imageUrl = maid.profileImage,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp)),
                        applyRoundedClip = false
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                maid.name,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                cafeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.explore_cast_followers, maid.followerCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorFromHex("EF6797")
                )
            }
        }
    }
}

private sealed interface ExploreGridItem {
    data class CafeItem(val cafe: Cafe) : ExploreGridItem
    data class MaidItem(val maid: Cast, val cafeName: String) : ExploreGridItem
}

private fun exploreGridColumnCount(contentWidth: Dp): Int {
    val availableWidth = contentWidth.value - EXPLORE_GRID_HORIZONTAL_PADDING_DP
    val minimumGridWidth = (EXPLORE_GRID_MIN_CELL_WIDTH_DP * 2) + EXPLORE_GRID_ITEM_SPACING_DP
    val normalizedWidth = maxOf(availableWidth, minimumGridWidth)
    val rawCount = ((normalizedWidth + EXPLORE_GRID_ITEM_SPACING_DP) /
        (EXPLORE_GRID_MIN_CELL_WIDTH_DP + EXPLORE_GRID_ITEM_SPACING_DP)).toInt()
    return rawCount.coerceIn(EXPLORE_GRID_MIN_COLUMN_COUNT, EXPLORE_GRID_MAX_COLUMN_COUNT)
}

private const val EXPLORE_GRID_MIN_COLUMN_COUNT = 2
private const val EXPLORE_GRID_MAX_COLUMN_COUNT = 6
private const val EXPLORE_GRID_HORIZONTAL_PADDING_DP = 24f
private const val EXPLORE_GRID_ITEM_SPACING_DP = 12f
private const val EXPLORE_GRID_MIN_CELL_WIDTH_DP = 180f
