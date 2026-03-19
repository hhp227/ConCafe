package com.hhp227.concafe.presentation.main.cafemanagement.menugoods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun MenuGoodsScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: MenuGoodsViewModel = viewModel(
        key = "menu-goods-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<MenuGoodsViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                MenuGoodsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is MenuGoodsEvent.NavigateToEdit -> {
                    onNavigationAction(
                        NavigationAction.NavigateToMenuGoodsEdit(
                            cafeId = event.cafeId,
                            itemId = event.itemId
                        )
                    )
                }
            }
        }
    }
    MenuGoodsContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
    uiState.pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MenuGoodsAction.CancelDeleteItem) },
            title = { Text("항목 삭제") },
            text = { Text("항목을 삭제 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(MenuGoodsAction.ConfirmDeleteItem(item.id)) }) {
                    Text("삭제", color = Color(0xFFD96B7A))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(MenuGoodsAction.CancelDeleteItem) }) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuGoodsContentScreen(
    uiState: MenuGoodsUiState,
    onAction: (MenuGoodsAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "메뉴&굿즈 관리",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(MenuGoodsAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(MenuGoodsAction.ClickSearch) }) {
                        Icon(Icons.Default.Search, contentDescription = "검색")
                    }
                }
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(MenuGoodsAction.ClickAddNewItem) },
                    containerColor = Color(0xFFFFD1DC),
                    contentColor = Color(0xFF2B2330),
                    text = {
                        Text(
                            text = "새 항목 추가",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8FB), Color(0xFFFFF2F6), Color(0xFFFFFCFD))
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CafeContextCard(cafeName = uiState.cafeName)
                }
                item {
                    CollectionTabRow(
                        selectedCollection = uiState.selectedCollection,
                        onSelect = { onAction(MenuGoodsAction.SelectCollection(it)) }
                    )
                }
                if (uiState.isSearchVisible) {
                    item {
                        SearchField(
                            value = uiState.searchQuery,
                            onValueChange = { onAction(MenuGoodsAction.ChangeSearchQuery(it)) }
                        )
                    }
                }
                item {
                    CategoryChipRow(
                        chips = uiState.visibleCategories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onSelect = { onAction(MenuGoodsAction.SelectCategory(it)) }
                    )
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = message,
                            onDismiss = { onAction(MenuGoodsAction.DismissInfoMessage) }
                        )
                    }
                }
                if (uiState.isLoading) {
                    item {
                        LoadingCard()
                    }
                } else if (uiState.filteredVisibleItems.isEmpty()) {
                    item {
                        EmptyStateCard(isSearchMode = uiState.searchQuery.isNotBlank())
                    }
                } else {
                    items(uiState.filteredVisibleItems, key = { it.id }) { item ->
                        ManageItemCard(
                            item = item,
                            isMenu = uiState.selectedCollection == MenuGoodsUiState.CollectionTab.MENU,
                            onEdit = { onAction(MenuGoodsAction.ClickEditItem(item.id)) },
                            onDelete = { onAction(MenuGoodsAction.ClickDeleteItem(item.id)) },
                            onToggleAvailability = { onAction(MenuGoodsAction.ToggleItemAvailability(item.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CafeContextCard(cafeName: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF351B42), Color(0xFF7B3F68), Color(0xFFF28EB5))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (cafeName.isBlank()) "카페 판매 항목" else cafeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "메뉴와 굿즈 판매 상태를 한 화면에서 관리합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun CollectionTabRow(
    selectedCollection: MenuGoodsUiState.CollectionTab,
    onSelect: (MenuGoodsUiState.CollectionTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x1AFFD1DC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionTabButton(
                modifier = Modifier.weight(1f),
                label = "메뉴",
                selected = selectedCollection == MenuGoodsUiState.CollectionTab.MENU,
                onClick = { onSelect(MenuGoodsUiState.CollectionTab.MENU) }
            )
            CollectionTabButton(
                modifier = Modifier.weight(1f),
                label = "굿즈",
                selected = selectedCollection == MenuGoodsUiState.CollectionTab.GOODS,
                onClick = { onSelect(MenuGoodsUiState.CollectionTab.GOODS) }
            )
        }
    }
}

@Composable
private fun CollectionTabButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFFFD1DC) else Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color(0xFF2B2330) else Color(0xFF7A6671),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1D9E4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF9A7D8E)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2B2330)),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = "항목명, 카테고리, 키워드 검색",
                            color = Color(0xFFB395A8),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipRow(
    chips: List<MenuGoodsUiState.CategoryChip>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(chips, key = { it.id ?: "all" }) { chip ->
            val selected = chip.id == selectedCategoryId || (chip.id == null && selectedCategoryId == null)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) Color(0xFFFFD1DC) else Color(0x33FFD1DC),
                onClick = { onSelect(chip.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = categoryChipIcon(chip.iconKey),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) Color(0xFF2B2330) else Color(0xFF6F5E68)
                    )
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color(0xFF2B2330) else Color(0xFF6F5E68),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun categoryChipIcon(iconKey: String): ImageVector = when (iconKey) {
    "food" -> Icons.Default.Restaurant
    "drink" -> Icons.Default.LocalCafe
    "dessert" -> Icons.Default.Icecream
    "goods" -> Icons.Default.Inventory2
    else -> Icons.Default.Storefront
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF6D7),
        border = BorderStroke(1.dp, Color(0xFFF1D88D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "안내 닫기",
                    tint = Color(0xFF6B5320)
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF0E2E9)), RoundedCornerShape(24.dp))
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFEF6797))
    }
}

@Composable
private fun EmptyStateCard(
    isSearchMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0E2E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFCE7EF)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFFEF6797),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                text = if (isSearchMode) "검색 결과가 없습니다." else "등록된 항목이 없습니다.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B2330)
            )
            Text(
                text = if (isSearchMode) {
                    "검색어 또는 카테고리를 바꿔 다시 확인해보세요."
                } else {
                    "새 메뉴나 굿즈를 등록하면 이 목록에 표시됩니다."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7B6B75)
            )
        }
    }
}

@Composable
private fun ManageItemCard(
    item: MenuGoodsUiState.ManageItem,
    isMenu: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0E2E9))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ItemThumbnail(
                label = item.name,
                isMenu = isMenu,
                isAvailable = item.isAvailable
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2330),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.priceText,
                            color = Color(0xFFEF6797),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "편집", tint = Color(0xFF7A6671))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFD96B7A))
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFCE7EF)
                ) {
                    Text(
                        text = item.badgeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB64A79),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B6B75),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.inventoryLabel?.let { stockLabel ->
                    Text(
                        text = stockLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B7A84),
                        fontWeight = FontWeight.Medium
                    )
                }
                Divider(color = Color(0xFFF4E7EE))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.availabilityLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (item.isAvailable) Color(0xFF3B7B5A) else Color(0xFF8A7A82),
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = item.isAvailable,
                        onCheckedChange = { onToggleAvailability() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemThumbnail(
    label: String,
    isMenu: Boolean,
    isAvailable: Boolean
) {
    val gradient = if (isMenu) {
        Brush.linearGradient(
            colors = if (isAvailable) {
                listOf(Color(0xFFFFE0EA), Color(0xFFFAB6D0))
            } else {
                listOf(Color(0xFFF1E2EA), Color(0xFFD7C1CE))
            }
        )
    } else {
        Brush.linearGradient(
            colors = if (isAvailable) {
                listOf(Color(0xFFFFEBCB), Color(0xFFFFD7A1))
            } else {
                listOf(Color(0xFFE7E1DA), Color(0xFFCBC0B2))
            }
        )
    }

    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isMenu) Icons.Default.Storefront else Icons.Default.Inventory2,
                contentDescription = null,
                tint = Color(0xFF704A5F)
            )
            Text(
                text = label.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF704A5F),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
