package com.hhp227.concafe.presentation.main.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.presentation.component.CafeSummaryCard
import com.hhp227.concafe.presentation.component.CapsuleDropdown
import com.hhp227.concafe.presentation.component.ConCafeTabBar
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import kotlin.collections.chunked
import kotlin.collections.map

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
            title = { Text("로그인이 필요합니다") },
            text = { Text("카페/캐스트 상세는 로그인 후 이용할 수 있습니다.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ExploreAction.ClickLoginPromptSignIn) }) {
                    Text("로그인")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ExploreAction.DismissLoginPrompt) }) {
                    Text("취소")
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
    val cafeNameById = uiState.cafes.associate { it.id to it.name }
    val rows = if (uiState.selectedTab == ExploreUiState.TabType.CAFE) {
        uiState.cafes.chunked(2).map { pair ->
            pair.map { ExploreGridItem.CafeItem(it) }
        }
    } else {
        uiState.maids.chunked(2).map { pair ->
            pair.map { ExploreGridItem.MaidItem(it, cafeNameById[it.cafeId] ?: it.cafeId) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
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
                    value = uiState.query,
                    onValueChange = { onAction(ExploreAction.QueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("카페나 메이드를 검색하세요...") },
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
                color = Color(0xFFFFFBFD),
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
                        text = "탐색 데이터를 불러오지 못했습니다.",
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
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    ExploreEmptyPlaceholder(
                        title = if (uiState.selectedTab == ExploreUiState.TabType.CAFE) "카페 검색 결과가 없어요" else "캐스트 검색 결과가 없어요",
                        description = "검색어 또는 필터를 바꿔서 다시 찾아보세요."
                    )
                }
            }
            item {
                ExplorePagingTrigger(uiState = uiState, onAction = onAction)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                color = Color(0xFF5C525D)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A7F8B)
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
    CafeSummaryCard(
        name = cafe.name,
        rating = "${cafe.ratingAvg}",
        location = cafe.region.city,
        thumbnailImage = cafe.thumbnailImage,
        onClick = onClick
    )
}

@Composable
private fun MaidCard(maid: Cast, cafeName: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFDFEA), Color(0xFFFFBED5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }
        }
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(maid.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                cafeName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF777777),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    maid.followerCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF6797)
                )
            }
        }
    }
}

private sealed interface ExploreGridItem {
    data class CafeItem(val cafe: Cafe) : ExploreGridItem
    data class MaidItem(val maid: Cast, val cafeName: String) : ExploreGridItem
}
