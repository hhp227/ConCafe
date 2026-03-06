package org.hhp227.concafe.presentation.main.explore

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<ExploreViewModel>()
            }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val cafeNameById = state.cafes.associate { it.id to it.name }
    val rows = if (state.selectedTab == ExploreUiState.TabType.CAFE) {
        state.cafes.chunked(2).map { pair ->
            pair.map { ExploreGridItem.CafeItem(it) }
        }
    } else {
        state.maids.chunked(2).map { pair ->
            pair.map { ExploreGridItem.MaidItem(it, cafeNameById[it.cafeId] ?: it.cafeId) }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is ExploreEvent.NavigateToCafeDetail -> onNavigate(NavigationAction.NavigateToCafeDetail(event.id))
                is ExploreEvent.NavigateToCastDetail -> onNavigate(NavigationAction.NavigateToCastDetail(event.id))
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
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
                    value = state.query,
                    onValueChange = { viewModel.onAction(ExploreAction.QueryChanged(it)) },
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
                        selected = state.selectedRegion.label,
                        options = ExploreUiState.RegionFilter.entries.map { it.label to it },
                        onSelected = { viewModel.onAction(ExploreAction.RegionChanged(it)) }
                    )
                    CapsuleDropdown(
                        selected = state.selectedSort.label,
                        options = ExploreUiState.SortFilter.entries.map { it.label to it },
                        onSelected = { viewModel.onAction(ExploreAction.SortChanged(it)) }
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
                val selectedTabIndex = if (state.selectedTab == ExploreUiState.TabType.CAFE) 0 else 1
                TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                    ExploreUiState.TabType.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = { viewModel.onAction(ExploreAction.TabChanged(tab)) },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }
        }
        if (state.isLoading) {
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
        } else if (state.errorMessage != null) {
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
                                    onClick = { viewModel.onAction(ExploreAction.ClickCafe(item.cafe.id)) }
                                )
                                is ExploreGridItem.MaidItem -> MaidCard(
                                    maid = item.maid,
                                    cafeName = item.cafeName,
                                    onClick = { viewModel.onAction(ExploreAction.ClickMaid(item.maid.id)) }
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> CapsuleDropdown(
    selected: String,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF3F3F3),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selected,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF555555)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF777777)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CafeCard(cafe: Cafe, onClick: () -> Unit) {
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
                            colors = listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))
                        )
                    )
            )
        }
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(cafe.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${cafe.ratingAvg}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📍", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                Text(cafe.region.city, style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777))
            }
        }
    }
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
